package bond.maniakov.maniflix.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.ManiFlixApi
import bond.maniakov.maniflix.model.EpisodeServer
import bond.maniakov.maniflix.model.FranchiseSeason
import bond.maniakov.maniflix.model.HistoryEntry
import bond.maniakov.maniflix.model.MovieDetail
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    slug: String,
    seedMovie: MovieItem?,
    bottomContentPadding: Dp = 24.dp,
    onBack: () -> Unit,
    onSelectMovie: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()
    val isLight = ManiFlixColors.isLight()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isTwoPane = screenWidthDp >= 640

    var detail by remember(slug) { mutableStateOf<MovieDetail?>(null) }
    var servers by remember(slug) { mutableStateOf<List<EpisodeServer>>(emptyList()) }
    var franchiseSeasons by remember(slug) { mutableStateOf<List<FranchiseSeason>>(emptyList()) }
    var relatedMovies by remember(slug) { mutableStateOf<List<MovieItem>>(emptyList()) }
    var selectedServerIdx by remember(slug) { mutableIntStateOf(0) }
    var isFranchiseExpanded by remember(slug) { mutableStateOf(true) }
    var isLoading by remember(slug) { mutableStateOf(true) }
    var errorMsg by remember(slug) { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(slug) {
        isLoading = true
        errorMsg = null
        try {
            val result = ManiFlixApi.fetchMovieDetail(slug)
            detail = result.movie
            servers = result.episodes
            val loadedMovie = result.movie
            if (loadedMovie == null) {
                errorMsg = "Không tìm thấy thông tin phim."
            } else {
                franchiseSeasons = runCatching {
                    ManiFlixApi.fetchFranchise(slug, loadedMovie.name)
                }.getOrNull().orEmpty()

                val firstCatSlug = loadedMovie.category.firstOrNull { it.slug.isNotBlank() }?.slug
                val candidateList = runCatching {
                    if (firstCatSlug != null) {
                        ManiFlixApi.fetchCategory(firstCatSlug, 1)
                    } else {
                        ManiFlixApi.fetchMovies(1)
                    }
                }.getOrNull().orEmpty().ifEmpty {
                    runCatching { ManiFlixApi.fetchMovies(1) }.getOrNull().orEmpty()
                }
                relatedMovies = candidateList
                    .filter { it.slug != slug && !AppState.isBlocked(it.slug) }
                    .take(12)
            }
        } catch (t: Throwable) {
            errorMsg = t.localizedMessage ?: "Không thể tải thông tin phim."
        } finally {
            isLoading = false
        }
    }

    val heroArtworkUrl = detail?.displayPosterUrl?.takeIf { it.isNotBlank() }
        ?: seedMovie?.displayPosterUrl?.takeIf { it.isNotBlank() }
        ?: detail?.displayThumbUrl?.takeIf { it.isNotBlank() }
        ?: seedMovie?.displayThumbUrl

    val resumeEntry = AppState.history.firstOrNull { it.slug == (detail?.slug ?: slug) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AmbientArtworkBackground(
            imageUrl = heroArtworkUrl,
            height = 420.dp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomContentPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLight) ManiFlixColors.surface().copy(alpha = 0.85f)
                            else Color.Black.copy(alpha = 0.42f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = textPrimary
                    )
                }
            }

            if (!isTwoPane) {
                DetailHeroHeader(
                    heroArtworkUrl = heroArtworkUrl,
                    detail = detail,
                    seedMovie = seedMovie,
                    posterWidth = 158.dp,
                    posterHeight = 228.dp,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    borderColor = borderColor
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            if (isLoading) {
                if (isTwoPane) {
                    DetailHeroHeader(
                        heroArtworkUrl = heroArtworkUrl,
                        detail = detail,
                        seedMovie = seedMovie,
                        posterWidth = 180.dp,
                        posterHeight = 260.dp,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        borderColor = borderColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accent, strokeWidth = 2.5.dp)
                }
            } else if (errorMsg != null) {
                if (isTwoPane) {
                    DetailHeroHeader(
                        heroArtworkUrl = heroArtworkUrl,
                        detail = detail,
                        seedMovie = seedMovie,
                        posterWidth = 180.dp,
                        posterHeight = 260.dp,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        borderColor = borderColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = errorMsg ?: "",
                    fontSize = 14.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(horizontal = if (isTwoPane) 20.dp else 16.dp)
                )
            } else {
                detail?.let { movie ->
                    val activeEpisodes = servers.getOrNull(selectedServerIdx)?.serverData.orEmpty()
                    val targetIdx = resumeEntry?.episodeIndex
                        ?.takeIf { it in activeEpisodes.indices }
                        ?: 0
                    val playLabel = if (
                        resumeEntry != null &&
                        resumeEntry.episodeName.isNotBlank() &&
                        targetIdx in activeEpisodes.indices
                    ) {
                        "Xem tiếp • ${resumeEntry.episodeName}"
                    } else {
                        "Phát"
                    }

                    val onPlayPrimary = {
                        triggerHaptic(haptic)
                        AppState.startPlayback(
                            context = context,
                            movie = movie,
                            servers = servers,
                            serverIndex = selectedServerIdx,
                            episodeIndex = targetIdx
                        )
                    }
                    val onToggleFavorite = {
                        triggerHaptic(haptic)
                        scope.launch { AppState.toggleFavorite(movie) }
                        Unit
                    }
                    val onPlayEpisode: (Int) -> Unit = { epIdx ->
                        triggerHaptic(haptic)
                        AppState.startPlayback(
                            context = context,
                            movie = movie,
                            servers = servers,
                            serverIndex = selectedServerIdx,
                            episodeIndex = epIdx
                        )
                    }

                    if (isTwoPane) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(0.42f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                DetailHeroHeader(
                                    heroArtworkUrl = heroArtworkUrl,
                                    detail = detail,
                                    seedMovie = seedMovie,
                                    posterWidth = 180.dp,
                                    posterHeight = 260.dp,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    borderColor = borderColor
                                )

                                DetailActionRow(
                                    movie = movie,
                                    playLabel = playLabel,
                                    hasEpisodes = activeEpisodes.isNotEmpty(),
                                    horizontalPadding = 0.dp,
                                    accent = accent,
                                    textPrimary = textPrimary,
                                    menuExpanded = menuExpanded,
                                    onMenuOpen = {
                                        triggerHaptic(haptic)
                                        menuExpanded = true
                                    },
                                    onMenuDismiss = { menuExpanded = false },
                                    onPlay = onPlayPrimary,
                                    onToggleFavorite = onToggleFavorite
                                )

                                DetailMetadataPills(
                                    movie = movie,
                                    horizontalPadding = 0.dp,
                                    textPrimary = textPrimary
                                )

                                DetailSynopsisSection(
                                    movie = movie,
                                    horizontalPadding = 0.dp,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )

                                DetailCreditsSection(
                                    movie = movie,
                                    horizontalPadding = 0.dp,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.58f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                DetailFranchiseSection(
                                    slug = slug,
                                    franchiseSeasons = franchiseSeasons,
                                    isFranchiseExpanded = isFranchiseExpanded,
                                    horizontalPadding = 0.dp,
                                    accent = accent,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    isLight = isLight,
                                    onToggleExpand = {
                                        isFranchiseExpanded = !isFranchiseExpanded
                                        triggerHaptic(haptic)
                                    },
                                    onSelectSeason = { seasonSlug ->
                                        triggerHaptic(haptic)
                                        onSelectMovie(seasonSlug, null)
                                    }
                                )

                                DetailEpisodesSection(
                                    servers = servers,
                                    selectedServerIdx = selectedServerIdx,
                                    resumeEntry = resumeEntry,
                                    horizontalPadding = 0.dp,
                                    accent = accent,
                                    textPrimary = textPrimary,
                                    isLight = isLight,
                                    onSelectServer = { idx ->
                                        selectedServerIdx = idx
                                        triggerHaptic(haptic)
                                    },
                                    onPlayEpisode = onPlayEpisode
                                )

                                DetailRelatedRail(
                                    relatedMovies = relatedMovies,
                                    horizontalPadding = 0.dp,
                                    cardWidth = 132.dp,
                                    textPrimary = textPrimary,
                                    onSelectMovie = onSelectMovie
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DetailActionRow(
                                movie = movie,
                                playLabel = playLabel,
                                hasEpisodes = activeEpisodes.isNotEmpty(),
                                horizontalPadding = 16.dp,
                                accent = accent,
                                textPrimary = textPrimary,
                                menuExpanded = menuExpanded,
                                onMenuOpen = {
                                    triggerHaptic(haptic)
                                    menuExpanded = true
                                },
                                onMenuDismiss = { menuExpanded = false },
                                onPlay = onPlayPrimary,
                                onToggleFavorite = onToggleFavorite
                            )

                            DetailMetadataPills(
                                movie = movie,
                                horizontalPadding = 16.dp,
                                textPrimary = textPrimary
                            )

                            DetailFranchiseSection(
                                slug = slug,
                                franchiseSeasons = franchiseSeasons,
                                isFranchiseExpanded = isFranchiseExpanded,
                                horizontalPadding = 16.dp,
                                accent = accent,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                isLight = isLight,
                                onToggleExpand = {
                                    isFranchiseExpanded = !isFranchiseExpanded
                                    triggerHaptic(haptic)
                                },
                                onSelectSeason = { seasonSlug ->
                                    triggerHaptic(haptic)
                                    onSelectMovie(seasonSlug, null)
                                }
                            )

                            DetailEpisodesSection(
                                servers = servers,
                                selectedServerIdx = selectedServerIdx,
                                resumeEntry = resumeEntry,
                                horizontalPadding = 16.dp,
                                accent = accent,
                                textPrimary = textPrimary,
                                isLight = isLight,
                                onSelectServer = { idx ->
                                    selectedServerIdx = idx
                                    triggerHaptic(haptic)
                                },
                                onPlayEpisode = onPlayEpisode
                            )

                            DetailSynopsisSection(
                                movie = movie,
                                horizontalPadding = 16.dp,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )

                            DetailCreditsSection(
                                movie = movie,
                                horizontalPadding = 16.dp,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )

                            DetailRelatedRail(
                                relatedMovies = relatedMovies,
                                horizontalPadding = 16.dp,
                                cardWidth = 120.dp,
                                textPrimary = textPrimary,
                                onSelectMovie = onSelectMovie
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeroHeader(
    heroArtworkUrl: String?,
    detail: MovieDetail?,
    seedMovie: MovieItem?,
    posterWidth: Dp,
    posterHeight: Dp,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val posterShape = RoundedCornerShape(12.dp)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(heroArtworkUrl)
                .size(320, 480)
                .precision(Precision.INEXACT)
                .crossfade(true)
                .build(),
            contentDescription = detail?.name ?: seedMovie?.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = posterWidth, height = posterHeight)
                .clip(posterShape)
                .background(ManiFlixColors.surfaceElevated())
                .border(0.7.dp, borderColor, posterShape)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = detail?.name ?: seedMovie?.name ?: "",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        val originName = detail?.originName?.takeIf { it.isNotBlank() }
            ?: seedMovie?.originName?.takeIf { it.isNotBlank() }
        if (!originName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = originName,
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        val metaParts = listOfNotNull(
            (detail?.year?.takeIf { it > 0 } ?: seedMovie?.year?.takeIf { it > 0 })?.toString(),
            detail?.time?.takeIf { it.isNotBlank() } ?: seedMovie?.time?.takeIf { it.isNotBlank() },
            detail?.lang?.takeIf { it.isNotBlank() } ?: seedMovie?.lang?.takeIf { it.isNotBlank() }
        )
        if (metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = metaParts.joinToString(" • "),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailActionRow(
    movie: MovieDetail,
    playLabel: String,
    hasEpisodes: Boolean,
    horizontalPadding: Dp,
    accent: Color,
    textPrimary: Color,
    menuExpanded: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val menuMovie = remember(movie) { movie.toMovieItem() }
    val isFav = AppState.isFavorite(movie.slug)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPlay,
            enabled = hasEpisodes,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                disabledContainerColor = accent.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = playLabel,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = appleGlassSurfaceModifier(cornerRadius = 12.dp)
                .size(46.dp)
                .clickable(onClick = onToggleFavorite)
        ) {
            Icon(
                imageVector = if (isFav) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = "Lưu tủ phim",
                tint = if (isFav) accent else textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = appleGlassSurfaceModifier(cornerRadius = 12.dp)
                    .size(46.dp)
                    .clickable(onClick = onMenuOpen)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "Thêm tuỳ chọn",
                    tint = textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            MovieContextDropdownMenu(
                movie = menuMovie,
                expanded = menuExpanded,
                onDismiss = onMenuDismiss
            )
        }
    }
}

@Composable
private fun DetailMetadataPills(
    movie: MovieDetail,
    horizontalPadding: Dp,
    textPrimary: Color
) {
    val pills = buildList {
        movie.quality.takeIf { it.isNotBlank() }?.let { add(it) }
        movie.episodeCurrent.takeIf { it.isNotBlank() }?.let { add(it) }
        movie.country.take(3).forEach { c ->
            c.name.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        movie.category.take(5).forEach { tag ->
            tag.name.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }.distinct()

    if (pills.isNotEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(pills) { _, pill ->
                Text(
                    text = pill,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    modifier = appleGlassSurfaceModifier(cornerRadius = 50.dp)
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailFranchiseSection(
    slug: String,
    franchiseSeasons: List<FranchiseSeason>,
    isFranchiseExpanded: Boolean,
    horizontalPadding: Dp,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    isLight: Boolean,
    onToggleExpand: () -> Unit,
    onSelectSeason: (String) -> Unit
) {
    if (franchiseSeasons.size <= 1) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .then(appleGlassSurfaceModifier(cornerRadius = 14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Các phần phim (${franchiseSeasons.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = if (isFranchiseExpanded) "Thu gọn" else "Mở rộng",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = if (isFranchiseExpanded) {
                    Icons.Rounded.KeyboardArrowUp
                } else {
                    Icons.Rounded.KeyboardArrowDown
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = isFranchiseExpanded) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                franchiseSeasons.forEach { season ->
                    val isCurrent = season.slug == slug
                    Box(
                        modifier = appleGlassSurfaceModifier(
                            cornerRadius = 10.dp,
                            highlighted = isCurrent
                        )
                            .clickable {
                                if (!isCurrent) {
                                    onSelectSeason(season.slug)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = season.displayFullTitle,
                            fontSize = 12.5.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isCurrent && isLight) accent else textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailEpisodesSection(
    servers: List<EpisodeServer>,
    selectedServerIdx: Int,
    resumeEntry: HistoryEntry?,
    horizontalPadding: Dp,
    accent: Color,
    textPrimary: Color,
    isLight: Boolean,
    onSelectServer: (Int) -> Unit,
    onPlayEpisode: (Int) -> Unit
) {
    if (servers.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(servers) { idx, srv ->
                val selected = selectedServerIdx == idx
                Box(
                    modifier = appleGlassSurfaceModifier(
                        cornerRadius = 50.dp,
                        highlighted = selected
                    )
                        .clickable { onSelectServer(idx) }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = srv.serverName,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected && isLight) accent else textPrimary
                    )
                }
            }
        }

        val activeServer = servers.getOrNull(selectedServerIdx)
        if (activeServer != null) {
            Text(
                text = "Danh sách tập (${activeServer.serverData.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(horizontal = horizontalPadding)
            )

            val watchedEpIdx = resumeEntry?.episodeIndex
            val rows = activeServer.serverData.chunked(4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var runningIndex = 0
                rows.forEach { rowEpisodes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEpisodes.forEach { ep ->
                            val epIdx = runningIndex++
                            val isWatchedEp = watchedEpIdx == epIdx
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        appleGlassSurfaceModifier(
                                            cornerRadius = 9.dp,
                                            highlighted = isWatchedEp
                                        )
                                    )
                                    .clickable { onPlayEpisode(epIdx) }
                                    .padding(horizontal = 6.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = ep.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isWatchedEp) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isWatchedEp && isLight) accent else textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(4 - rowEpisodes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSynopsisSection(
    movie: MovieDetail,
    horizontalPadding: Dp,
    textPrimary: Color,
    textSecondary: Color
) {
    if (movie.plainSynopsis.isBlank()) return
    Column(
        modifier = Modifier.padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Giới thiệu",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            text = movie.plainSynopsis,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = textSecondary
        )
    }
}

@Composable
private fun DetailCreditsSection(
    movie: MovieDetail,
    horizontalPadding: Dp,
    textPrimary: Color,
    textSecondary: Color
) {
    val directors = movie.director.filter { it.isNotBlank() }
    val actors = movie.actor.filter { it.isNotBlank() }
    if (directors.isEmpty() && actors.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .then(appleGlassSurfaceModifier(cornerRadius = 14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (directors.isNotEmpty()) {
            DetailMetaLine(
                label = "Đạo diễn",
                value = directors.joinToString(", "),
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
        if (actors.isNotEmpty()) {
            DetailMetaLine(
                label = "Diễn viên",
                value = actors.take(8).joinToString(", "),
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun DetailRelatedRail(
    relatedMovies: List<MovieItem>,
    horizontalPadding: Dp,
    cardWidth: Dp,
    textPrimary: Color,
    onSelectMovie: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    if (relatedMovies.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Có thể bạn cũng thích",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            items(relatedMovies, key = { it.slug }, contentType = { "movie" }) { related ->
                MoviePosterCard(
                    movie = related,
                    modifier = Modifier.width(cardWidth),
                    onClick = { onSelectMovie(related.slug, related) }
                )
            }
        }
    }
}

@Composable
private fun DetailMetaLine(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textSecondary
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            color = textPrimary,
            lineHeight = 18.sp
        )
    }
}
