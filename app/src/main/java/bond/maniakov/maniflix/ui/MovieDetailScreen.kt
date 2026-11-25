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
import bond.maniakov.maniflix.model.EpisodeServer
import bond.maniakov.maniflix.model.FranchiseSeason
import bond.maniakov.maniflix.model.MovieDetail
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val oled = AppPreferences.oledBlackEnabled
    val accent = ManiFlixColors.accent()

    var detail by remember(slug) { mutableStateOf<MovieDetail?>(null) }
    var servers by remember(slug) { mutableStateOf<List<EpisodeServer>>(emptyList()) }
    var franchiseSeasons by remember(slug) { mutableStateOf<List<FranchiseSeason>>(emptyList()) }
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
            if (result.movie == null) {
                errorMsg = "Không tìm thấy thông tin phim."
            } else {
                val franchise = runCatching {
                    ManiFlixApi.fetchFranchise(slug, result.movie?.name.orEmpty())
                }.getOrNull()
                franchiseSeasons = franchise.orEmpty()
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
            .background(ManiFlixColors.background(oled))
    ) {
        AmbientArtworkBackground(
            imageUrl = heroArtworkUrl,
            oled = oled,
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
                        .background(Color.Black.copy(alpha = 0.42f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White
                    )
                }
            }

            // Apple Music Album Hero
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
                        .crossfade(true)
                        .build(),
                    contentDescription = detail?.name ?: seedMovie?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 158.dp, height = 228.dp)
                        .clip(posterShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.7.dp, Color.White.copy(alpha = 0.18f), posterShape)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = detail?.name ?: seedMovie?.name ?: "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                val originName = detail?.originName?.takeIf { it.isNotBlank() }
                    ?: seedMovie?.originName?.takeIf { it.isNotBlank() }
                if (!originName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = originName,
                        fontSize = 14.sp,
                        color = Color(0xFFB8B8C0),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
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
                        color = Color(0xFF98989F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp)
                }
            } else if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    fontSize = 14.sp,
                    color = Color(0xFFA0A0A8),
                    modifier = Modifier.padding(horizontal = 16.dp)
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

                val menuMovie = remember(movie) { movie.toMovieItem() }
                val isFav = AppState.isFavorite(movie.slug)

                // Action Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            triggerHaptic(haptic)
                            AppState.startPlayback(
                                context = context,
                                movie = movie,
                                servers = servers,
                                serverIndex = selectedServerIdx,
                                episodeIndex = targetIdx
                            )
                        },
                        enabled = activeEpisodes.isNotEmpty(),
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
                            .clickable {
                                triggerHaptic(haptic)
                                scope.launch { AppState.toggleFavorite(movie) }
                            }
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Lưu tủ phim",
                            tint = if (isFav) accent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = appleGlassSurfaceModifier(cornerRadius = 12.dp)
                                .size(46.dp)
                                .clickable {
                                    triggerHaptic(haptic)
                                    menuExpanded = true
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Thêm tuỳ chọn",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        MovieContextDropdownMenu(
                            movie = menuMovie,
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Pills
                val pills = buildList {
                    movie.quality.takeIf { it.isNotBlank() }?.let { add(it) }
                    movie.episodeCurrent.takeIf { it.isNotBlank() }?.let { add(it) }
                    movie.category.take(5).forEach { tag ->
                        tag.name.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
                if (pills.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        itemsIndexed(pills) { _, pill ->
                            Text(
                                text = pill,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFDADAE0),
                                modifier = appleGlassSurfaceModifier(cornerRadius = 50.dp)
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Collapsible Multi-line Wrapping Franchise Section
                if (franchiseSeasons.size > 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .then(appleGlassSurfaceModifier(cornerRadius = 14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isFranchiseExpanded = !isFranchiseExpanded
                                    triggerHaptic(haptic)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Các phần phim (${franchiseSeasons.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = if (isFranchiseExpanded) "Thu gọn" else "Mở rộng",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFA6A6AC)
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
                                    val chipShape = RoundedCornerShape(10.dp)
                                    Box(
                                        modifier = Modifier
                                            .clip(chipShape)
                                            .background(
                                                if (isCurrent) accent.copy(alpha = 0.28f)
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .border(
                                                width = if (isCurrent) 1.2.dp else 0.6.dp,
                                                color = if (isCurrent) accent else Color.White.copy(alpha = 0.14f),
                                                shape = chipShape
                                            )
                                            .clickable {
                                                if (!isCurrent) {
                                                    triggerHaptic(haptic)
                                                    onSelectMovie(season.slug, null)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = season.displayFullTitle,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Server Selector + Episode Grid
                if (servers.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(servers) { idx, srv ->
                            val selected = selectedServerIdx == idx
                            Box(
                                modifier = appleGlassSurfaceModifier(
                                    cornerRadius = 50.dp,
                                    highlighted = selected
                                )
                                    .clickable {
                                        selectedServerIdx = idx
                                        triggerHaptic(haptic)
                                    }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = srv.serverName,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeServer = servers.getOrNull(selectedServerIdx)
                    if (activeServer != null) {
                        Text(
                            text = "Danh sách tập (${activeServer.serverData.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val watchedEpIdx = resumeEntry?.episodeIndex
                        val rows = activeServer.serverData.chunked(4)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
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
                                                .clickable {
                                                    triggerHaptic(haptic)
                                                    AppState.startPlayback(
                                                        context = context,
                                                        movie = movie,
                                                        servers = servers,
                                                        serverIndex = selectedServerIdx,
                                                        episodeIndex = epIdx
                                                    )
                                                }
                                                .padding(horizontal = 6.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = ep.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isWatchedEp) FontWeight.SemiBold else FontWeight.Medium,
                                                color = Color.White,
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

                if (movie.plainSynopsis.isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Giới thiệu",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = movie.plainSynopsis,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFFC4C4CC)
                        )
                    }
                }
                }
            }
        }
    }
}
