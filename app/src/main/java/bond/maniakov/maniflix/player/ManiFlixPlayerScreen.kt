package bond.maniakov.maniflix.player

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.KeyEvent as AndroidKeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import bond.maniakov.maniflix.BuildConfig
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.ExternalPlayerOption
import bond.maniakov.maniflix.data.SoundProfileMode
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@OptIn(UnstableApi::class)
@Composable
fun ManiFlixPlayerScreen(
    engine: PlayerEngine,
    isInPiP: Boolean = PlayerSessionHolder.isInPiPMode,
    onEnterPiP: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val accentColor = ManiFlixColors.accent()

    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var showAudioDrawer by remember { mutableStateOf(false) }

    var isLongPressBoosting by remember { mutableStateOf(false) }
    var seekPulseText by remember { mutableStateOf<String?>(null) }
    var gestureHudIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureHudValue by remember { mutableFloatStateOf(0f) }

    var isDraggingScrubber by remember { mutableStateOf(false) }
    var scrubPreviewTime by remember { mutableFloatStateOf(0f) }
    var interactionNonce by remember { mutableStateOf(0) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val isTvMode = BuildConfig.IS_TV_BUILD
    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    val supportsPiP = remember(context) {
        !isTvMode &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun triggerHaptic(type: HapticFeedbackType = HapticFeedbackType.TextHandleMove) {
        if (AppPreferences.hapticsEnabled) {
            haptics.performHapticFeedback(type)
        }
    }

    fun handleClose() {
        OrientationHelper.forcePortrait(activity)
        onClose()
    }

    fun bumpAutoHide() {
        interactionNonce += 1
    }

    fun triggerSeekPulse(text: String) {
        seekPulseText = text
        triggerHaptic()
        scope.launch {
            delay(650L)
            if (seekPulseText == text) {
                seekPulseText = null
            }
        }
    }

    BackHandler(enabled = !isInPiP) {
        handleClose()
    }

    DisposableEffect(activity) {
        val window = activity?.window
        val controller = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else {
            null
        }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        if (AppPreferences.autoRotateLandscape) {
            OrientationHelper.setPlayerLandscape(activity, true)
        }

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (window != null) {
                window.attributes = window.attributes.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
            OrientationHelper.forcePortrait(activity)
        }
    }

    LaunchedEffect(showControls, engine.isPlaying, isDraggingScrubber, interactionNonce, isLocked, isInPiP) {
        if (showControls && engine.isPlaying && !isDraggingScrubber && !isInPiP) {
            delay(4_000L)
            showControls = false
        }
    }

    LaunchedEffect(isInPiP, showControls, showEpisodeDrawer, showAudioDrawer) {
        if (!isInPiP && !showEpisodeDrawer && !showAudioDrawer) {
            if (showControls && isTvMode && !isLocked) {
                runCatching { playPauseFocusRequester.requestFocus() }
            } else {
                runCatching { rootFocusRequester.requestFocus() }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (isInPiP || keyEvent.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                val code = keyEvent.nativeKeyEvent.keyCode
                when (code) {
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        engine.togglePlayPause()
                        showControls = true
                        bumpAutoHide()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                        if (!engine.isPlaying) engine.togglePlayPause()
                        showControls = true
                        bumpAutoHide()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        if (engine.isPlaying) engine.togglePlayPause()
                        showControls = true
                        bumpAutoHide()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        if (!isLocked) {
                            val sec = AppPreferences.doubleTapSeekSeconds
                            engine.seekRelative(sec.toDouble())
                            triggerSeekPulse("+${sec}s")
                            bumpAutoHide()
                            true
                        } else false
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                        if (!isLocked) {
                            val sec = AppPreferences.doubleTapSeekSeconds
                            engine.seekRelative(-sec.toDouble())
                            triggerSeekPulse("-${sec}s")
                            bumpAutoHide()
                            true
                        } else false
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP -> {
                        if (!isLocked && engine.hasNextEpisode) {
                            engine.nextEpisode()
                            showControls = true
                            bumpAutoHide()
                            true
                        } else false
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        if (!isLocked && engine.hasPreviousEpisode) {
                            engine.previousEpisode()
                            showControls = true
                            bumpAutoHide()
                            true
                        } else false
                    }
                    AndroidKeyEvent.KEYCODE_INFO,
                    AndroidKeyEvent.KEYCODE_GUIDE -> {
                        showControls = !showControls
                        if (showControls) bumpAutoHide()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_CAPTIONS -> {
                        if (!isLocked) {
                            showControls = true
                            showAudioDrawer = !showAudioDrawer
                            bumpAutoHide()
                            true
                        } else false
                    }
                    AndroidKeyEvent.KEYCODE_MENU -> {
                        if (!isLocked) {
                            showControls = true
                            showEpisodeDrawer = !showEpisodeDrawer
                            bumpAutoHide()
                            true
                        } else false
                    }
                    AndroidKeyEvent.KEYCODE_BACK -> {
                        when {
                            showEpisodeDrawer -> { showEpisodeDrawer = false; true }
                            showAudioDrawer -> { showAudioDrawer = false; true }
                            isTvMode && showControls -> { showControls = false; true }
                            else -> { handleClose(); true }
                        }
                    }
                    else -> {
                        if (showEpisodeDrawer || showAudioDrawer) {
                            false
                        } else if (!showControls) {
                            when (code) {
                                AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                                AndroidKeyEvent.KEYCODE_ENTER,
                                AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                    if (!isLocked && engine.activeSkipPrompt != null) {
                                        engine.skipActiveMarker()
                                        triggerSeekPulse("Đã bỏ qua")
                                    } else if (!isLocked && engine.activeOutroPrompt != null) {
                                        engine.handleOutroAction()
                                    } else {
                                        engine.togglePlayPause()
                                        showControls = true
                                        bumpAutoHide()
                                    }
                                    true
                                }
                                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                    if (!isLocked) {
                                        val sec = AppPreferences.doubleTapSeekSeconds
                                        engine.seekRelative(-sec.toDouble())
                                        triggerSeekPulse("-${sec}s")
                                        bumpAutoHide()
                                        true
                                    } else false
                                }
                                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (!isLocked) {
                                        val sec = AppPreferences.doubleTapSeekSeconds
                                        engine.seekRelative(sec.toDouble())
                                        triggerSeekPulse("+${sec}s")
                                        bumpAutoHide()
                                        true
                                    } else false
                                }
                                AndroidKeyEvent.KEYCODE_DPAD_UP,
                                AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                    showControls = true
                                    bumpAutoHide()
                                    true
                                }
                                else -> false
                            }
                        } else {
                            if (code == AndroidKeyEvent.KEYCODE_DPAD_UP ||
                                code == AndroidKeyEvent.KEYCODE_DPAD_DOWN ||
                                code == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
                                code == AndroidKeyEvent.KEYCODE_DPAD_RIGHT ||
                                code == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                                code == AndroidKeyEvent.KEYCODE_ENTER
                            ) {
                                bumpAutoHide()
                            }
                            false
                        }
                    }
                }
            }
    ) {
        // Video surface (pure video in PiP, aspect & flip bound)
        val explicitRatio = engine.aspectMode.explicitRatio
        val resizeMode = when (engine.aspectMode) {
            PlayerAspectMode.FIT_SAFE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            PlayerAspectMode.EDGE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            PlayerAspectMode.RATIO_16_9,
            PlayerAspectMode.RATIO_4_3,
            PlayerAspectMode.RATIO_21_9 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val containerRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 16f / 9f
            val videoModifier = if (explicitRatio != null) {
                val sized = if (containerRatio > explicitRatio) {
                    Modifier.fillMaxHeight().aspectRatio(explicitRatio)
                } else {
                    Modifier.fillMaxWidth().aspectRatio(explicitRatio)
                }
                sized.graphicsLayer {
                    scaleX = if (engine.flipHorizontal) -1f else 1f
                    scaleY = if (engine.flipVertical) -1f else 1f
                }
            } else {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = if (engine.flipHorizontal) -1f else 1f
                        scaleY = if (engine.flipVertical) -1f else 1f
                    }
            }

            AndroidView(
                modifier = videoModifier,
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setKeepContentOnPlayerReset(true)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        player = engine.exoPlayer
                        this.resizeMode = resizeMode
                    }
                },
                update = { view ->
                    if (view.player !== engine.exoPlayer) {
                        view.player = engine.exoPlayer
                    }
                    if (view.resizeMode != resizeMode) {
                        view.resizeMode = resizeMode
                    }
                }
            )
        }

        if (!isInPiP) {
            // Full-screen touch gesture layer (tap, double-tap seek, long-press boost, vertical brightness/volume)
            val audioManager = remember(context) {
                context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked, containerSize) {
                        detectTapGestures(
                            onPress = {
                                try {
                                    awaitRelease()
                                } finally {
                                    if (isLongPressBoosting) {
                                        isLongPressBoosting = false
                                        engine.setTemporaryBoostRate(false)
                                    }
                                }
                            },
                            onTap = {
                                showControls = !showControls
                                if (showControls) bumpAutoHide()
                            },
                            onDoubleTap = { offset ->
                                if (isLocked) return@detectTapGestures
                                val width = containerSize.width.coerceAtLeast(1)
                                val delta = AppPreferences.doubleTapSeekSeconds.toDouble()
                                if (offset.x < width / 2f) {
                                    engine.seekRelative(-delta)
                                    triggerSeekPulse("-${delta.toInt()}s")
                                } else {
                                    engine.seekRelative(delta)
                                    triggerSeekPulse("+${delta.toInt()}s")
                                }
                            },
                            onLongPress = {
                                if (isLocked) return@detectTapGestures
                                isLongPressBoosting = true
                                engine.setTemporaryBoostRate(true, AppPreferences.longPressSpeed)
                                triggerHaptic(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                    .pointerInput(isLocked, containerSize) {
                        var isLeftSide = true
                        var startBrightness = 0.5f
                        var accumulatedDy = 0f
                        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
                        var startVolumeRatio = 0.5f

                        detectVerticalDragGestures(
                            onDragStart = { startOffset ->
                                if (isLocked) return@detectVerticalDragGestures
                                val width = containerSize.width.coerceAtLeast(1)
                                isLeftSide = startOffset.x < width / 2f
                                accumulatedDy = 0f
                                if (isLeftSide) {
                                    val lp = activity?.window?.attributes
                                    val raw = lp?.screenBrightness ?: -1f
                                    startBrightness = if (raw in 0.01f..1f) raw else 0.5f
                                } else {
                                    val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2)
                                    startVolumeRatio = curVol.toFloat() / maxVol.toFloat()
                                }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                if (isLocked) return@detectVerticalDragGestures
                                accumulatedDy += dragAmount
                                val travel = (containerSize.height * 0.55f).coerceAtLeast(240f)
                                val delta = -accumulatedDy / travel
                                if (isLeftSide) {
                                    val next = (startBrightness + delta).coerceIn(0.05f, 1.0f)
                                    activity?.window?.let { win ->
                                        val attrs = win.attributes
                                        attrs.screenBrightness = next
                                        win.attributes = attrs
                                    }
                                    gestureHudIcon = Icons.Rounded.WbSunny
                                    gestureHudValue = next
                                } else {
                                    val next = (startVolumeRatio + delta).coerceIn(0f, 1f)
                                    val targetVol = (next * maxVol).toInt().coerceIn(0, maxVol)
                                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    gestureHudIcon = Icons.AutoMirrored.Rounded.VolumeUp
                                    gestureHudValue = next
                                }
                            },
                            onDragEnd = {
                                gestureHudIcon = null
                            },
                            onDragCancel = {
                                gestureHudIcon = null
                            }
                        )
                    }
            )

            // Center buffering / error / seek pulse
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    seekPulseText != null -> {
                        GlassPill {
                            Text(
                                text = seekPulseText.orEmpty(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    engine.isFailed -> {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xD9161618))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "Không thể phát tập này",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (engine.errorMessage.isNotBlank()) {
                                Text(
                                    text = engine.errorMessage,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = { engine.loadCurrentEpisode() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = CircleShape
                            ) {
                                Text("Thử lại", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    engine.isBuffering -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            // Brightness / Volume HUD
            val hudIcon = gestureHudIcon
            if (hudIcon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    GlassPill {
                        Icon(
                            imageVector = hudIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        LinearProgressIndicator(
                            progress = { gestureHudValue },
                            modifier = Modifier
                                .width(110.dp)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.28f)
                        )
                    }
                }
            }

            // Long-press boost badge
            if (isLongPressBoosting) {
                val speedText = if (AppPreferences.longPressSpeed % 1f == 0f) {
                    "${AppPreferences.longPressSpeed.toInt()}x"
                } else {
                    "${AppPreferences.longPressSpeed}x"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    GlassPill {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = speedText,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            softWrap = false
                        )
                    }
                }
            }

            // Status toast
            engine.statusToast?.let { toast ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 56.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    GlassPill {
                        Text(
                            text = toast,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Skip Intro/Recap or Outro floating prompt (bottom-right)
            if (!isLocked) {
                val bottomPadding = if (showControls) 104.dp else 24.dp
                val skipPrompt = engine.activeSkipPrompt
                val outroPrompt = engine.activeOutroPrompt

                if (skipPrompt != null) {
                    val label = when (skipPrompt.type) {
                        "recap" -> "Bỏ qua tóm tắt"
                        "intro" -> "Bỏ qua phần giới thiệu"
                        else -> "Bỏ qua phân đoạn"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(end = 20.dp, bottom = bottomPadding),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        GlassPill(clickable = true, onClick = { engine.skipActiveMarker() }) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Đóng",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { engine.dismissSkipPrompt() }
                            )
                        }
                    }
                } else if (outroPrompt != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(end = 20.dp, bottom = bottomPadding),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        GlassPill(clickable = true, onClick = { engine.handleOutroAction() }) {
                            Text(
                                text = if (engine.hasNextEpisode) "Tập tiếp theo" else "Bỏ qua kết thúc",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Đóng",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { engine.dismissOutroPrompt() }
                            )
                        }
                    }
                }

                // Undo auto-skip pill (bottom-left)
                if (engine.undoSkipTime != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(start = 20.dp, bottom = bottomPadding),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        GlassPill(clickable = true, onClick = { engine.undoLastSkip() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Hoàn tác",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Main Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(top = 12.dp, end = 16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        GlassCircleButton(
                            icon = Icons.Rounded.LockOpen,
                            contentDescription = "Mở khóa",
                            size = 44.dp
                        ) {
                            isLocked = false
                            bumpAutoHide()
                            triggerHaptic()
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.65f),
                                    0.25f to Color.Transparent,
                                    0.72f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.78f)
                                )
                            )
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GlassCircleButton(
                                icon = Icons.Rounded.Close,
                                contentDescription = "Đóng trình phát",
                                size = 38.dp,
                                onClick = { handleClose() }
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = engine.movie.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = engine.currentEpisode?.name ?: "Tập 1",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val rateLabel = if (engine.playbackRate % 1f == 0f) {
                                "${engine.playbackRate.toInt()}x"
                            } else {
                                "${engine.playbackRate}x"
                            }
                            GlassPill(
                                clickable = true,
                                horizontalPadding = 10.dp,
                                onClick = {
                                    bumpAutoHide()
                                    showAudioDrawer = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Âm & Tốc độ",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Âm & Tốc độ ($rateLabel)",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (supportsPiP) {
                                GlassCircleButton(
                                    icon = Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = "Hình trong hình",
                                    size = 36.dp
                                ) {
                                    OrientationHelper.forcePortrait(activity)
                                    onEnterPiP()
                                }
                            }

                            GlassCircleButton(
                                icon = Icons.Rounded.Lock,
                                contentDescription = "Khóa màn hình",
                                size = 36.dp
                            ) {
                                isLocked = true
                                triggerHaptic()
                            }
                        }

                        // Center Skip/Play Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassCircleButton(
                                icon = Icons.Rounded.Replay10,
                                contentDescription = "Tua lùi",
                                size = 50.dp
                            ) {
                                engine.seekRelative(-AppPreferences.doubleTapSeekSeconds.toDouble())
                                bumpAutoHide()
                                triggerHaptic()
                            }

                            GlassCircleButton(
                                icon = if (engine.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (engine.isPlaying) "Tạm dừng" else "Phát",
                                size = 64.dp,
                                modifier = Modifier.focusRequester(playPauseFocusRequester)
                            ) {
                                engine.togglePlayPause()
                                bumpAutoHide()
                                triggerHaptic()
                            }

                            GlassCircleButton(
                                icon = Icons.Rounded.Forward10,
                                contentDescription = "Tua tới",
                                size = 50.dp
                            ) {
                                engine.seekRelative(AppPreferences.doubleTapSeekSeconds.toDouble())
                                bumpAutoHide()
                                triggerHaptic()
                            }
                        }

                        // Bottom Scrubber & Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val displayedTime = if (isDraggingScrubber) {
                                    scrubPreviewTime.toDouble()
                                } else {
                                    engine.currentTime
                                }
                                Text(
                                    text = engine.formatTime(displayedTime),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val maxDuration = engine.duration.coerceAtLeast(1.0).toFloat()
                                    val markers = engine.filteredTimelineMarkers
                                    if (engine.duration > 1.0 && markers.isNotEmpty()) {
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp)
                                                .height(4.dp)
                                        ) {
                                            val dur = engine.duration.coerceAtLeast(1.0)
                                            for (mk in markers) {
                                                val st = (mk.start / dur).coerceIn(0.0, 1.0).toFloat()
                                                val en = (engine.effectiveEnd(mk) / dur).coerceIn(st.toDouble(), 1.0).toFloat()
                                                val x = size.width * st
                                                val w = (size.width * (en - st)).coerceAtLeast(4f)
                                                drawRoundRect(
                                                    color = markerSegmentColor(mk.type),
                                                    topLeft = Offset(x, 0f),
                                                    size = Size(w, size.height),
                                                    cornerRadius = CornerRadius(4f, 4f)
                                                )
                                            }
                                        }
                                    }

                                    Slider(
                                        value = if (isDraggingScrubber) {
                                            scrubPreviewTime.coerceIn(0f, maxDuration)
                                        } else {
                                            engine.currentTime.toFloat().coerceIn(0f, maxDuration)
                                        },
                                        onValueChange = { newVal ->
                                            isDraggingScrubber = true
                                            scrubPreviewTime = newVal
                                        },
                                        onValueChangeFinished = {
                                            isDraggingScrubber = false
                                            engine.seek(scrubPreviewTime.toDouble())
                                            bumpAutoHide()
                                        },
                                        valueRange = 0f..maxDuration,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = accentColor,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Text(
                                    text = engine.formatTime(engine.duration),
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 12.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (engine.hasPreviousEpisode) {
                                    GlassCircleButton(
                                        icon = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Tập trước",
                                        size = 32.dp
                                    ) {
                                        engine.previousEpisode()
                                        bumpAutoHide()
                                    }
                                }

                                GlassPill(
                                    clickable = true,
                                    horizontalPadding = 10.dp,
                                    modifier = Modifier.weight(1f, fill = false),
                                    onClick = {
                                        bumpAutoHide()
                                        showEpisodeDrawer = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GridView,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Tập & nguồn",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (engine.hasNextEpisode) {
                                    GlassCircleButton(
                                        icon = Icons.Rounded.SkipNext,
                                        contentDescription = "Tập tiếp theo",
                                        size = 32.dp
                                    ) {
                                        engine.nextEpisode()
                                        bumpAutoHide()
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                GlassPill(
                                    clickable = true,
                                    horizontalPadding = 9.dp,
                                    onClick = {
                                        engine.cycleAspectMode()
                                        bumpAutoHide()
                                        triggerHaptic()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = engine.aspectMode.title,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                GlassPill(
                                    clickable = true,
                                    horizontalPadding = 9.dp,
                                    onClick = {
                                        OrientationHelper.setPlayerLandscape(activity, !isLandscape)
                                        bumpAutoHide()
                                        triggerHaptic()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ScreenRotation,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isLandscape) "Dọc" else "Ngang",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Episode & Server Modal Sheet
    if (showEpisodeDrawer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showEpisodeDrawer = false },
            sheetState = sheetState,
            containerColor = Color(0xFF18181B),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tập & nguồn phát",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showEpisodeDrawer = false }) {
                        Text("Xong", color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (engine.servers.isNotEmpty()) {
                    Text(
                        text = "Máy chủ",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        engine.servers.forEachIndexed { idx, srv ->
                            val selected = idx == engine.serverIndex
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val maxIdx = (srv.serverData.size - 1).coerceAtLeast(0)
                                    engine.selectEpisode(idx, engine.episodeIndex.coerceAtMost(maxIdx))
                                },
                                label = { Text(srv.serverName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }

                val currentServer = engine.servers.getOrNull(engine.serverIndex)
                if (currentServer != null) {
                    Text(
                        text = "Danh sách tập (${currentServer.serverData.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 84.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(currentServer.serverData) { epIdx, ep ->
                            val selected = epIdx == engine.episodeIndex
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) accentColor else Color.White.copy(alpha = 0.09f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        engine.selectEpisode(engine.serverIndex, epIdx)
                                        showEpisodeDrawer = false
                                    }
                            ) {
                                Text(
                                    text = ep.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Audio / Aspect / Speed / External Player Modal Sheet
    if (showAudioDrawer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val outputLabel = remember(showAudioDrawer) { SpatialAudioController.currentAudioOutputLabel(context) }

        ModalBottomSheet(
            onDismissRequest = { showAudioDrawer = false },
            sheetState = sheetState,
            containerColor = Color(0xFF18181B),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Âm thanh & phát",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAudioDrawer = false }) {
                        Text("Xong", color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Audio Output Info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DiagnosticRow("Đầu ra âm thanh", outputLabel)
                    }
                }

                Text("Cấu hình âm thanh", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundProfileMode.entries.forEach { profile ->
                        FilterChip(
                            selected = AppPreferences.soundProfile == profile,
                            onClick = {
                                AppPreferences.soundProfile = profile
                                engine.refreshAudioEffects()
                            },
                            label = { Text(profile.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tăng cường âm lượng", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Text("${(AppPreferences.comfortGain * 100).toInt()}%", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
                    }
                    Slider(
                        value = AppPreferences.comfortGain,
                        onValueChange = {
                            AppPreferences.comfortGain = it
                            engine.refreshAudioEffects()
                        },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Text("Tỉ lệ & khung hình", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerAspectMode.entries.forEach { mode ->
                        FilterChip(
                            selected = engine.aspectMode == mode,
                            onClick = { engine.updateAspectMode(mode) },
                            label = { Text(mode.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lật ngang khung hình", fontSize = 14.sp)
                    Switch(
                        checked = engine.flipHorizontal,
                        onCheckedChange = { engine.flipHorizontal = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lật dọc khung hình", fontSize = 14.sp)
                    Switch(
                        checked = engine.flipVertical,
                        onCheckedChange = { engine.flipVertical = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Text("Tốc độ phát", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                        val selected = engine.playbackRate == rate
                        val label = if (rate % 1f == 0f) "${rate.toInt()}x" else "${rate}x"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) accentColor else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { engine.setRate(rate) }
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Text("Liên kết & mở ngoài", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                val targetStream = engine.activeStreamUrl.ifBlank { engine.currentEpisode?.linkM3u8.orEmpty() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = targetStream.isNotBlank()) {
                            clipboard.setText(AnnotatedString(targetStream))
                            engine.showToast("Đã sao chép liên kết HLS")
                            showAudioDrawer = false
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Text("Sao chép liên kết .m3u8", fontSize = 14.sp)
                }

                ExternalPlayerOption.entries
                    .filter { it != ExternalPlayerOption.INTERNAL }
                    .forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = targetStream.isNotBlank()) {
                                    val opened = openInExternalPlayer(context, targetStream, option.packageName)
                                    if (!opened) {
                                        engine.showToast("Thiết bị chưa cài ${option.title}")
                                    } else {
                                        showAudioDrawer = false
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Text("Mở bằng ${option.title}", fontSize = 14.sp)
                        }
                    }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
        Text(text = value, fontSize = 12.sp, color = Color.White.copy(alpha = 0.58f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GlassPill(
    modifier: Modifier = Modifier,
    clickable: Boolean = false,
    horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val pillModifier = modifier
        .clip(CircleShape)
        .background(if (isFocused && clickable) Color(0xCC24242C) else Color(0xA6141418))
        .border(
            width = if (isFocused && clickable) 2.dp else 1.dp,
            color = if (isFocused && clickable) Color.White else Color.White.copy(alpha = 0.16f),
            shape = CircleShape
        )
        .let { base ->
            if (clickable && onClick != null) {
                base
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable(onClick = onClick)
            } else {
                base
            }
        }
        .padding(horizontal = horizontalPadding, vertical = 7.dp)

    Row(
        modifier = pillModifier,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isFocused) Color(0xCC282834) else Color(0xA6141418))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.16f),
                shape = CircleShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size((size.value * 0.52f).dp)
        )
    }
}

private fun markerSegmentColor(type: String?): Color = when (type) {
    "ad" -> Color(0xFF22C55E)
    "recap" -> Color(0xFFEC4899)
    "credit", "outro" -> Color(0xFFF59E0B)
    else -> Color(0xFF06B6D4)
}

private fun openInExternalPlayer(context: Context, url: String, packageName: String?): Boolean {
    if (url.isBlank()) return false
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            if (!packageName.isNullOrBlank()) {
                setPackage(packageName)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
