package bond.maniakov.maniflix.ui

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.SurfaceStyle
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

fun triggerHaptic(haptic: HapticFeedback, strong: Boolean = false) {
    if (!AppPreferences.hapticsEnabled) return
    haptic.performHapticFeedback(
        if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
    )
}

@Composable
fun appleGlassSurfaceModifier(
    cornerRadius: Dp = 14.dp,
    highlighted: Boolean = false
): Modifier {
    val style = AppPreferences.surfaceStyle
    val oled = AppPreferences.oledBlackEnabled
    val accent = ManiFlixColors.accent()
    val shape = RoundedCornerShape(cornerRadius)
    val fillColor = when {
        highlighted -> accent.copy(alpha = 0.24f)
        style == SurfaceStyle.TRANSLUCENT -> Color.White.copy(alpha = if (oled) 0.08f else 0.11f)
        style == SurfaceStyle.MINIMAL -> ManiFlixColors.surface(oled)
        else -> Color(0xFF1F1F23).copy(alpha = if (oled) 0.85f else 0.92f)
    }
    val borderColor = when {
        highlighted -> accent.copy(alpha = 0.85f)
        style == SurfaceStyle.TRANSLUCENT -> Color.White.copy(alpha = 0.18f)
        else -> Color.White.copy(alpha = 0.10f)
    }
    return Modifier
        .clip(shape)
        .background(fillColor)
        .border(width = if (highlighted) 1.dp else 0.6.dp, color = borderColor, shape = shape)
}

@Composable
fun AmbientArtworkBackground(
    imageUrl: String?,
    oled: Boolean,
    height: Dp = 360.dp
) {
    val bgColor = ManiFlixColors.background(oled)
    if (!AppPreferences.ambientArtworkEnabled || imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ManiFlixColors.accent().copy(alpha = if (oled) 0.08f else 0.12f),
                            bgColor
                        )
                    )
                )
        )
        return
    }

    val context = LocalContext.current
    val supportsNativeBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val artworkAlpha = when {
        !supportsNativeBlur -> if (oled) 0.16f else 0.22f
        oled -> 0.34f
        else -> 0.46f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (supportsNativeBlur) Modifier.blur(50.dp) else Modifier)
                .alpha(artworkAlpha)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.20f),
                        0.45f to bgColor.copy(alpha = if (supportsNativeBlur) 0.56f else 0.72f),
                        1.0f to bgColor
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoviePosterCard(
    movie: MovieItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(10.dp)
    val badgeText = movie.quality.takeIf { it.isNotBlank() }
        ?: movie.episodeCurrent.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(cardShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(0.6.dp, Color.White.copy(alpha = 0.10f), cardShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(movie.displayPosterUrl.ifBlank { movie.displayThumbUrl })
                    .crossfade(true)
                    .build(),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (!badgeText.isNullOrBlank()) {
                Text(
                    text = badgeText,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = movie.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(2.dp))

            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 24.dp, height = 22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            triggerHaptic(haptic)
                            menuExpanded = true
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "Tuỳ chọn phim",
                        tint = Color(0xFFA6A6AC),
                        modifier = Modifier.size(16.dp)
                    )
                }

                MovieContextDropdownMenu(
                    movie = movie,
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false }
                )
            }
        }

        val yearStr = movie.year.takeIf { it > 0 }?.toString()
        val langStr = movie.lang.takeIf { it.isNotBlank() }
        val metaLine = listOfNotNull(yearStr, langStr ?: if (yearStr != null) "Vietsub" else null)
            .joinToString(" • ")

        if (metaLine.isNotBlank()) {
            Text(
                text = metaLine,
                fontSize = 11.sp,
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MovieContextDropdownMenu(
    movie: MovieItem,
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFavorite = AppState.isFavorite(movie.slug)
    val isBlocked = AppState.isBlocked(movie.slug)
    val hasHistory = AppState.hasHistory(movie.slug)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF222226)
    ) {
        DropdownMenuItem(
            text = { Text("Xem ngay", color = Color.White) },
            leadingIcon = {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
            },
            onClick = {
                onDismiss()
                scope.launch { AppState.quickWatch(context, movie.slug) }
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    if (isFavorite) "Xoá khỏi Tủ phim" else "Thêm vào Tủ phim",
                    color = Color.White
                )
            },
            leadingIcon = {
                Icon(
                    if (isFavorite) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd,
                    contentDescription = null,
                    tint = if (isFavorite) ManiFlixColors.accent() else Color.White
                )
            },
            onClick = {
                onDismiss()
                scope.launch { AppState.toggleFavoriteItem(movie) }
            }
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        DropdownMenuItem(
            text = { Text("Thích", color = Color.White) },
            leadingIcon = {
                Icon(Icons.Rounded.ThumbUp, contentDescription = null, tint = Color.White)
            },
            onClick = {
                onDismiss()
                AppState.likeMovie(movie)
            }
        )

        DropdownMenuItem(
            text = { Text("Đề xuất ít lại", color = Color.White) },
            leadingIcon = {
                Icon(Icons.Rounded.ThumbDown, contentDescription = null, tint = Color.White)
            },
            onClick = {
                onDismiss()
                AppState.dislikeMovie(movie)
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    if (isBlocked) "Hiện lại phim" else "Ẩn phim",
                    color = if (isBlocked) Color.White else Color(0xFFFF453A)
                )
            },
            leadingIcon = {
                Icon(
                    if (isBlocked) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    tint = if (isBlocked) Color.White else Color(0xFFFF453A)
                )
            },
            onClick = {
                onDismiss()
                if (isBlocked) {
                    AppState.unblockMovie(movie.slug)
                } else {
                    AppState.blockMovie(movie)
                }
            }
        )

        if (hasHistory) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DropdownMenuItem(
                text = { Text("Xoá khỏi lịch sử", color = Color(0xFFFF453A)) },
                leadingIcon = {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFFF453A))
                },
                onClick = {
                    onDismiss()
                    scope.launch { AppState.deleteHistory(movie.slug) }
                }
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        DropdownMenuItem(
            text = { Text("Chia sẻ liên kết", color = Color.White) },
            leadingIcon = {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
            },
            onClick = {
                onDismiss()
                AppState.shareMovie(context, movie.name, movie.slug)
            }
        )
    }
}
