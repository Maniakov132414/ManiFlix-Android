package bond.maniakov.maniflix.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AccentPreset
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.IntroSkipBehavior
import bond.maniakov.maniflix.data.ManiFlixApi
import bond.maniakov.maniflix.data.SoundProfileMode
import bond.maniakov.maniflix.data.THEME_PRESETS
import bond.maniakov.maniflix.data.UpdateManager
import bond.maniakov.maniflix.model.EpisodeServer
import bond.maniakov.maniflix.model.FranchiseSeason
import bond.maniakov.maniflix.model.HistoryEntry
import bond.maniakov.maniflix.model.MovieDetail
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.player.PlayerEngine
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TvTab(val label: String, val icon: ImageVector) {
    DISCOVER("Trang chủ", Icons.Rounded.VideoLibrary),
    SEARCH("Tìm kiếm", Icons.Rounded.Search),
    LIBRARY("Tủ phim", Icons.Rounded.Bookmarks),
    NETWORK("Kiểm tra mạng", Icons.Rounded.NetworkCheck),
    SETTINGS("Cài đặt", Icons.Rounded.Settings)
}

private data class TvCategoryOption(
    val label: String,
    val apiSlug: String?
)

private val TV_CATEGORIES = listOf(
    TvCategoryOption("Tất cả", null),
    TvCategoryOption("Phim bộ", "phim-bo"),
    TvCategoryOption("Phim lẻ", "phim-le"),
    TvCategoryOption("Hoạt hình", "hoat-hinh"),
    TvCategoryOption("TV Shows", "tv-shows")
)

private val TV_QUICK_TOPICS = listOf(
    "Hành động", "Hoạt hình", "Hài hước", "Kinh dị",
    "Cổ trang", "Viễn tưởng", "Anime", "Tình cảm"
)

private val TV_DPAD_KEYS = listOf(
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
    "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
    "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3",
    "4", "5", "6", "7", "8", "9"
)

@Composable
fun TvShellScreen(
    activeDetailSlug: String?,
    activeDetailSeed: MovieItem?,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit,
    onCloseDetail: () -> Unit,
    onClearDetails: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(TvTab.DISCOVER) }
    val bg = ManiFlixColors.background()
    val surface = ManiFlixColors.surface()
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (keyEvent.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_PROG_RED -> {
                        onClearDetails()
                        selectedTab = TvTab.DISCOVER
                        true
                    }
                    AndroidKeyEvent.KEYCODE_PROG_GREEN -> {
                        onClearDetails()
                        selectedTab = TvTab.SEARCH
                        true
                    }
                    AndroidKeyEvent.KEYCODE_PROG_YELLOW -> {
                        onClearDetails()
                        selectedTab = TvTab.LIBRARY
                        true
                    }
                    AndroidKeyEvent.KEYCODE_PROG_BLUE,
                    AndroidKeyEvent.KEYCODE_INFO,
                    AndroidKeyEvent.KEYCODE_GUIDE -> {
                        onClearDetails()
                        selectedTab = TvTab.NETWORK
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MENU -> {
                        onClearDetails()
                        selectedTab = TvTab.SETTINGS
                        true
                    }
                    else -> false
                }
            }
            .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Left 10-foot Leanback Navigation Rail
        Column(
            modifier = Modifier
                .width(204.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(surface.copy(alpha = 0.94f))
                .border(0.8.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ManiFlix",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "Android TV ${UpdateManager.currentVersion}",
                            fontSize = 10.5.sp,
                            color = textSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TvTab.entries.forEach { tab ->
                    val selected = activeDetailSlug == null && selectedTab == tab
                    TvRailButton(
                        icon = tab.icon,
                        label = tab.label,
                        selected = selected,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = {
                            onClearDetails()
                            selectedTab = tab
                        }
                    )
                }
            }

            // Account card at bottom of TV rail
            val accountName = AppState.currentUser?.let {
                it.displayName.ifBlank { it.username }
            } ?: "Đăng nhập"
            val accountSub = if (AppState.isLoggedIn) "Đã đồng bộ tài khoản" else "Nhấn để đăng nhập"

            TvFocusableCard(
                cornerRadius = 12.dp,
                selected = false,
                scaleOnFocus = 1.03f,
                onClick = { AppState.showAuthSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ManiFlixColors.surfaceElevated())
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = accountName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = accountSub,
                            fontSize = 10.5.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Right 10-foot Content Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (activeDetailSlug != null) {
                TvMovieDetailPane(
                    slug = activeDetailSlug,
                    seedMovie = activeDetailSeed,
                    onBack = onCloseDetail,
                    onSelectMovie = onMovieClick
                )
            } else {
                when (selectedTab) {
                    TvTab.DISCOVER -> TvDiscoverPane(onMovieClick = onMovieClick)
                    TvTab.SEARCH -> TvSearchPane(onMovieClick = onMovieClick)
                    TvTab.LIBRARY -> TvLibraryPane(onMovieClick = onMovieClick)
                    TvTab.NETWORK -> DeviceMonitorScreen(
                        bottomContentPadding = 16.dp,
                        onBack = { selectedTab = TvTab.DISCOVER }
                    )
                    TvTab.SETTINGS -> TvSettingsPane()
                }
            }
        }
    }
}

