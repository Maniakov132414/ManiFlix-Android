package bond.maniakov.maniflix.player

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.session.MediaSession
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.IntroSkipBehavior
import bond.maniakov.maniflix.data.ManiFlixApi
import bond.maniakov.maniflix.model.AdMarker
import bond.maniakov.maniflix.model.EpisodeItem
import bond.maniakov.maniflix.model.EpisodeServer
import bond.maniakov.maniflix.model.MovieDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class PlayerAspectMode(val title: String) {
    FIT_SAFE("Gốc"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    RATIO_21_9("21:9"),
    EDGE_FILL("Tràn viền");

    val explicitRatio: Float?
        get() = when (this) {
            RATIO_16_9 -> 16f / 9f
            RATIO_4_3 -> 4f / 3f
            RATIO_21_9 -> 21f / 9f
            FIT_SAFE, EDGE_FILL -> null
        }
}

object PlayerSessionHolder {
    var activeEngine by mutableStateOf<PlayerEngine?>(null)
    var isInPiPMode by mutableStateOf(false)

    fun open(
        context: Context,
        movie: MovieDetail,
        servers: List<EpisodeServer>,
        serverIndex: Int,
        episodeIndex: Int,
        resumeSec: Double
    ) {
        val existing = activeEngine
        if (existing != null &&
            existing.movie.slug == movie.slug &&
            existing.serverIndex == serverIndex &&
            existing.episodeIndex == episodeIndex
        ) {
            if (resumeSec > 3.0 && existing.currentTime < 2.0) {
                existing.seek(resumeSec)
            }
            AppState.isPlayerPresented = true
            return
        }

        existing?.shutdown()
        val engine = PlayerEngine(
            context = context.applicationContext,
            initialMovie = movie,
            initialServers = servers,
            initialServerIndex = serverIndex,
            initialEpisodeIndex = episodeIndex,
            initialResumeSeconds = resumeSec
        )
        activeEngine = engine
        isInPiPMode = false
        AppState.isPlayerPresented = true
    }

    fun stopCompletely() {
        activeEngine?.shutdown()
        activeEngine = null
        isInPiPMode = false
        AppState.isPlayerPresented = false
    }
}

@OptIn(UnstableApi::class)
class PlayerEngine(
    context: Context,
    initialMovie: MovieDetail,
    initialServers: List<EpisodeServer>,
    initialServerIndex: Int,
    initialEpisodeIndex: Int,
    initialResumeSeconds: Double
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var movie by mutableStateOf(initialMovie)
    var servers by mutableStateOf(initialServers)
    var serverIndex by mutableIntStateOf(initialServerIndex)
    var episodeIndex by mutableIntStateOf(initialEpisodeIndex)

    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(true)
    var isFailed by mutableStateOf(false)
    var errorMessage by mutableStateOf("")
    var currentTime by mutableDoubleStateOf(0.0)
    var duration by mutableDoubleStateOf(0.0)
    var playbackRate by mutableFloatStateOf(1.0f)
    var activeStreamUrl by mutableStateOf("")
    var currentSourceLabel by mutableStateOf("HLS")

    var adMarkers by mutableStateOf<List<AdMarker>>(emptyList())
    var activeSkipPrompt by mutableStateOf<AdMarker?>(null)
    var activeOutroPrompt by mutableStateOf<AdMarker?>(null)
    var undoSkipTime by mutableStateOf<Double?>(null)
    var statusToast by mutableStateOf<String?>(null)
    var aspectMode by mutableStateOf(PlayerAspectMode.FIT_SAFE)
    var flipHorizontal by mutableStateOf(false)
    var flipVertical by mutableStateOf(false)

    val currentEpisode: EpisodeItem?
        get() = servers.getOrNull(serverIndex)?.serverData?.getOrNull(episodeIndex)

    val hasPreviousEpisode: Boolean
        get() = episodeIndex > 0

    val hasNextEpisode: Boolean
        get() {
            val count = servers.getOrNull(serverIndex)?.serverData?.size ?: 0
            return episodeIndex + 1 < count
        }

    val filteredTimelineMarkers: List<AdMarker>
        get() = adMarkers.filter { marker ->
            val endSec = effectiveEnd(marker)
            if (endSec <= marker.start) return@filter false
            if (marker.type == "outro" && marker.start < 30.0) return@filter false
            val cap = if (duration > 0.0) min(420.0, duration * 0.45) else 420.0
            (endSec - marker.start) <= cap
        }

    private var pendingResumeSeconds: Double = initialResumeSeconds
    private var usingCleanSource = true
    private var didFailoverThisEpisode = false
    private var lastSavedProgressTimestamp = -999.0
    private var lastPlayPosSec = 0.0
    private var isTempBoostActive = false

    private val skippedCutIds = mutableSetOf<String>()
    private val dismissedSkipIds = mutableSetOf<String>()
    private val shownPromptIds = mutableSetOf<String>()
    private val userSeekedInsideAdIds = mutableSetOf<String>()

    private var watchdogJob: Job? = null
    private var promptAutoHideJob: Job? = null
    private var undoClearJob: Job? = null
    private var toastJob: Job? = null
    private var adsFetchJob: Job? = null
    private var tickerJob: Job? = null
    private var isShutDown = false

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; ManiFlix) AppleWebKit/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)

    private val hlsMediaSourceFactory = HlsMediaSource.Factory(httpDataSourceFactory)
        .setAllowChunklessPreparation(true)

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setLooper(Looper.getMainLooper())
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var mediaSession: MediaSession? = runCatching {
        MediaSession.Builder(appContext, exoPlayer)
            .setId("maniflix-${System.nanoTime()}")
            .build()
    }.getOrNull()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    isBuffering = true
                }
                Player.STATE_READY -> {
                    isBuffering = false
                    watchdogJob?.cancel()
                    val durMs = exoPlayer.duration
                    if (durMs != C.TIME_UNSET && durMs > 0L) {
                        duration = durMs / 1000.0
                    }
                    if (pendingResumeSeconds > 3.0) {
                        val limit = if (duration > 10.0) max(0.0, duration - 10.0) else pendingResumeSeconds
                        val target = min(pendingResumeSeconds, limit)
                        pendingResumeSeconds = 0.0
                        seek(target)
                        showToast("Phát tiếp từ ${formatTime(target)}")
                    }
                    refreshAudioEffectsInternal(showBanner = false)
                }
                Player.STATE_ENDED -> {
                    isPlaying = false
                    isBuffering = false
                    if (AppPreferences.autoNextEpisode && hasNextEpisode) {
                        nextEpisode()
                    }
                }
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            SpatialAudioController.attachToAudioSession(appContext, audioSessionId)
        }

        override fun onPlayerError(error: PlaybackException) {
            val ep = currentEpisode
            val desc = error.localizedMessage ?: error.message ?: "Không tải được luồng phát"
            if (ep != null) {
                tryFailoverStream(ep, desc)
            } else {
                isFailed = true
                isBuffering = false
                errorMessage = desc
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        applyComfortVolume()
        startTicker()
        loadCurrentEpisode()
    }

    fun formatTime(seconds: Double): String = Companion.formatTime(seconds)

    fun effectiveEnd(marker: AdMarker): Double {
        return if (marker.openEnd && duration > marker.start) {
            duration
        } else {
            marker.end
        }
    }

    private fun resolveMarkerMode(marker: AdMarker): String =
        marker.mode.ifBlank {
            when (marker.type) {
                "ad" -> "cut"
                "credit" -> "mark"
                else -> "skip"
            }
        }

    fun loadCurrentEpisode() {
        if (isShutDown) return
        watchdogJob?.cancel()
        adsFetchJob?.cancel()
        promptAutoHideJob?.cancel()
        undoClearJob?.cancel()

        isFailed = false
        errorMessage = ""
        isBuffering = true
        currentTime = 0.0
        duration = 0.0
        activeSkipPrompt = null
        activeOutroPrompt = null
        undoSkipTime = null
        adMarkers = emptyList()
        skippedCutIds.clear()
        dismissedSkipIds.clear()
        shownPromptIds.clear()
        userSeekedInsideAdIds.clear()
        lastPlayPosSec = 0.0
        didFailoverThisEpisode = false

        val ep = currentEpisode
        if (ep == null) {
            isFailed = true
            isBuffering = false
            errorMessage = "Không tìm thấy tập phim."
            return
        }

        usingCleanSource = true
        startPlaybackStream(ep, useClean = true)

        val epKey = ep.slug.ifBlank { "$episodeIndex" }
        val titleHint = movie.originName.ifBlank { movie.name }
        adsFetchJob = scope.launch {
            val fetched = runCatching {
                ManiFlixApi.fetchAdsMarkers(
                    slug = movie.slug,
                    ep = epKey,
                    titleHint = titleHint
                )
            }.getOrNull()
            adMarkers = fetched ?: emptyList()
        }
    }

    private fun startPlaybackStream(ep: EpisodeItem, useClean: Boolean) {
        val rawM3u8 = ep.linkM3u8.trim()
        if (rawM3u8.isEmpty()) {
            isFailed = true
            isBuffering = false
            errorMessage = "Tập này chưa có luồng phát HLS."
            return
        }

        val resolvedUrl = if (useClean) {
            currentSourceLabel = "Chuẩn"
            ManiFlixApi.cleanM3u8Url(rawM3u8)
        } else {
            currentSourceLabel = "Dự phòng"
            rawM3u8
        }

        val uri = runCatching { Uri.parse(resolvedUrl) }.getOrNull()
        if (uri == null || resolvedUrl.isBlank()) {
            isFailed = true
            isBuffering = false
            errorMessage = "Địa chỉ luồng phát không hợp lệ."
            return
        }

        activeStreamUrl = resolvedUrl
        isFailed = false
        isBuffering = true

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(movie.name)
            .setSubtitle(ep.name)
            .setAlbumTitle(ep.name)
        val posterUrl = movie.displayPosterUrl
        if (posterUrl.isNotBlank()) {
            runCatching { Uri.parse(posterUrl) }.getOrNull()?.let {
                metadataBuilder.setArtworkUri(it)
            }
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        val source = hlsMediaSourceFactory.createMediaSource(mediaItem)
        exoPlayer.setMediaSource(source)
        exoPlayer.playbackParameters = PlaybackParameters(playbackRate)
        applyComfortVolume()
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        scheduleFailoverWatchdog(ep)
    }

    private fun scheduleFailoverWatchdog(ep: EpisodeItem) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            delay(8_500L)
            if (!isShutDown && isBuffering && currentTime < 0.5 && !didFailoverThisEpisode) {
                tryFailoverStream(ep, "Đang đổi sang nguồn dự phòng...")
            }
        }
    }

    private fun tryFailoverStream(ep: EpisodeItem, reason: String) {
        if (didFailoverThisEpisode) {
            isFailed = true
            isBuffering = false
            errorMessage = reason
            return
        }
        didFailoverThisEpisode = true
        usingCleanSource = !usingCleanSource
        showToast(if (usingCleanSource) "Đổi sang nguồn lọc quảng cáo" else "Đổi sang nguồn gốc")
        startPlaybackStream(ep, useClean = usingCleanSource)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && !isShutDown) {
                val posMs = exoPlayer.currentPosition
                val durMs = exoPlayer.duration
                if (durMs != C.TIME_UNSET && durMs > 0L) {
                    duration = durMs / 1000.0
                }
                val sec = max(0.0, posMs / 1000.0)
                currentTime = sec
                if (isBuffering && sec > 0.2 && exoPlayer.playbackState == Player.STATE_READY) {
                    isBuffering = false
                }
                evaluateAdAndIntroMarkers(sec)
                maybeSyncHistory(sec)
                delay(400L)
            }
        }
    }

    private fun evaluateAdAndIntroMarkers(sec: Double) {
        val validMarkers = filteredTimelineMarkers
        if (validMarkers.isEmpty()) {
            lastPlayPosSec = sec
            if (activeSkipPrompt != null) activeSkipPrompt = null
            if (activeOutroPrompt != null) activeOutroPrompt = null
            return
        }

        for (marker in validMarkers) {
            val resolvedMode = resolveMarkerMode(marker)
            if (marker.type == "ad" || resolvedMode == "cut") {
                val endSec = effectiveEnd(marker)
                if (sec < marker.start - 1.0 || sec >= endSec) {
                    userSeekedInsideAdIds.remove(marker.id)
                } else if (abs(sec - lastPlayPosSec) > 3.5 && sec > marker.start + 1.5 && sec < endSec) {
                    userSeekedInsideAdIds.add(marker.id)
                }
            }
        }

        if (AppPreferences.autoCutAds) {
            for (marker in validMarkers) {
                val endSec = effectiveEnd(marker)
                val mode = resolveMarkerMode(marker)
                if (mode == "cut" &&
                    !userSeekedInsideAdIds.contains(marker.id) &&
                    !skippedCutIds.contains(marker.id)
                ) {
                    val atEntry = sec >= marker.start && sec < min(endSec, marker.start + 1.2)
                    val crossedStart = lastPlayPosSec < marker.start && sec >= marker.start && sec < endSec
                    if (atEntry || crossedStart) {
                        skippedCutIds.add(marker.id)
                        lastPlayPosSec = endSec + 0.1
                        seek(endSec + 0.1)
                        showToast("Đã bỏ qua quảng cáo")
                        return
                    }
                }
            }
        }
        lastPlayPosSec = sec

        var foundIntro: AdMarker? = null
        val skipBehavior = AppPreferences.introSkipBehavior
        if (skipBehavior != IntroSkipBehavior.OFF) {
            for (marker in validMarkers) {
                val mode = resolveMarkerMode(marker)
                if (mode != "skip" || marker.type == "outro") continue
                val endSec = effectiveEnd(marker)
                if (sec >= marker.start && sec < endSec - 0.5) {
                    if (skipBehavior == IntroSkipBehavior.AUTO && !skippedCutIds.contains(marker.id)) {
                        skippedCutIds.add(marker.id)
                        recordUndoAnchor(sec)
                        seek(endSec + 0.1)
                        return
                    } else if (!dismissedSkipIds.contains(marker.id) && !skippedCutIds.contains(marker.id)) {
                        foundIntro = marker
                        break
                    }
                } else if (sec < marker.start) {
                    shownPromptIds.remove(marker.id)
                }
            }
        }

        if (foundIntro != null) {
            if (!shownPromptIds.contains(foundIntro.id)) {
                shownPromptIds.add(foundIntro.id)
                activeSkipPrompt = foundIntro
                scheduleIntroPromptAutoHide(foundIntro.id)
            }
        } else if (activeSkipPrompt != null) {
            activeSkipPrompt = null
        }

        var foundOutro: AdMarker? = null
        if (skipBehavior != IntroSkipBehavior.OFF) {
            for (marker in validMarkers) {
                if (marker.type != "outro") continue
                val mode = resolveMarkerMode(marker)
                if (mode == "mark") continue
                val endSec = effectiveEnd(marker)
                if (sec >= marker.start && sec < endSec - 0.5 && !dismissedSkipIds.contains(marker.id)) {
                    foundOutro = marker
                    break
                }
            }
        }
        activeOutroPrompt = foundOutro
    }

    private fun scheduleIntroPromptAutoHide(markerId: String) {
        promptAutoHideJob?.cancel()
        promptAutoHideJob = scope.launch {
            delay(5_000L)
            if (activeSkipPrompt?.id == markerId) {
                activeSkipPrompt = null
            }
        }
    }

    private fun recordUndoAnchor(previousTime: Double) {
        undoSkipTime = previousTime
        undoClearJob?.cancel()
        undoClearJob = scope.launch {
            delay(5_500L)
            undoSkipTime = null
        }
    }

    fun undoLastSkip() {
        val prev = undoSkipTime ?: return
        undoSkipTime = null
        seek(prev)
        showToast("Đã hoàn tác vị trí trước khi bỏ qua")
    }

    private fun maybeSyncHistory(sec: Double) {
        if (sec <= 5.0 || duration <= 10.0) return
        if (abs(sec - lastSavedProgressTimestamp) >= 12.0) {
            lastSavedProgressTimestamp = sec
            val epName = currentEpisode?.name ?: "Tập ${episodeIndex + 1}"
            scope.launch {
                AppState.recordProgress(
                    movie = movie,
                    episodeIndex = episodeIndex,
                    episodeName = epName,
                    currentTime = sec,
                    duration = duration
                )
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPlaying = false
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
            exoPlayer.playbackParameters = PlaybackParameters(if (isTempBoostActive) AppPreferences.longPressSpeed else playbackRate)
            isPlaying = true
        }
    }

    fun seek(seconds: Double) {
        val upper = if (duration > 0.0) duration else seconds
        val clamped = max(0.0, min(seconds, upper))
        exoPlayer.seekTo((clamped * 1000.0).toLong())
        currentTime = clamped
    }

    fun seekRelative(delta: Double) {
        seek(currentTime + delta)
    }

    fun setRate(rate: Float) {
        playbackRate = rate
        if (!isTempBoostActive) {
            exoPlayer.playbackParameters = PlaybackParameters(rate)
        }
        val formatted = if (rate % 1f == 0f) "${rate.toInt()}x" else "${rate}x"
        showToast("Tốc độ $formatted")
    }

    fun setTemporaryBoostRate(boosting: Boolean, boostRate: Float = AppPreferences.longPressSpeed) {
        isTempBoostActive = boosting
        val target = if (boosting) boostRate else playbackRate
        exoPlayer.playbackParameters = PlaybackParameters(target)
    }

    fun updateAspectMode(mode: PlayerAspectMode) {
        aspectMode = mode
    }

    fun cycleAspectMode() {
        val all = PlayerAspectMode.entries
        val next = all[(aspectMode.ordinal + 1) % all.size]
        updateAspectMode(next)
        showToast("Tỉ lệ: ${next.title}")
    }

    fun skipActiveMarker() {
        val marker = activeSkipPrompt ?: return
        val endSec = effectiveEnd(marker)
        skippedCutIds.add(marker.id)
        activeSkipPrompt = null
        recordUndoAnchor(currentTime)
        seek(endSec + 0.1)
    }

    fun dismissSkipPrompt() {
        activeSkipPrompt?.let { dismissedSkipIds.add(it.id) }
        activeSkipPrompt = null
    }

    fun handleOutroAction() {
        val marker = activeOutroPrompt ?: return
        dismissedSkipIds.add(marker.id)
        activeOutroPrompt = null
        if (hasNextEpisode) {
            nextEpisode()
        } else {
            val endSec = effectiveEnd(marker)
            recordUndoAnchor(currentTime)
            seek(min(endSec + 0.1, max(0.0, duration - 1.0)))
        }
    }

    fun dismissOutroPrompt() {
        activeOutroPrompt?.let { dismissedSkipIds.add(it.id) }
        activeOutroPrompt = null
    }

    fun selectEpisode(serverIdx: Int, epIdx: Int) {
        serverIndex = serverIdx
        episodeIndex = epIdx
        pendingResumeSeconds = AppState.savedResumeTime(movie.slug, epIdx)
        loadCurrentEpisode()
    }

    fun previousEpisode() {
        if (!hasPreviousEpisode) return
        selectEpisode(serverIndex, episodeIndex - 1)
    }

    fun nextEpisode() {
        if (!hasNextEpisode) return
        selectEpisode(serverIndex, episodeIndex + 1)
    }

    fun refreshAudioEffects() {
        refreshAudioEffectsInternal(showBanner = true)
    }

    private fun applyComfortVolume() {
        exoPlayer.volume = AppPreferences.comfortGain.coerceIn(0.25f, 1.0f)
    }

    private fun refreshAudioEffectsInternal(showBanner: Boolean) {
        applyComfortVolume()
        val sessionId = exoPlayer.audioSessionId
        if (sessionId > 0) {
            SpatialAudioController.attachToAudioSession(appContext, sessionId)
        }
        if (showBanner) {
            showToast(SpatialAudioController.activeSpatialSummary(appContext))
        }
    }

    fun showToast(text: String) {
        statusToast = text
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(2_000L)
            if (statusToast == text) {
                statusToast = null
            }
        }
    }

    fun shutdown() {
        if (isShutDown) return
        isShutDown = true

        val cur = currentTime
        val dur = duration
        val epName = currentEpisode?.name ?: "Tập ${episodeIndex + 1}"
        if (cur > 5.0 && dur > 10.0) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    AppState.recordProgress(
                        movie = movie,
                        episodeIndex = episodeIndex,
                        episodeName = epName,
                        currentTime = cur,
                        duration = dur
                    )
                }
            }
        }

        watchdogJob?.cancel()
        adsFetchJob?.cancel()
        promptAutoHideJob?.cancel()
        undoClearJob?.cancel()
        toastJob?.cancel()
        tickerJob?.cancel()

        exoPlayer.removeListener(playerListener)
        runCatching { exoPlayer.stop() }
        runCatching { mediaSession?.release() }
        mediaSession = null
        runCatching { exoPlayer.release() }
        SpatialAudioController.release()
        scope.cancel()
    }

    companion object {
        fun formatTime(seconds: Double): String {
            if (!seconds.isFinite() || seconds < 0.0) return "00:00"
            val total = seconds.toInt()
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) {
                String.format("%d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }
        }
    }
}
