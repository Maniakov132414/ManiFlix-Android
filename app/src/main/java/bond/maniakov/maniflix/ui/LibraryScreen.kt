package bond.maniakov.maniflix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.player.PlayerEngine
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    bottomContentPadding: Dp = 16.dp,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    val tabTitles = listOf(
        "Đang xem",
        "Đã lưu",
        "Đã ẩn (${AppState.blockedMovies.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Text(
            text = "Tủ phim",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp)
        )

        // Apple Music Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ManiFlixColors.surfaceElevated())
                .border(0.6.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabTitles.forEachIndexed { idx, title ->
                val active = selectedTab == idx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) accent else Color.Transparent)
                        .clickable {
                            selectedTab = idx
                            triggerHaptic(haptic)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) Color.White else textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    runCatching { AppState.refreshLibrary() }
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = bottomContentPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!AppState.isLoggedIn) {
                            item(key = "sync_banner") {
                                SyncAccountBanner(
                                    accent = accent,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }

                        if (AppState.history.isEmpty()) {
                            item(key = "empty_history") {
                                LibraryEmptyState(
                                    icon = Icons.Rounded.History,
                                    message = "Chưa có phim đang xem.",
                                    textSecondary = textSecondary
                                )
                            }
                        } else {
                            items(AppState.history, key = { it.slug }, contentType = { "history_item" }) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(appleGlassSurfaceModifier(cornerRadius = 12.dp))
                                        .clickable { onMovieClick(entry.slug, entry.toMovieItem()) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(entry.displayPosterUrl)
                                            .size(240, 145)
                                            .precision(Precision.INEXACT)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = entry.name.ifBlank { entry.slug },
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 96.dp, height = 58.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ManiFlixColors.surfaceElevated())
                                            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = entry.name.ifBlank { entry.slug },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${entry.episodeName.ifBlank { "Tập 1" }} • ${PlayerEngine.formatTime(entry.currentTime)}",
                                            fontSize = 11.5.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        LinearProgressIndicator(
                                            progress = { entry.progressRatio.coerceIn(0f, 1f) },
                                            color = accent,
                                            trackColor = borderColor,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(CircleShape)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            triggerHaptic(haptic)
                                            scope.launch { AppState.deleteHistory(entry.slug) }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Xoá khỏi lịch sử",
                                            tint = textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (!AppState.isLoggedIn) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Đồng bộ danh sách đã lưu",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Đăng nhập để lưu và đồng bộ các phim bạn yêu thích trên mọi thiết bị.",
                                fontSize = 12.5.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { AppState.showAuthSheet = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text(
                                    text = "Đăng nhập",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    } else if (AppState.favorites.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LibraryEmptyState(
                                icon = Icons.Rounded.BookmarkBorder,
                                message = "Chưa có phim đã lưu.",
                                textSecondary = textSecondary
                            )
                        }
                    } else {
                        val gridColumns = adaptiveGridColumns(LocalConfiguration.current.screenWidthDp)
                        val favItems = remember(AppState.favorites) {
                            AppState.favorites.map { it.toMovieItem() }
                        }
                        val favRows = remember(favItems, gridColumns) {
                            favItems.chunked(gridColumns)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = bottomContentPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                items = favRows,
                                key = { row -> row.joinToString("-") { it.slug } },
                                contentType = { "movie_row" }
                            ) { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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

                else -> {
                    if (AppState.blockedMovies.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LibraryEmptyState(
                                icon = Icons.Rounded.VisibilityOff,
                                message = "Chưa có phim nào bị ẩn.",
                                textSecondary = textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = bottomContentPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(AppState.blockedMovies, key = { it.slug }, contentType = { "blocked_item" }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(appleGlassSurfaceModifier(cornerRadius = 12.dp))
                                        .clickable { onMovieClick(item.slug, null) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(item.displayPosterUrl)
                                            .size(160, 220)
                                            .precision(Precision.INEXACT)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 52.dp, height = 74.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ManiFlixColors.surfaceElevated())
                                            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Đang ẩn khỏi trang chủ & tìm kiếm",
                                            fontSize = 11.5.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(accent)
                                            .clickable {
                                                triggerHaptic(haptic)
                                                AppState.unblockMovie(item.slug)
                                            }
                                            .padding(horizontal = 13.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "Hiện lại",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1
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

@Composable
private fun SyncAccountBanner(
    accent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(appleGlassSurfaceModifier(cornerRadius = 12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Chưa đăng nhập",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Đăng nhập để đồng bộ lịch sử xem trên nhiều thiết bị.",
                fontSize = 11.5.sp,
                color = textSecondary
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(accent)
                .clickable { AppState.showAuthSheet = true }
                .padding(horizontal = 13.dp, vertical = 7.dp)
        ) {
            Text(
                text = "Đăng nhập",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LibraryEmptyState(
    icon: ImageVector,
    message: String,
    textSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = message,
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
