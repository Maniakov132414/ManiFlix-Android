package bond.maniakov.maniflix.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.ManiFlixApi
import bond.maniakov.maniflix.model.HistoryEntry
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class DiscoverCategoryOption(
    val label: String,
    val apiSlug: String?
)

private val DISCOVER_CATEGORIES = listOf(
    DiscoverCategoryOption("Tất cả", null),
    DiscoverCategoryOption("Phim bộ", "phim-bo"),
    DiscoverCategoryOption("Phim lẻ", "phim-le"),
    DiscoverCategoryOption("Hoạt hình", "hoat-hinh"),
    DiscoverCategoryOption("Hành động", "hanh-dong"),
    DiscoverCategoryOption("Cổ trang", "co-trang"),
    DiscoverCategoryOption("Tình cảm", "tinh-cam"),
    DiscoverCategoryOption("Kinh dị", "kinh-di"),
    DiscoverCategoryOption("Viễn tưởng", "vien-tuong")
)

internal fun adaptiveGridColumns(screenWidthDp: Int): Int = when {
    screenWidthDp >= 1100 -> 6
    screenWidthDp >= 840 -> 5
    screenWidthDp >= 600 -> 4
    else -> AppPreferences.gridColumnsPhone.coerceIn(2, 3)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    bottomContentPadding: Dp = 16.dp,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()

    var heroMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var seriesMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var singleMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var animeMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var filteredMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }

    var selectedCategory by remember { mutableStateOf(DISCOVER_CATEGORIES.first()) }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMorePages by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var isCategoryLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadCatalog(refreshing: Boolean = false) {
        if (refreshing) isRefreshing = true else isLoading = true
        errorMessage = null
        try {
            coroutineScope {
                val latestJob = async { runCatching { ManiFlixApi.fetchMovies(1) } }
                val seriesJob = async { runCatching { ManiFlixApi.fetchCategory("phim-bo", 1) } }
                val singleJob = async { runCatching { ManiFlixApi.fetchCategory("phim-le", 1) } }
                val animeJob = async { runCatching { ManiFlixApi.fetchCategory("hoat-hinh", 1) } }

                val latestRes = latestJob.await()
                val seriesRes = seriesJob.await()
                val singleRes = singleJob.await()
                val animeRes = animeJob.await()

                val latestList = latestRes.getOrNull().orEmpty()
                if (latestList.isNotEmpty()) {
                    heroMovies = latestList
                    if (selectedCategory.apiSlug == null) {
                        currentPage = 1
                        hasMorePages = latestList.size >= 12
                    }
                } else if (latestRes.isFailure && heroMovies.isEmpty()) {
                    errorMessage = latestRes.exceptionOrNull()?.localizedMessage
                        ?: "Không thể tải danh sách phim."
                }
                seriesRes.getOrNull()?.let { if (it.isNotEmpty()) seriesMovies = it }
                singleRes.getOrNull()?.let { if (it.isNotEmpty()) singleMovies = it }
                animeRes.getOrNull()?.let { if (it.isNotEmpty()) animeMovies = it }

                selectedCategory.apiSlug?.let { catSlug ->
                    val catList = runCatching { ManiFlixApi.fetchCategory(catSlug, 1) }.getOrNull().orEmpty()
                    filteredMovies = catList
                    currentPage = 1
                    hasMorePages = catList.size >= 12
                }
            }
        } catch (t: Throwable) {
            if (heroMovies.isEmpty()) {
                errorMessage = t.localizedMessage ?: "Không thể kết nối máy chủ."
            }
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    suspend fun selectCategory(cat: DiscoverCategoryOption) {
        selectedCategory = cat
        currentPage = 1
        if (cat.apiSlug == null) {
            filteredMovies = emptyList()
            hasMorePages = heroMovies.size >= 12
            return
        }
        isCategoryLoading = true
        try {
            val items = ManiFlixApi.fetchCategory(cat.apiSlug, 1)
            filteredMovies = items
            hasMorePages = items.size >= 12
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
        if (heroMovies.isEmpty()) {
            loadCatalog()
        }
    }

    val visibleHero = heroMovies.filter { !AppState.isBlocked(it.slug) }
    val featuredHero = visibleHero.take(5)
    val visibleSeries = (seriesMovies.ifEmpty { heroMovies.take(12) })
        .filter { !AppState.isBlocked(it.slug) }
    val visibleSingle = (singleMovies.ifEmpty { heroMovies.drop(4).take(12) })
        .filter { !AppState.isBlocked(it.slug) }
    val visibleAnime = animeMovies.filter { !AppState.isBlocked(it.slug) }

    val activeGridItems = remember(selectedCategory, heroMovies, filteredMovies, AppState.blockedMovies) {
        val base = if (selectedCategory.apiSlug == null) heroMovies else filteredMovies
        base.filter { !AppState.isBlocked(it.slug) }
    }

    val pagerState = rememberPagerState(pageCount = { featuredHero.size.coerceAtLeast(1) })
    val activeHeroUrl = featuredHero.getOrNull(pagerState.currentPage)?.let {
        it.displayPosterUrl.ifBlank { it.displayThumbUrl }
    } ?: featuredHero.firstOrNull()?.displayPosterUrl

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidthDp >= 600
    val heroHeight = if (isWideScreen) 344.dp else 296.dp
    val railCardWidth = if (isWideScreen) 138.dp else 120.dp
    val gridColumns = adaptiveGridColumns(screenWidthDp)
    val gridRows = remember(activeGridItems, gridColumns) {
        activeGridItems.chunked(gridColumns)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AmbientArtworkBackground(
            imageUrl = activeHeroUrl,
            height = 380.dp
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { scope.launch { loadCatalog(refreshing = true) } },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "top_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 16.dp, end = 10.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Trang chủ",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                triggerHaptic(haptic)
                                AppState.showAuthSheet = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = "Tài khoản",
                                tint = if (AppState.isLoggedIn) accent else textPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                item(key = "category_chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DISCOVER_CATEGORIES, key = { it.label }) { cat ->
                            val selected = selectedCategory.label == cat.label
                            Box(
                                modifier = appleGlassSurfaceModifier(
                                    cornerRadius = 50.dp,
                                    highlighted = selected
                                )
                                    .clickable {
                                        triggerHaptic(haptic)
                                        scope.launch { selectCategory(cat) }
                                    }
                                    .padding(horizontal = 15.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected && ManiFlixColors.isLight()) accent else textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null && heroMovies.isEmpty()) {
                    item(key = "error_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = errorMessage ?: "Lỗi kết nối",
                                fontSize = 14.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { scope.launch { loadCatalog() } },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Thử lại", color = Color.White)
                            }
                        }
                    }
                } else {
                    if (featuredHero.isNotEmpty() && selectedCategory.apiSlug == null) {
                        item(key = "hero_pager") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    pageSpacing = 12.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) { page ->
                                    val movie = featuredHero[page]
                                    HeroFeaturedCard(
                                        movie = movie,
                                        accent = accent,
                                        height = heroHeight,
                                        onClick = { onMovieClick(movie.slug, movie) }
                                    )
                                }

                                if (featuredHero.size > 1) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(featuredHero.size) { index ->
                                            val active = pagerState.currentPage == index
                                            Box(
                                                modifier = Modifier
                                                    .width(if (active) 18.dp else 6.dp)
                                                    .height(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (active) accent else textSecondary.copy(alpha = 0.35f)
                                                    )
                                                    .clickable {
                                                        scope.launch { pagerState.animateScrollToPage(index) }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isLoading) {
                        item(key = "hero_skeleton") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(heroHeight)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ManiFlixColors.surfaceElevated())
                            )
                        }
                    }

                    if (AppState.history.isNotEmpty() && selectedCategory.apiSlug == null) {
                        item(key = "continue_watching") {
                            ContinueWatchingRail(
                                entries = AppState.history.take(10),
                                accent = accent,
                                onMovieClick = onMovieClick
                            )
                        }
                    }

                    if (selectedCategory.apiSlug == null) {
                        if (visibleSeries.isNotEmpty()) {
                            item(key = "rail_series") {
                                HorizontalMovieRail(
                                    title = "Phim bộ mới",
                                    items = visibleSeries,
                                    cardWidth = railCardWidth,
                                    onMovieClick = onMovieClick
                                )
                            }
                        }
                        if (visibleSingle.isNotEmpty()) {
                            item(key = "rail_single") {
                                HorizontalMovieRail(
                                    title = "Phim lẻ tuyển chọn",
                                    items = visibleSingle,
                                    cardWidth = railCardWidth,
                                    onMovieClick = onMovieClick
                                )
                            }
                        }
                        if (visibleAnime.isNotEmpty()) {
                            item(key = "rail_anime") {
                                HorizontalMovieRail(
                                    title = "Hoạt hình & Anime",
                                    items = visibleAnime,
                                    cardWidth = railCardWidth,
                                    onMovieClick = onMovieClick
                                )
                            }
                        }
                    }

                    item(key = "latest_header") {
                        Text(
                            text = if (selectedCategory.apiSlug == null) {
                                "Mới cập nhật"
                            } else {
                                selectedCategory.label
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (isCategoryLoading) {
                        item(key = "category_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = accent, strokeWidth = 2.5.dp)
                            }
                        }
                    } else if (gridRows.isEmpty()) {
                        item(key = "empty_grid") {
                            Text(
                                text = "Chưa có phim trong mục này.",
                                fontSize = 14.sp,
                                color = textSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            )
                        }
                    } else {
                        items(
                            items = gridRows,
                            key = { row -> "${selectedCategory.label}-${row.joinToString("-") { it.slug }}" }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                rowItems.forEach { movie ->
                                    MoviePosterCard(
                                        movie = movie,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onMovieClick(movie.slug, movie) }
                                    )
                                }
                                repeat(gridColumns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (hasMorePages) {
                            item(key = "load_more_footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { scope.launch { loadMoreMovies() } },
                                        enabled = !isLoadingMore,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(
                                                color = accent,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Xem thêm phim",
                                                color = accent,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroFeaturedCard(
    movie: MovieItem,
    accent: Color,
    height: Dp = 296.dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val isFav = AppState.isFavorite(movie.slug)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(0.7.dp, Color.White.copy(alpha = 0.16f), shape)
            .combinedClickable(
                onClick = {
                    triggerHaptic(haptic)
                    onClick()
                },
                onLongClick = {
                    triggerHaptic(haptic, strong = true)
                    menuExpanded = true
                }
            )
    ) {
        val heroPrimary = movie.displayThumbUrl.ifBlank { movie.displayPosterUrl }
        val heroFallback = movie.displayPosterUrl.ifBlank { movie.displayThumbUrl }
        var useHeroFallback by remember(movie.slug) { mutableStateOf(false) }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(if (!useHeroFallback) heroPrimary else heroFallback)
                .crossfade(true)
                .build(),
            contentDescription = movie.name,
            contentScale = ContentScale.Crop,
            onError = {
                if (!useHeroFallback && heroFallback.isNotBlank() && heroFallback != heroPrimary) {
                    useHeroFallback = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.36f to Color.Black.copy(alpha = 0.28f),
                        1.0f to Color.Black.copy(alpha = 0.93f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val eyebrow = buildString {
                append("PHIM MỚI")
                if (movie.year > 0) {
                    append(" • ${movie.year}")
                }
            }
            Text(
                text = eyebrow,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = accent
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                movie.quality.takeIf { it.isNotBlank() }?.let { q ->
                    Text(
                        text = q,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.copy(alpha = 0.90f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                movie.lang.takeIf { it.isNotBlank() }?.let { lang ->
                    Text(
                        text = lang,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE4E4EA)
                    )
                }
                movie.category.take(2).forEach { tag ->
                    if (tag.name.isNotBlank()) {
                        Text(
                            text = "• ${tag.name}",
                            fontSize = 10.sp,
                            color = Color(0xFFC2C2CA),
                            maxLines = 1
                        )
                    }
                }
            }

            Text(
                text = movie.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            movie.originName.takeIf { it.isNotBlank() }?.let { origin ->
                Text(
                    text = origin,
                    fontSize = 12.sp,
                    color = Color(0xFFB8B8C0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent)
                        .clickable {
                            triggerHaptic(haptic)
                            scope.launch { AppState.quickWatch(context, movie.slug) }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Xem ngay",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .clickable {
                            triggerHaptic(haptic)
                            scope.launch { AppState.toggleFavoriteItem(movie) }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkAdd,
                        contentDescription = null,
                        tint = if (isFav) accent else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isFav) "Đã lưu" else "Tủ phim",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            MovieContextDropdownMenu(
                movie = movie,
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingRail(
    entries: List<HistoryEntry>,
    accent: Color,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Xem tiếp",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.slug }) { entry ->
                val cardMovie = remember(entry) { entry.toMovieItem() }
                var menuExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .width(158.dp)
                        .combinedClickable(
                            onClick = {
                                triggerHaptic(haptic)
                                onMovieClick(entry.slug, cardMovie)
                            },
                            onLongClick = {
                                triggerHaptic(haptic, strong = true)
                                menuExpanded = true
                            }
                        ),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(158.dp)
                            .height(94.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ManiFlixColors.surfaceElevated())
                            .border(0.6.dp, borderColor, RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(entry.displayPosterUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = cardMovie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        LinearProgressIndicator(
                            progress = { entry.progressRatio.coerceIn(0f, 1f) },
                            color = accent,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cardMovie.name,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(width = 22.dp, height = 20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .clickable {
                                        triggerHaptic(haptic)
                                        menuExpanded = true
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = "Tuỳ chọn phim",
                                    tint = textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            MovieContextDropdownMenu(
                                movie = cardMovie,
                                expanded = menuExpanded,
                                onDismiss = { menuExpanded = false }
                            )
                        }
                    }

                    Text(
                        text = entry.episodeName.takeIf { it.isNotBlank() } ?: "Xem tiếp",
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
private fun HorizontalMovieRail(
    title: String,
    items: List<MovieItem>,
    cardWidth: Dp = 120.dp,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val textPrimary = ManiFlixColors.textPrimary()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            items(items, key = { it.slug }) { movie ->
                MoviePosterCard(
                    movie = movie,
                    modifier = Modifier.width(cardWidth),
                    onClick = { onMovieClick(movie.slug, movie) }
                )
            }
        }
    }
}
