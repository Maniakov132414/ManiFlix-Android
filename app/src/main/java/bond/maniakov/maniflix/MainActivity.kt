package bond.maniakov.maniflix

import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.BuildConfig
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.ManiFlixApi
import bond.maniakov.maniflix.data.UpdateManager
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.player.ManiFlixPlayerScreen
import bond.maniakov.maniflix.player.OrientationHelper
import bond.maniakov.maniflix.player.PlayerEngine
import bond.maniakov.maniflix.player.PlayerSessionHolder
import bond.maniakov.maniflix.ui.AuthSheetDialog
import bond.maniakov.maniflix.ui.CachedPosterImage
import bond.maniakov.maniflix.ui.DiscoverScreen
import bond.maniakov.maniflix.ui.LibraryScreen
import bond.maniakov.maniflix.ui.MovieDetailScreen
import bond.maniakov.maniflix.ui.SearchScreen
import bond.maniakov.maniflix.ui.SettingsScreen
import bond.maniakov.maniflix.ui.TvShellScreen
import bond.maniakov.maniflix.ui.UpdatePromptDialog
import bond.maniakov.maniflix.ui.appleGlassSurfaceModifier
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import bond.maniakov.maniflix.ui.theme.ManiFlixTheme
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        AppPreferences.init(applicationContext)
        AppState.init(applicationContext)
        configureCoilImageLoader(applicationContext)
        AppState.onLaunchInternalPlayer = { ctx, movie, servers, serverIdx, episodeIdx, resumeSec ->
            PlayerSessionHolder.open(ctx, movie, servers, serverIdx, episodeIdx, resumeSec)
        }

        handleDeepLink(intent)

        setContent {
            ManiFlixTheme {
                val isLight = ManiFlixColors.isLight()
                val isPlayerActive = AppState.isPlayerPresented && PlayerSessionHolder.activeEngine != null
                DisposableEffect(isLight, isPlayerActive) {
                    val lightBars = isLight && !isPlayerActive
                    enableEdgeToEdge(
                        statusBarStyle = if (lightBars) {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        },
                        navigationBarStyle = if (lightBars) {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        }
                    )
                    onDispose {}
                }

                RootScaffold(
                    onEnterPiP = { enterPiPMode() },
                    onClosePlayer = { closeInternalPlayer() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "maniflix" && data.host == "movie") {
            val slug = data.pathSegments.firstOrNull { it.isNotBlank() }
                ?: data.lastPathSegment?.takeIf { it.isNotBlank() && it != "movie" }
            if (!slug.isNullOrBlank()) {
                val resumeParam = data.getQueryParameter("resume")
                AppState.deepLinkAutoWatch = resumeParam == "1" ||
                    resumeParam.equals("true", ignoreCase = true)
                AppState.deepLinkSlug = slug
            }
        }
    }

    private fun configureCoilImageLoader(appContext: Context) {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRamOrTv = am?.isLowRamDevice == true || BuildConfig.IS_TV_BUILD
        val loader = ImageLoader.Builder(appContext)
            .okHttpClient { ManiFlixApi.sharedImageOkHttpClient }
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.22)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.cacheDir, "coil_image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .allowHardware(true)
            .allowRgb565(isLowRamOrTv)
            .crossfade(true)
            .build()
        Coil.setImageLoader(loader)
    }

    fun enterPiPMode() {
        val supportsPiP = !BuildConfig.IS_TV_BUILD &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        if (supportsPiP) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (
            !BuildConfig.IS_TV_BUILD &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            AppState.isPlayerPresented &&
            AppPreferences.autoPiP &&
            PlayerSessionHolder.activeEngine != null
        ) {
            enterPiPMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerSessionHolder.isInPiPMode = isInPictureInPictureMode
    }

    private fun closeInternalPlayer() {
        AppState.isPlayerPresented = false
        PlayerSessionHolder.activeEngine?.shutdown()
        PlayerSessionHolder.stopCompletely()
        OrientationHelper.forcePortrait(this)
        AppState.requestImmediateWidgetRefresh()
    }
}

private enum class MainTab(
    val label: String,
    val icon: ImageVector
) {
    DISCOVER("Trang chủ", Icons.Rounded.VideoLibrary),
    SEARCH("Tìm kiếm", Icons.Rounded.Search),
    LIBRARY("Tủ phim", Icons.Rounded.Bookmarks),
    SETTINGS("Cài đặt", Icons.Rounded.Settings)
}

private data class DetailNavEntry(
    val slug: String,
    val seedMovie: MovieItem?
)

@Composable
private fun RootScaffold(
    onEnterPiP: () -> Unit,
    onClosePlayer: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.DISCOVER) }
    val detailStack = remember { mutableStateListOf<DetailNavEntry>() }

    LaunchedEffect(Unit) {
        AppState.bootstrapSession()
        UpdateManager.checkForUpdates(manual = false)
    }

    val rootContext = LocalContext.current
    val incomingDeepLink = AppState.deepLinkSlug
    LaunchedEffect(incomingDeepLink) {
        if (!incomingDeepLink.isNullOrBlank()) {
            val autoWatch = AppState.deepLinkAutoWatch
            if (detailStack.lastOrNull()?.slug != incomingDeepLink) {
                detailStack.add(DetailNavEntry(incomingDeepLink, null))
            }
            AppState.deepLinkSlug = null
            AppState.deepLinkAutoWatch = false
            if (autoWatch) {
                AppState.quickWatch(rootContext, incomingDeepLink)
            }
        }
    }

    val activeEngine = PlayerSessionHolder.activeEngine
    val isPlayerOverlayVisible = AppState.isPlayerPresented && activeEngine != null
    val isPiP = PlayerSessionHolder.isInPiPMode
    val showMiniPlayer = activeEngine != null && !AppState.isPlayerPresented && !isPiP

    BackHandler(enabled = !isPlayerOverlayVisible && detailStack.isNotEmpty()) {
        detailStack.removeAt(detailStack.lastIndex)
    }

    val bg = ManiFlixColors.background()
    val surface = ManiFlixColors.surface()
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val isTvMode = BuildConfig.IS_TV_BUILD
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        if (isTvMode) {
            val currentDetail = detailStack.lastOrNull()
            TvShellScreen(
                activeDetailSlug = currentDetail?.slug,
                activeDetailSeed = currentDetail?.seedMovie,
                onMovieClick = { slug, seed ->
                    detailStack.add(DetailNavEntry(slug, seed))
                },
                onCloseDetail = {
                    if (detailStack.isNotEmpty()) {
                        detailStack.removeAt(detailStack.lastIndex)
                    }
                },
                onClearDetails = {
                    detailStack.clear()
                }
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen && !isPlayerOverlayVisible && !isPiP) {
                NavigationRail(
                    containerColor = surface.copy(alpha = 0.96f),
                    modifier = Modifier.statusBarsPadding()
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                selectedTab = tab
                                detailStack.clear()
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = accent,
                                selectedTextColor = accent,
                                indicatorColor = accent.copy(alpha = 0.16f),
                                unselectedIconColor = textSecondary,
                                unselectedTextColor = textSecondary
                            )
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = bg,
                bottomBar = {
                    if (!isPlayerOverlayVisible && !isPiP) {
                        if (isWideScreen) {
                            if (showMiniPlayer && activeEngine != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MiniPlayerBar(
                                        engine = activeEngine,
                                        onExpand = { AppState.isPlayerPresented = true },
                                        onClose = onClosePlayer,
                                        modifier = Modifier
                                            .widthIn(max = 520.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (showMiniPlayer && activeEngine != null) {
                                    MiniPlayerBar(
                                        engine = activeEngine,
                                        onExpand = { AppState.isPlayerPresented = true },
                                        onClose = onClosePlayer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                NavigationBar(
                                    containerColor = surface.copy(alpha = 0.96f),
                                    tonalElevation = 0.dp
                                ) {
                                    MainTab.entries.forEach { tab ->
                                        val selected = selectedTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                selectedTab = tab
                                                detailStack.clear()
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.label,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = accent,
                                                selectedTextColor = accent,
                                                indicatorColor = accent.copy(alpha = 0.16f),
                                                unselectedIconColor = textSecondary,
                                                unselectedTextColor = textSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val bottomPadding = innerPadding.calculateBottomPadding() + 12.dp
                val currentDetail = detailStack.lastOrNull()
                if (currentDetail != null) {
                    MovieDetailScreen(
                        slug = currentDetail.slug,
                        seedMovie = currentDetail.seedMovie,
                        bottomContentPadding = bottomPadding,
                        onBack = {
                            if (detailStack.isNotEmpty()) {
                                detailStack.removeAt(detailStack.lastIndex)
                            }
                        },
                        onSelectMovie = { slug, seed ->
                            detailStack.add(DetailNavEntry(slug, seed))
                        }
                    )
                } else {
                    when (selectedTab) {
                        MainTab.DISCOVER -> DiscoverScreen(
                            bottomContentPadding = bottomPadding,
                            onMovieClick = { slug, seed ->
                                detailStack.add(DetailNavEntry(slug, seed))
                            }
                        )

                        MainTab.SEARCH -> SearchScreen(
                            bottomContentPadding = bottomPadding,
                            onMovieClick = { slug, seed ->
                                detailStack.add(DetailNavEntry(slug, seed))
                            }
                        )

                        MainTab.LIBRARY -> LibraryScreen(
                            bottomContentPadding = bottomPadding,
                            onMovieClick = { slug, seed ->
                                detailStack.add(DetailNavEntry(slug, seed))
                            }
                        )

                        MainTab.SETTINGS -> SettingsScreen(
                            bottomContentPadding = bottomPadding
                        )
                    }
                }
            }
        }
        }

        if (isPlayerOverlayVisible && activeEngine != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                ManiFlixPlayerScreen(
                    engine = activeEngine,
                    isInPiP = isPiP,
                    onEnterPiP = onEnterPiP,
                    onClose = onClosePlayer
                )
            }
        }

        AnimatedVisibility(
            visible = !AppState.globalBannerMessage.isNullOrBlank() && !isPiP,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ManiFlixColors.surfaceElevated().copy(alpha = 0.96f))
                    .border(0.7.dp, ManiFlixColors.cardBorder(), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = AppState.globalBannerMessage.orEmpty(),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (AppState.showAuthSheet) {
            AuthSheetDialog(
                onDismiss = { AppState.showAuthSheet = false }
            )
        }

        if (UpdateManager.showForceBlocker) {
            UpdatePromptDialog(
                isForced = true,
                onDismiss = {}
            )
        } else if (UpdateManager.showSoftPrompt) {
            UpdatePromptDialog(
                isForced = false,
                onDismiss = { UpdateManager.snoozeSoftUpdateFor24Hours() }
            )
        }

        if (!AppPreferences.hasCompletedLanguageSetup) {
            LanguageOnboardingOverlay(
                onSelectLanguage = { lang ->
                    AppPreferences.uiLanguage = lang
                    AppPreferences.defaultSubtitleLang = lang
                    AppPreferences.hasCompletedLanguageSetup = true
                }
            )
        }
    }
}

@Composable
private fun LanguageOnboardingOverlay(
    onSelectLanguage: (String) -> Unit
) {
    val surface = ManiFlixColors.surface()
    val border = ManiFlixColors.cardBorder()
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(surface)
                .border(1.dp, border, RoundedCornerShape(22.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chọn ngôn ngữ / Choose Language",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = "Ngôn ngữ giao diện và phụ đề mặc định sẽ được tự động thiết lập theo lựa chọn của bạn.",
                fontSize = 13.sp,
                color = textSecondary
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple("vi", "Tiếng Việt", "Giao diện Tiếng Việt + Phụ đề Tiếng Việt mặc định"),
                    Triple("en", "English", "English UI + English Subtitles by default")
                ).forEach { (code, title, subtitle) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ManiFlixColors.surfaceElevated())
                            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .focusable()
                            .clickable { onSelectLanguage(code) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    engine: PlayerEngine,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()

    Row(
        modifier = modifier
            .then(appleGlassSurfaceModifier(cornerRadius = 16.dp))
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CachedPosterImage(
            url = engine.movie.displayPosterUrl.ifBlank { engine.movie.displayThumbUrl },
            contentDescription = engine.movie.name,
            contentScale = ContentScale.Crop,
            maxWidthPx = 120,
            maxHeightPx = 120,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ManiFlixColors.surfaceElevated())
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = engine.movie.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = engine.currentEpisode?.name?.takeIf { it.isNotBlank() } ?: "Đang phát",
                fontSize = 11.5.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { engine.togglePlayPause() },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (engine.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (engine.isPlaying) "Tạm dừng" else "Phát",
                tint = textPrimary
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Đóng trình phát",
                tint = textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
