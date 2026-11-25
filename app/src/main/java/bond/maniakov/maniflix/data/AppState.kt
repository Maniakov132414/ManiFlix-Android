package bond.maniakov.maniflix.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import bond.maniakov.maniflix.model.BlockedMovieRecord
import bond.maniakov.maniflix.model.EpisodeServer
import bond.maniakov.maniflix.model.FavoriteEntry
import bond.maniakov.maniflix.model.HistoryEntry
import bond.maniakov.maniflix.model.MovieDetail
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object AppState {
    private const val PREFS_NAME = "maniflix_state"
    private const val KEY_AUTH_TOKEN = "auth.token"
    private const val KEY_BLOCKED = "prefs.blockedMovies.v1"
    private const val KEY_LIKED = "prefs.likedSlugs.v1"
    private const val KEY_LOCAL_HISTORY = "prefs.localHistory.v1"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var prefs: SharedPreferences? = null
    private var bannerJob: Job? = null

    private var _authToken by mutableStateOf("")
    var authToken: String
        get() = _authToken
        set(value) {
            _authToken = value
            prefs?.edit()?.putString(KEY_AUTH_TOKEN, value)?.apply()
        }

    var currentUser by mutableStateOf<UserInfo?>(null)
    val isLoggedIn: Boolean
        get() = authToken.isNotBlank() && currentUser != null

    var favorites by mutableStateOf<List<FavoriteEntry>>(emptyList())
    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
    var blockedMovies by mutableStateOf<List<BlockedMovieRecord>>(emptyList())
    var likedSlugs by mutableStateOf<Set<String>>(emptySet())
    var showAuthSheet by mutableStateOf(false)
    var isPlayerPresented by mutableStateOf(false)
    var deepLinkSlug by mutableStateOf<String?>(null)
    var globalBannerMessage by mutableStateOf<String?>(null)

    var onLaunchInternalPlayer: ((Context, MovieDetail, List<EpisodeServer>, Int, Int, Double) -> Unit)? = null

    fun init(context: Context) {
        val appCtx = context.applicationContext
        val sp = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        UpdateManager.init(appCtx)

        _authToken = sp.getString(KEY_AUTH_TOKEN, "").orEmpty()
        blockedMovies = decodeBlocked(sp.getString(KEY_BLOCKED, null))
        likedSlugs = sp.getStringSet(KEY_LIKED, emptySet())?.toSet() ?: emptySet()
        history = decodeHistory(sp.getString(KEY_LOCAL_HISTORY, null))
    }

    suspend fun bootstrapSession() {
        val token = authToken
        if (token.isBlank()) return
        try {
            val user = ManiFlixApi.fetchMe(token)
            currentUser = user
            refreshLibrary()
        } catch (e: ApiHttpException) {
            if (e.statusCode == 401) {
                logout()
            }
        } catch (_: Exception) {
            // Keep cached state when offline
        }
    }

    suspend fun refreshLibrary() {
        val token = authToken
        if (token.isBlank()) return
        coroutineScope {
            val histTask = async { runCatching { ManiFlixApi.fetchHistory(token) }.getOrNull() }
            val favTask = async { runCatching { ManiFlixApi.fetchFavorites(token) }.getOrNull() }
            histTask.await()?.let { serverHist ->
                history = serverHist
                persistLocalHistory()
            }
            favTask.await()?.let { serverFavs ->
                favorites = serverFavs
            }
        }
    }

    suspend fun handleAuthSuccess(token: String, user: UserInfo?) {
        authToken = token
        currentUser = user
        showAuthSheet = false
        refreshLibrary()
    }

    fun logout() {
        authToken = ""
        currentUser = null
        favorites = emptyList()
    }

    fun isFavorite(slug: String): Boolean = favorites.any { it.slug == slug }

    fun isBlocked(slug: String): Boolean = blockedMovies.any { it.slug == slug }

    fun hasHistory(slug: String): Boolean = history.any { it.slug == slug }

    suspend fun toggleFavorite(movie: MovieDetail) {
        val token = authToken
        if (token.isBlank()) {
            showAuthSheet = true
            return
        }
        try {
            val wasFav = isFavorite(movie.slug)
            val updated = ManiFlixApi.toggleFavorite(token, movie)
            if (updated != null) {
                favorites = updated
            } else {
                refreshLibrary()
            }
            showBanner(if (wasFav) "Đã xoá khỏi Tủ phim" else "Đã thêm vào Tủ phim")
        } catch (_: Exception) {
            showBanner("Không thể cập nhật Tủ phim")
        }
    }

    suspend fun toggleFavoriteItem(item: MovieItem) {
        val token = authToken
        if (token.isBlank()) {
            showAuthSheet = true
            return
        }
        try {
            val wasFav = isFavorite(item.slug)
            val updated = ManiFlixApi.toggleFavoriteItem(token, item)
            if (updated != null) {
                favorites = updated
            } else {
                refreshLibrary()
            }
            showBanner(if (wasFav) "Đã xoá khỏi Tủ phim" else "Đã thêm vào Tủ phim")
        } catch (_: Exception) {
            showBanner("Không thể cập nhật Tủ phim")
        }
    }

    fun likeMovie(item: MovieItem) {
        val next = likedSlugs.toMutableSet().apply { add(item.slug) }
        likedSlugs = next
        prefs?.edit()?.putStringSet(KEY_LIKED, next)?.apply()
        showBanner("Đã thích: sẽ gợi ý thêm phim tương tự")
    }

    fun dislikeMovie(item: MovieItem) {
        val next = likedSlugs.toMutableSet().apply { remove(item.slug) }
        likedSlugs = next
        prefs?.edit()?.putStringSet(KEY_LIKED, next)?.apply()
        showBanner("Đã ghi nhận: giảm bớt phim tương tự")
    }

    fun blockMovie(item: MovieItem) {
        if (isBlocked(item.slug)) return
        val record = BlockedMovieRecord(
            slug = item.slug,
            name = item.name,
            poster = item.posterUrl.ifBlank { item.thumbUrl }
        )
        blockedMovies = listOf(record) + blockedMovies
        persistBlocked()
        showBanner("Đã ẩn \"${item.name}\" khỏi đề xuất")
    }

    fun unblockMovie(slug: String) {
        blockedMovies = blockedMovies.filterNot { it.slug == slug }
        persistBlocked()
        showBanner("Đã bỏ ẩn phim")
    }

    fun showBanner(text: String) {
        globalBannerMessage = text
        bannerJob?.cancel()
        bannerJob = scope.launch {
            delay(2_200L)
            if (globalBannerMessage == text) {
                globalBannerMessage = null
            }
        }
    }

    suspend fun quickWatch(context: Context, slug: String) {
        showBanner("Đang mở trình phát...")
        try {
            val detail = ManiFlixApi.fetchMovieDetail(slug)
            val movie = detail.movie ?: run {
                showBanner("Không tìm thấy thông tin phim")
                return
            }
            val servers = detail.episodes
            val match = history.firstOrNull { it.slug == slug }
            val maxEpIndex = ((servers.firstOrNull()?.serverData?.size ?: 1) - 1).coerceAtLeast(0)
            val epIdx = (match?.episodeIndex ?: 0).coerceIn(0, maxEpIndex)
            startPlayback(context, movie, servers, 0, epIdx)
        } catch (_: Exception) {
            showBanner("Không tải được luồng phát")
        }
    }

    fun savedResumeTime(slug: String, episodeIndex: Int): Double {
        val match = history.firstOrNull { it.slug == slug && it.episodeIndex == episodeIndex } ?: return 0.0
        val cur = match.currentTime
        val dur = match.duration
        return if (cur > 10.0 && (dur <= 0.0 || cur < dur - 20.0)) cur else 0.0
    }

    suspend fun recordProgress(
        movie: MovieDetail,
        episodeIndex: Int,
        episodeName: String,
        currentTime: Double,
        duration: Double
    ) {
        val localEntry = HistoryEntry(
            slug = movie.slug,
            name = movie.name,
            poster = movie.posterUrl.ifBlank { movie.thumbUrl },
            episodeIndex = episodeIndex,
            episodeName = episodeName,
            currentTime = currentTime,
            duration = duration,
            updatedAt = System.currentTimeMillis()
        )
        history = listOf(localEntry) + history.filterNot { it.slug == movie.slug }
        persistLocalHistory()

        val token = authToken
        if (token.isNotBlank()) {
            ManiFlixApi.saveHistory(
                token = token,
                slug = movie.slug,
                name = movie.name,
                poster = movie.posterUrl.ifBlank { movie.thumbUrl },
                episodeIndex = episodeIndex,
                episodeName = episodeName,
                currentTime = currentTime,
                duration = duration
            )
        }
    }

    suspend fun deleteHistory(slug: String) {
        history = history.filterNot { it.slug == slug }
        persistLocalHistory()
        val token = authToken
        if (token.isNotBlank()) {
            ManiFlixApi.deleteHistory(token, slug)
        }
        showBanner("Đã xoá khỏi lịch sử xem")
    }

    fun startPlayback(
        context: Context,
        movie: MovieDetail,
        servers: List<EpisodeServer>,
        serverIndex: Int,
        episodeIndex: Int
    ) {
        val ep = servers.getOrNull(serverIndex)?.serverData?.getOrNull(episodeIndex)
        val extOption = AppPreferences.externalPlayer
        if (extOption != ExternalPlayerOption.INTERNAL && ep != null && ep.linkM3u8.isNotBlank()) {
            val cleaned = ManiFlixApi.cleanM3u8Url(ep.linkM3u8).ifBlank { ep.linkM3u8 }
            if (launchExternalPlayer(context, cleaned, movie.name, extOption)) {
                return
            }
            showBanner("Chưa cài đặt ${extOption.title}, chuyển về trình phát tích hợp")
        }

        val resume = savedResumeTime(movie.slug, episodeIndex)
        onLaunchInternalPlayer?.invoke(context, movie, servers, serverIndex, episodeIndex, resume)
        isPlayerPresented = true
    }

    fun shareMovie(context: Context, name: String, slug: String) {
        val shareText = "Xem \"$name\" trên ManiFlix: ${AppPreferences.BASE_URL}/?movie=$slug\nmaniflix://movie/$slug"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, "Chia sẻ \"$name\"").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    private fun launchExternalPlayer(
        context: Context,
        streamUrl: String,
        title: String,
        option: ExternalPlayerOption
    ): Boolean {
        val pkg = option.packageName ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(streamUrl), "video/*")
            setPackage(pkg)
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun persistBlocked() {
        val arr = JSONArray()
        for (item in blockedMovies) {
            arr.put(
                JSONObject().apply {
                    put("slug", item.slug)
                    put("name", item.name)
                    put("poster", item.poster)
                }
            )
        }
        prefs?.edit()?.putString(KEY_BLOCKED, arr.toString())?.apply()
    }

    private fun decodeBlocked(raw: String?): List<BlockedMovieRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val slug = obj.optString("slug", "").trim()
                    if (slug.isEmpty()) continue
                    add(
                        BlockedMovieRecord(
                            slug = slug,
                            name = obj.optString("name", slug),
                            poster = obj.optString("poster", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistLocalHistory() {
        val arr = JSONArray()
        for (item in history) {
            arr.put(
                JSONObject().apply {
                    put("slug", item.slug)
                    put("name", item.name)
                    put("poster", item.poster)
                    put("episodeIndex", item.episodeIndex)
                    put("episodeName", item.episodeName)
                    put("currentTime", item.currentTime)
                    put("duration", item.duration)
                    put("updatedAt", item.updatedAt)
                }
            )
        }
        prefs?.edit()?.putString(KEY_LOCAL_HISTORY, arr.toString())?.apply()
    }

    private fun decodeHistory(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val slug = obj.optString("slug", "").trim()
                    if (slug.isEmpty()) continue
                    add(
                        HistoryEntry(
                            slug = slug,
                            name = obj.optString("name", slug),
                            poster = obj.optString("poster", ""),
                            episodeIndex = obj.optInt("episodeIndex", 0),
                            episodeName = obj.optString("episodeName", ""),
                            currentTime = obj.optDouble("currentTime", 0.0),
                            duration = obj.optDouble("duration", 0.0),
                            updatedAt = obj.optLong("updatedAt", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
