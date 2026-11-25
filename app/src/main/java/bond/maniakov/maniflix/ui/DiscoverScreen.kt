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
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    bottomContentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val oled = AppPreferences.oledBlackEnabled
    val accent = ManiFlixColors.accent()

    var heroMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var seriesMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var singleMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var animeMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = remember { listOf("Tất cả", "Phim bộ", "Phim lẻ", "Hoạt hình") }

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
                } else if (latestRes.isFailure && heroMovies.isEmpty()) {
                    errorMessage = latestRes.exceptionOrNull()?.localizedMessage
                        ?: "Không thể tải danh sách phim."
                }
                seriesRes.getOrNull()?.let { if (it.isNotEmpty()) seriesMovies = it }
                singleRes.getOrNull()?.let { if (it.isNotEmpty()) singleMovies = it }
                animeRes.getOrNull()?.let { if (it.isNotEmpty()) animeMovies = it }
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

    val pagerState = rememberPagerState(pageCount = { featuredHero.size.coerceAtLeast(1) })
    val activeHeroUrl = featuredHero.getOrNull(pagerState.currentPage)?.let {
        it.displayPosterUrl.ifBlank { it.displayThumbUrl }
    } ?: featuredHero.firstOrNull()?.displayPosterUrl

    val gridColumns = AppPreferences.gridColumnsPhone.coerceIn(2, 3)
    val latestRows = remember(visibleHero, gridColumns) {
        visibleHero.chunked(gridColumns)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ManiFlixColors.background(oled))
    ) {
        AmbientArtworkBackground(
            imageUrl = activeHeroUrl,
            oled = oled,
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
                            color = Color.White
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
                                tint = if (AppState.isLoggedIn) accent else Color(0xFFEBEBF0),
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
                        items(categories, key = { it }) { cat ->
                            val selected = selectedCategory == cat
                            Box(
                                modifier = appleGlassSurfaceModifier(
                                    cornerRadius = 50.dp,
                                    highlighted = selected
                                )
                                    .clickable {
                                        selectedCategory = cat
                                        triggerHaptic(haptic)
                                    }
                                    .padding(horizontal = 15.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = Color.White
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
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = errorMessage ?: "Lỗi kết nối",
                                fontSize = 14.sp,
                                color = Color(0xFFA0A0A8),
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
                    if (featuredHero.isNotEmpty()) {
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
                                                    .size(if (active) 7.dp else 5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (active) accent else Color.White.copy(alpha = 0.28f)
                                                    )
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
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                            )
                        }
                    }

                    if (AppState.history.isNotEmpty()) {
                        item(key = "continue_watching") {
                            ContinueWatchingRail(
                                entries = AppState.history.take(10),
                                accent = accent,
                                onMovieClick = onMovieClick
                            )
                        }
                    }

                    if ((selectedCategory == "Tất cả" || selectedCategory == "Phim bộ") &&
                        visibleSeries.isNotEmpty()
                    ) {
                        item(key = "rail_series") {
                            HorizontalMovieRail(
                                title = "Phim bộ mới",
                                items = visibleSeries,
                                onMovieClick = onMovieClick
                            )
                        }
                    }

                    if ((selectedCategory == "Tất cả" || selectedCategory == "Phim lẻ") &&
                        visibleSingle.isNotEmpty()
                    ) {
                        item(key = "rail_single") {
                            HorizontalMovieRail(
                                title = "Phim lẻ tuyển chọn",
                                items = visibleSingle,
                                onMovieClick = onMovieClick
                            )
                        }
                    }

                    if ((selectedCategory == "Tất cả" || selectedCategory == "Hoạt hình") &&
                        visibleAnime.isNotEmpty()
                    ) {
                        item(key = "rail_anime") {
                            HorizontalMovieRail(
                                title = "Hoạt hình & Anime",
                                items = visibleAnime,
                                onMovieClick = onMovieClick
                            )
                        }
                    }

                    if (latestRows.isNotEmpty()) {
                        item(key = "latest_header") {
                            Text(
                                text = "Mới cập nhật",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        items(
                            items = latestRows,
                            key = { row -> row.joinToString("-") { it.slug } }
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
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .clip(shape)
            .border(0.7.dp, Color.White.copy(alpha = 0.14f), shape)
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
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(movie.displayThumbUrl.ifBlank { movie.displayPosterUrl })
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
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.30f),
                        1.0f to Color.Black.copy(alpha = 0.90f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                            .background(accent.copy(alpha = 0.88f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                movie.lang.takeIf { it.isNotBlank() }?.let { lang ->
                    Text(
                        text = lang,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDADAE0)
                    )
                }
                movie.year.takeIf { it > 0 }?.let { year ->
                    Text(
                        text = "• $year",
                        fontSize = 10.sp,
                        color = Color(0xFFAEAEB4)
                    )
                }
            }

            Text(
                text = movie.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            movie.originName.takeIf { it.isNotBlank() }?.let { origin ->
                Text(
                    text = origin,
                    fontSize = 12.sp,
                    color = Color(0xFFB4B4BC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Xem tiếp",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
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
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
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
                            color = Color.White,
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
                                    tint = Color(0xFFA6A6AC),
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
                        color = Color(0xFF8E8E93),
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
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            items(items, key = { it.slug }) { movie ->
                MoviePosterCard(
                    movie = movie,
                    modifier = Modifier.width(120.dp),
                    onClick = { onMovieClick(movie.slug, movie) }
                )
            }
        }
    }
}