@Composable
private fun TvRailButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.03f else 1f, label = "tvRailScale")
    val fill = when {
        isFocused -> accent.copy(alpha = 0.26f)
        selected -> accent.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val borderCol = when {
        isFocused -> accent
        selected -> accent.copy(alpha = 0.48f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(if (isFocused) 2.dp else 1.dp, borderCol, RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused || selected) accent else textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected || isFocused) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isFocused || selected) textPrimary else textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvFocusableCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    selected: Boolean = false,
    scaleOnFocus: Float = 1.05f,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1f,
        label = "tvCardScale"
    )
    val accent = ManiFlixColors.accent()
    val borderColor = when {
        isFocused -> accent
        selected -> accent.copy(alpha = 0.72f)
        else -> ManiFlixColors.cardBorder()
    }
    val borderWidth = if (isFocused) 2.5.dp else if (selected) 1.5.dp else 0.7.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocused?.invoke()
                }
            }
            .clickable(onClick = onClick)
    ) {
        content()
    }
}

@Composable
private fun TvActionPill(
    label: String,
    icon: ImageVector? = null,
    primary: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1f, label = "tvPillScale")
    val accent = ManiFlixColors.accent()
    val surface = ManiFlixColors.surfaceElevated()
    val textPrimary = ManiFlixColors.textPrimary()

    val bg = when {
        primary -> accent
        selected -> accent.copy(alpha = 0.22f)
        else -> surface
    }
    val borderCol = when {
        isFocused -> if (primary) Color.White else accent
        selected -> accent
        else -> ManiFlixColors.cardBorder()
    }

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bg)
            .border(if (isFocused) 2.5.dp else 1.dp, borderCol, CircleShape)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (primary) Color.White else if (selected || isFocused) accent else textPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Color.White else textPrimary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun TvMovieCard(
    movie: MovieItem,
    width: Dp = 148.dp,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val primaryUrl = movie.displayPosterUrl.ifBlank { movie.displayThumbUrl }
    val secondaryUrl = movie.displayThumbUrl.ifBlank { movie.displayPosterUrl }
    var useFallback by remember(movie.slug) { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TvFocusableCard(
            cornerRadius = 12.dp,
            scaleOnFocus = 1.06f,
            onFocused = onFocused,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ManiFlixColors.surfaceElevated())
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (!useFallback) primaryUrl else secondaryUrl)
                        .size(320, 480)
                        .precision(Precision.INEXACT)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    onError = {
                        if (!useFallback && secondaryUrl.isNotBlank() && secondaryUrl != primaryUrl) {
                            useFallback = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                val badge = movie.quality.takeIf { it.isNotBlank() }
                    ?: movie.episodeCurrent.takeIf { it.isNotBlank() }
                if (!badge.isNullOrBlank()) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            text = movie.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val meta = buildList {
            if (movie.year > 0) add(movie.year.toString())
            if (movie.episodeCurrent.isNotBlank()) add(movie.episodeCurrent)
        }.joinToString(" • ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                fontSize = 11.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvDiscoverPane(
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()

    var selectedCategory by remember { mutableStateOf(TV_CATEGORIES.first()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCategoryLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var heroMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var seriesMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var singleMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var animeMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var filteredMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var spotlightMovie by remember { mutableStateOf<MovieItem?>(null) }

    suspend fun loadCatalog() {
        isLoading = true
        errorMessage = null
        try {
            coroutineScope {
                val homeTask = async { runCatching { ManiFlixApi.fetchMovies(1) }.getOrNull() }
                val seriesTask = async { runCatching { ManiFlixApi.fetchCategory("phim-bo", 1) }.getOrNull() }
                val singleTask = async { runCatching { ManiFlixApi.fetchCategory("phim-le", 1) }.getOrNull() }
                val animeTask = async { runCatching { ManiFlixApi.fetchCategory("hoat-hinh", 1) }.getOrNull() }

                val home = homeTask.await().orEmpty()
                if (home.isEmpty()) {
                    errorMessage = "Không thể kết nối máy chủ ManiFlix."
                } else {
                    heroMovies = home
                    if (spotlightMovie == null) spotlightMovie = home.firstOrNull()
                    hasMorePages = home.size >= 12
                }
                seriesMovies = seriesTask.await().orEmpty()
                singleMovies = singleTask.await().orEmpty()
                animeMovies = animeTask.await().orEmpty()
                currentPage = 1
            }
        } finally {
            isLoading = false
        }
    }

    suspend fun selectCategory(cat: TvCategoryOption) {
        selectedCategory = cat
        currentPage = 1
        if (cat.apiSlug == null) {
            filteredMovies = emptyList()
            hasMorePages = heroMovies.size >= 12
            spotlightMovie = heroMovies.firstOrNull { !AppState.isBlocked(it.slug) }
            return
        }
        isCategoryLoading = true
        try {
            val items = ManiFlixApi.fetchCategory(cat.apiSlug, 1)
            filteredMovies = items
            hasMorePages = items.size >= 12
            spotlightMovie = items.firstOrNull { !AppState.isBlocked(it.slug) } ?: spotlightMovie
        } catch (_: Throwable) {
            filteredMovies = emptyList()
            hasMorePages = false
        } finally {
            isCategoryLoading = false
        }
    }

    suspend fun loadMoreMovies() {
        if (isLoadingMore || !hasMorePages) return
        isLoadingMore = true
        val nextPage = currentPage + 1
        try {
            val slug = selectedCategory.apiSlug
            val nextItems = if (slug == null) {
                ManiFlixApi.fetchMovies(nextPage)
            } else {
                ManiFlixApi.fetchCategory(slug, nextPage)
            }
            if (nextItems.isEmpty()) {
                hasMorePages = false
            } else {
                if (slug == null) {
                    val existing = heroMovies.mapTo(HashSet()) { it.slug }
                    val unique = nextItems.filter { it.slug !in existing }
                    heroMovies = heroMovies + unique
                    hasMorePages = unique.isNotEmpty()
                } else {
                    val existing = filteredMovies.mapTo(HashSet()) { it.slug }
                    val unique = nextItems.filter { it.slug !in existing }
                    filteredMovies = filteredMovies + unique
                    hasMorePages = unique.isNotEmpty()
                }
                currentPage = nextPage
            }
        } catch (_: Throwable) {
            hasMorePages = false
        } finally {
            isLoadingMore = false
        }
    }

    LaunchedEffect(Unit) {
        if (heroMovies.isEmpty()) loadCatalog()
    }

    val visibleHome = remember(heroMovies, AppState.blockedMovies) {
        heroMovies.filter { !AppState.isBlocked(it.slug) }
    }
    val visibleFiltered = remember(filteredMovies, AppState.blockedMovies) {
        filteredMovies.filter { !AppState.isBlocked(it.slug) }
    }
    val visibleSeries = remember(seriesMovies, heroMovies, AppState.blockedMovies) {
        (seriesMovies.ifEmpty { heroMovies.take(12) }).filter { !AppState.isBlocked(it.slug) }
    }
    val visibleSingle = remember(singleMovies, heroMovies, AppState.blockedMovies) {
        (singleMovies.ifEmpty { heroMovies.drop(4).take(12) }).filter { !AppState.isBlocked(it.slug) }
    }
    val visibleAnime = remember(animeMovies, AppState.blockedMovies) {
        animeMovies.filter { !AppState.isBlocked(it.slug) }
    }
    val hero = spotlightMovie ?: visibleHome.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "tv_categories", contentType = "categories") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(TV_CATEGORIES, key = { it.label }) { cat ->
                        TvActionPill(
                            label = cat.label,
                            selected = selectedCategory == cat,
                            onClick = { scope.launch { selectCategory(cat) } }
                        )
                    }
                }

                TvActionPill(
                    label = "Làm mới",
                    icon = Icons.Rounded.Refresh,
                    onClick = { scope.launch { loadCatalog() } }
                )
            }
        }

        if (isLoading && heroMovies.isEmpty()) {
            item(key = "tv_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accent)
                }
            }
        } else if (errorMessage != null && heroMovies.isEmpty()) {
            item(key = "tv_error") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "Lỗi kết nối",
                        fontSize = 15.sp,
                        color = textSecondary
                    )
                    TvActionPill(
                        label = "Thử lại",
                        icon = Icons.Rounded.Refresh,
                        primary = true,
                        onClick = { scope.launch { loadCatalog() } }
                    )
                }
            }
        } else {
            if (hero != null) {
                item(key = "tv_spotlight", contentType = "hero") {
                    TvSpotlightBanner(
                        movie = hero,
                        onPlay = { scope.launch { AppState.quickWatch(context, hero.slug) } },
                        onOpenDetail = { onMovieClick(hero.slug, hero) },
                        onToggleFavorite = { scope.launch { AppState.toggleFavoriteItem(hero) } }
                    )
                }
            }

            if (AppState.history.isNotEmpty() && selectedCategory.apiSlug == null) {
                item(key = "tv_continue_rail", contentType = "continue_rail") {
                    TvContinueWatchingRail(
                        entries = AppState.history.take(12),
                        onFocused = { spotlightMovie = it.toMovieItem() },
                        onMovieClick = { entry -> onMovieClick(entry.slug, entry.toMovieItem()) }
                    )
                }
            }

            if (selectedCategory.apiSlug != null) {
                if (isCategoryLoading) {
                    item(key = "tv_cat_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                } else {
                    item(key = "tv_cat_rail", contentType = "rail") {
                        TvMovieHorizontalRail(
                            title = selectedCategory.label,
                            movies = visibleFiltered,
                            onFocused = { spotlightMovie = it },
                            onMovieClick = onMovieClick
                        )
                    }
                }
            } else {
                if (visibleSeries.isNotEmpty()) {
                    item(key = "tv_series_rail", contentType = "rail") {
                        TvMovieHorizontalRail(
                            title = "Phim bộ mới",
                            movies = visibleSeries,
                            onFocused = { spotlightMovie = it },
                            onMovieClick = onMovieClick
                        )
                    }
                }
                if (visibleSingle.isNotEmpty()) {
                    item(key = "tv_single_rail", contentType = "rail") {
                        TvMovieHorizontalRail(
                            title = "Phim lẻ tuyển chọn",
                            movies = visibleSingle,
                            onFocused = { spotlightMovie = it },
                            onMovieClick = onMovieClick
                        )
                    }
                }
                if (visibleAnime.isNotEmpty()) {
                    item(key = "tv_anime_rail", contentType = "rail") {
                        TvMovieHorizontalRail(
                            title = "Hoạt hình & Anime",
                            movies = visibleAnime,
                            onFocused = { spotlightMovie = it },
                            onMovieClick = onMovieClick
                        )
                    }
                }
                if (visibleHome.isNotEmpty()) {
                    item(key = "tv_latest_rail", contentType = "rail") {
                        TvMovieHorizontalRail(
                            title = "Mới cập nhật",
                            movies = visibleHome,
                            onFocused = { spotlightMovie = it },
                            onMovieClick = onMovieClick
                        )
                    }
                }
            }

            if (hasMorePages) {
                item(key = "tv_load_more") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(color = accent, modifier = Modifier.size(28.dp))
                        } else {
                            TvActionPill(
                                label = "Tải thêm phim",
                                onClick = { scope.launch { loadMoreMovies() } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSpotlightBanner(
    movie: MovieItem,
    onPlay: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val isFav = AppState.isFavorite(movie.slug)
    val surface = ManiFlixColors.surface()
    val backdropUrl = movie.displayThumbUrl.ifBlank { movie.displayPosterUrl }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(242.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(surface)
            .border(0.8.dp, ManiFlixColors.cardBorder(), RoundedCornerShape(18.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(backdropUrl)
                .size(960, 540)
                .precision(Precision.INEXACT)
                .crossfade(true)
                .build(),
            contentDescription = movie.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.88f),
                        0.55f to Color.Black.copy(alpha = 0.62f),
                        1.0f to Color.Black.copy(alpha = 0.25f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.68f)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val meta = buildList {
                if (movie.year > 0) add(movie.year.toString())
                if (movie.quality.isNotBlank()) add(movie.quality)
                if (movie.episodeCurrent.isNotBlank()) add(movie.episodeCurrent)
            }.joinToString(" • ")

            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1
                )
            }

            Text(
                text = movie.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (movie.originName.isNotBlank()) {
                Text(
                    text = movie.originName,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvActionPill(
                    label = if (AppState.hasHistory(movie.slug)) "Xem tiếp" else "Xem ngay",
                    icon = Icons.Rounded.PlayArrow,
                    primary = true,
                    onClick = onPlay
                )
                TvActionPill(
                    label = "Chi tiết",
                    icon = Icons.Rounded.Info,
                    onClick = onOpenDetail
                )
                TvActionPill(
                    label = if (isFav) "Đã lưu" else "Lưu lại",
                    icon = if (isFav) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                    onClick = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun TvContinueWatchingRail(
    entries: List<HistoryEntry>,
    onFocused: (HistoryEntry) -> Unit,
    onMovieClick: (HistoryEntry) -> Unit
) {
    val context = LocalContext.current
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Xem tiếp",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(entries, key = { it.slug }, contentType = { "continue_entry" }) { entry ->
                Column(
                    modifier = Modifier.width(214.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TvFocusableCard(
                        cornerRadius = 12.dp,
                        scaleOnFocus = 1.05f,
                        onFocused = { onFocused(entry) },
                        onClick = { onMovieClick(entry) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(122.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ManiFlixColors.surfaceElevated())
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(entry.displayPosterUrl)
                                    .size(320, 190)
                                    .precision(Precision.INEXACT)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = entry.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            LinearProgressIndicator(
                                progress = { entry.progressRatio.coerceIn(0f, 1f) },
                                color = accent,
                                trackColor = Color.White.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                        }
                    }

                    Text(
                        text = entry.name.ifBlank { entry.slug },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.episodeName.ifBlank { "Tập" }} • ${PlayerEngine.formatTime(entry.currentTime)}",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TvMovieHorizontalRail(
    title: String,
    movies: List<MovieItem>,
    onFocused: (MovieItem) -> Unit,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ManiFlixColors.textPrimary()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(movies, key = { it.slug }, contentType = { "movie" }) { movie ->
                TvMovieCard(
                    movie = movie,
                    onFocused = { onFocused(movie) },
                    onClick = { onMovieClick(movie.slug, movie) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvSearchPane(
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()

    var query by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<MovieItem>>(emptyList()) }

    suspend fun runSearch(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) {
            results = emptyList()
            hasSearched = false
            return
        }
        isSearching = true
        hasSearched = true
        try {
            results = ManiFlixApi.searchMovies(q, 1)
        } catch (_: Throwable) {
            results = emptyList()
        } finally {
            isSearching = false
        }
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            results = emptyList()
            hasSearched = false
        } else {
            delay(360L)
            runSearch(trimmed)
        }
    }

    val visibleResults = remember(results, AppState.blockedMovies) {
        results.filter { !AppState.isBlocked(it.slug) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Tìm kiếm tên phim, diễn viên...", color = textSecondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = textSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = borderColor,
                    cursorColor = accent
                ),
                modifier = Modifier.weight(1f)
            )

            TvActionPill(
                label = "Xoá 1",
                icon = Icons.AutoMirrored.Rounded.Backspace,
                onClick = { if (query.isNotEmpty()) query = query.dropLast(1) }
            )
            TvActionPill(
                label = "Xoá hết",
                icon = Icons.Rounded.Clear,
                onClick = { query = "" }
            )
            TvActionPill(
                label = "Tìm",
                icon = Icons.Rounded.Search,
                primary = true,
                onClick = { scope.launch { runSearch(query) } }
            )
        }

        // On-screen D-Pad quick keyboard bar for TV boxes without soft keyboard
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TV_DPAD_KEYS.forEach { keyChar ->
                TvKeyboardKey(label = keyChar, accent = accent, textPrimary = textPrimary) {
                    query += keyChar.lowercase()
                }
            }
            TvKeyboardKey(label = "CÁCH", accent = accent, textPrimary = textPrimary) {
                if (query.isNotEmpty() && !query.endsWith(" ")) query += " "
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TV_QUICK_TOPICS, key = { it }) { topic ->
                TvActionPill(
                    label = topic,
                    selected = query.equals(topic, ignoreCase = true),
                    onClick = { query = topic }
                )
            }
        }

        when {
            isSearching -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            visibleResults.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (hasSearched) "Không tìm thấy phim nào cho \"$query\"" else "Chọn chữ cái hoặc nhập tên phim để tìm kiếm",
                        fontSize = 15.sp,
                        color = textSecondary
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItems(visibleResults, key = { it.slug }, contentType = { "movie" }) { movie ->
                        TvMovieCard(
                            movie = movie,
                            width = 140.dp,
                            onClick = { onMovieClick(movie.slug, movie) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvKeyboardKey(
    label: String,
    accent: Color,
    textPrimary: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1f, label = "tvKeyScale")
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) accent else ManiFlixColors.surfaceElevated())
            .border(
                width = if (isFocused) 2.dp else 0.7.dp,
                color = if (isFocused) Color.White else ManiFlixColors.cardBorder(),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocused) Color.White else textPrimary,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvMovieDetailPane(
    slug: String,
    seedMovie: MovieItem?,
    onBack: () -> Unit,
    onSelectMovie: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()

    var detail by remember(slug) { mutableStateOf<MovieDetail?>(null) }
    var servers by remember(slug) { mutableStateOf<List<EpisodeServer>>(emptyList()) }
    var franchise by remember(slug) { mutableStateOf<List<FranchiseSeason>>(emptyList()) }
    var selectedServerIdx by remember(slug) { mutableIntStateOf(0) }
    var isLoading by remember(slug) { mutableStateOf(true) }

    LaunchedEffect(slug) {
        isLoading = true
        try {
            val res = ManiFlixApi.fetchMovieDetail(slug)
            detail = res.movie
            servers = res.episodes
            val titleHint = res.movie?.name.orEmpty().ifBlank { seedMovie?.name.orEmpty() }
            if (titleHint.isNotBlank()) {
                franchise = runCatching { ManiFlixApi.fetchFranchise(slug, titleHint) }.getOrDefault(emptyList())
            }
        } catch (_: Throwable) {
        } finally {
            isLoading = false
        }
    }

    val historyMatch = AppState.history.firstOrNull { it.slug == slug }
    val activeEpisodes = servers.getOrNull(selectedServerIdx)?.serverData.orEmpty()
    val posterUrl = detail?.displayPosterUrl?.ifBlank { detail?.displayThumbUrl.orEmpty() }
        ?: seedMovie?.displayPosterUrl?.ifBlank { seedMovie.displayThumbUrl }.orEmpty()
    val title = detail?.name ?: seedMovie?.name ?: slug
    val isFav = AppState.isFavorite(slug)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvActionPill(
                label = "Quay lại",
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onBack
            )
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(posterUrl)
                    .size(320, 480)
                    .precision(Precision.INEXACT)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(180.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ManiFlixColors.surfaceElevated())
                    .border(0.8.dp, ManiFlixColors.cardBorder(), RoundedCornerShape(14.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                val origin = detail?.originName ?: seedMovie?.originName.orEmpty()
                if (origin.isNotBlank()) {
                    Text(text = origin, fontSize = 14.sp, color = textSecondary)
                }

                val meta = buildList {
                    val yr = detail?.year ?: seedMovie?.year ?: 0
                    if (yr > 0) add(yr.toString())
                    detail?.quality?.takeIf { it.isNotBlank() }?.let { add(it) }
                    detail?.time?.takeIf { it.isNotBlank() }?.let { add(it) }
                    detail?.episodeCurrent?.takeIf { it.isNotBlank() }?.let { add(it) }
                    detail?.country?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" • ")

                if (meta.isNotBlank()) {
                    Text(text = meta, fontSize = 13.sp, color = accent, fontWeight = FontWeight.SemiBold)
                }

                val genres = detail?.category?.joinToString(", ") { it.name }.orEmpty()
                if (genres.isNotBlank()) {
                    Text(text = "Thể loại: $genres", fontSize = 13.sp, color = textSecondary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvActionPill(
                        label = if (historyMatch != null) {
                            "Xem tiếp • ${historyMatch.episodeName.ifBlank { "Tập ${historyMatch.episodeIndex + 1}" }}"
                        } else {
                            "Phát ngay"
                        },
                        icon = Icons.Rounded.PlayArrow,
                        primary = true,
                        onClick = {
                            val d = detail
                            if (d != null && servers.isNotEmpty()) {
                                val epIdx = (historyMatch?.episodeIndex ?: 0).coerceIn(
                                    0,
                                    (servers.getOrNull(selectedServerIdx)?.serverData?.size ?: 1) - 1
                                )
                                AppState.startPlayback(context, d, servers, selectedServerIdx, epIdx)
                            } else {
                                scope.launch { AppState.quickWatch(context, slug) }
                            }
                        }
                    )

                    TvActionPill(
                        label = if (isFav) "Đã lưu tủ phim" else "Thêm tủ phim",
                        icon = if (isFav) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                        onClick = {
                            scope.launch {
                                val d = detail
                                if (d != null) {
                                    AppState.toggleFavorite(d)
                                } else if (seedMovie != null) {
                                    AppState.toggleFavoriteItem(seedMovie)
                                }
                            }
                        }
                    )
                }

                val synopsis = detail?.plainSynopsis.orEmpty()
                if (synopsis.isNotBlank()) {
                    Text(
                        text = synopsis,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = textSecondary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accent)
            }
        }

        if (franchise.size > 1) {
            Text(
                text = "Các phần phim (${franchise.size})",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                franchise.forEach { season ->
                    TvActionPill(
                        label = season.displayFullTitle,
                        selected = season.slug == slug,
                        onClick = {
                            if (season.slug != slug) {
                                onSelectMovie(season.slug, null)
                            }
                        }
                    )
                }
            }
        }

        if (servers.isNotEmpty()) {
            Text(
                text = "Nguồn phát & Danh sách tập",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(servers) { idx, srv ->
                    TvActionPill(
                        label = srv.serverName.ifBlank { "Server ${idx + 1}" },
                        selected = idx == selectedServerIdx,
                        onClick = { selectedServerIdx = idx }
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activeEpisodes.forEachIndexed { idx, ep ->
                    val isWatching = historyMatch?.episodeIndex == idx
                    TvActionPill(
                        label = ep.name.ifBlank { "Tập ${idx + 1}" },
                        selected = isWatching,
                        onClick = {
                            val d = detail
                            if (d != null) {
                                AppState.startPlayback(context, d, servers, selectedServerIdx, idx)
                            } else {
                                scope.launch { AppState.quickWatch(context, slug) }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvLibraryPane(
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    var tabIdx by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Đang xem", "Đã lưu", "Đã ẩn")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            tabs.forEachIndexed { idx, label ->
                TvActionPill(
                    label = label,
                    selected = tabIdx == idx,
                    onClick = { tabIdx = idx }
                )
            }
        }

        when (tabIdx) {
            0 -> {
                if (AppState.history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có lịch sử xem.", color = textSecondary, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(AppState.history, key = { it.slug }, contentType = { "tv_history" }) { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TvFocusableCard(
                                    cornerRadius = 12.dp,
                                    scaleOnFocus = 1.02f,
                                    onClick = { onMovieClick(entry.slug, entry.toMovieItem()) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ManiFlixColors.surface())
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(entry.displayPosterUrl)
                                                .size(240, 145)
                                                .precision(Precision.INEXACT)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = entry.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 112.dp, height = 66.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.name.ifBlank { entry.slug },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${entry.episodeName.ifBlank { "Tập ${entry.episodeIndex + 1}" }} • ${PlayerEngine.formatTime(entry.currentTime)}",
                                                fontSize = 12.5.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }
                                }

                                TvActionPill(
                                    label = "Xoá",
                                    icon = Icons.Rounded.DeleteOutline,
                                    onClick = { scope.launch { AppState.deleteHistory(entry.slug) } }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                if (!AppState.isLoggedIn) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Đăng nhập để đồng bộ Tủ phim đã lưu trên TV & điện thoại.",
                            fontSize = 15.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TvActionPill(
                            label = "Đăng nhập ngay",
                            primary = true,
                            onClick = { AppState.showAuthSheet = true }
                        )
                    }
                } else if (AppState.favorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tủ phim đang trống.", color = textSecondary, fontSize = 15.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        gridItems(AppState.favorites, key = { it.slug }, contentType = { "movie" }) { fav ->
                            val item = remember(fav) { fav.toMovieItem() }
                            TvMovieCard(
                                movie = item,
                                width = 140.dp,
                                onClick = { onMovieClick(item.slug, item) }
                            )
                        }
                    }
                }
            }

            else -> {
                if (AppState.blockedMovies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có phim nào bị ẩn.", color = textSecondary, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AppState.blockedMovies, key = { it.slug }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ManiFlixColors.surface())
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = item.name.ifBlank { item.slug },
                                    fontSize = 15.sp,
                                    color = textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                TvActionPill(
                                    label = "Hiện lại",
                                    icon = Icons.Rounded.Visibility,
                                    onClick = { AppState.unblockMovie(item.slug) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvSettingsPane() {
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Cài đặt Android TV",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        // Theme presets & accent swatches
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ManiFlixColors.surface())
                .border(0.8.dp, ManiFlixColors.cardBorder(), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Giao diện & Màu sắc",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                THEME_PRESETS.forEach { preset ->
                    val selected = AppPreferences.activePresetId == preset.id
                    TvActionPill(
                        label = preset.name,
                        selected = selected,
                        onClick = {
                            AppPreferences.applyPreset(preset)
                            scope.launch { AppState.saveAndSyncTheme(preset.id) }
                        }
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccentPreset.entries.forEach { preset ->
                    TvActionPill(
                        label = preset.label,
                        selected = AppPreferences.accentPreset == preset,
                        onClick = {
                            AppPreferences.accentPreset = preset
                            scope.launch { AppState.saveAndSyncTheme("custom") }
                        }
                    )
                }
            }
        }

        // Audio & Playback
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ManiFlixColors.surface())
                .border(0.8.dp, ManiFlixColors.cardBorder(), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Trình phát & Âm thanh",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SoundProfileMode.entries.forEach { profile ->
                    TvActionPill(
                        label = "Âm thanh: ${profile.title}",
                        selected = AppPreferences.soundProfile == profile,
                        onClick = { AppPreferences.soundProfile = profile }
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IntroSkipBehavior.entries.forEach { intro ->
                    TvActionPill(
                        label = "Intro: ${intro.title}",
                        selected = AppPreferences.introSkipBehavior == intro,
                        onClick = { AppPreferences.introSkipBehavior = intro }
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvActionPill(
                    label = if (AppPreferences.autoCutAds) "Cắt quảng cáo: Bật" else "Cắt quảng cáo: Tắt",
                    selected = AppPreferences.autoCutAds,
                    onClick = { AppPreferences.autoCutAds = !AppPreferences.autoCutAds }
                )
                TvActionPill(
                    label = if (AppPreferences.autoNextEpisode) "Tự chuyển tập: Bật" else "Tự chuyển tập: Tắt",
                    selected = AppPreferences.autoNextEpisode,
                    onClick = { AppPreferences.autoNextEpisode = !AppPreferences.autoNextEpisode }
                )
                listOf(5, 10, 15, 30).forEach { sec ->
                    TvActionPill(
                        label = "Tua ${sec}s",
                        selected = AppPreferences.doubleTapSeekSeconds == sec,
                        onClick = { AppPreferences.doubleTapSeekSeconds = sec }
                    )
                }
            }
        }

        // App & Updates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ManiFlixColors.surface())
                .border(0.8.dp, ManiFlixColors.cardBorder(), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Phiên bản ${UpdateManager.displayVersion}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                if (UpdateManager.lastCheckNote.isNotBlank()) {
                    Text(
                        text = UpdateManager.lastCheckNote,
                        fontSize = 12.sp,
                        color = accent
                    )
                }
            }
            TvActionPill(
                label = if (UpdateManager.isChecking) "Đang kiểm tra..." else "Kiểm tra cập nhật",
                primary = true,
                onClick = { scope.launch { UpdateManager.checkForUpdates(manual = true) } }
            )
            TvActionPill(
                label = "Đặt lại mặc định",
                onClick = { AppPreferences.resetToDefaults() }
            )
        }
    }
}
