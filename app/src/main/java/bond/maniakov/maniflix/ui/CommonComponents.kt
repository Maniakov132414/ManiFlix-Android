package bond.maniakov.maniflix.ui

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import coil.size.Precision
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
    val isLight = ManiFlixColors.isLight()
    val accent = ManiFlixColors.accent()
    val surface = ManiFlixColors.surface()
    val shape = RoundedCornerShape(cornerRadius)

    val fillColor = when {
        highlighted -> accent.copy(alpha = if (isLight) 0.16f else 0.24f)
        style == SurfaceStyle.TRANSLUCENT -> if (isLight) {
            surface.copy(alpha = 0.84f)
        } else {
            Color.White.copy(alpha = if (AppPreferences.oledBlackEnabled) 0.08f else 0.11f)
        }
        style == SurfaceStyle.MINIMAL -> surface
        else -> surface.copy(alpha = if (isLight) 0.96f else 0.92f)
    }

    val borderColor = when {
        highlighted -> accent.copy(alpha = 0.90f)
        else -> ManiFlixColors.cardBorder()
    }

    return Modifier
        .clip(shape)
        .background(fillColor)
        .border(width = if (highlighted) 1.dp else 0.6.dp, color = borderColor, shape = shape)
}

@Composable
fun AmbientArtworkBackground(
    imageUrl: String?,
    oled: Boolean = AppPreferences.oledBlackEnabled,
    height: Dp = 360.dp
) {
    val bgColor = ManiFlixColors.background()
    val accent = ManiFlixColors.accent()
    val isLight = ManiFlixColors.isLight()
    val enabled = AppPreferences.ambientArtworkEnabled && !imageUrl.isNullOrBlank()
    val glowAlpha = when {
        !enabled -> if (isLight) 0.08f else if (oled) 0.07f else 0.11f
        isLight -> 0.16f
        oled -> 0.15f
        else -> 0.24f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = glowAlpha),
                        bgColor.copy(alpha = if (oled) 0.96f else 0.85f),
                        bgColor
                    ),
                    center = Offset(320f, 120f),
                    radius = 900f
                )
            )
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.55f to bgColor.copy(alpha = 0.55f),
                    1.0f to bgColor
                )
            )
    )
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

    val cardShape = RoundedCornerShape(14.dp)
    val badgeText = movie.quality.takeIf { it.isNotBlank() }
        ?: movie.episodeCurrent.takeIf { it.isNotBlank() }
    val is4kBadge = badgeText?.let { it.contains("4K", ignoreCase = true) || it.contains("2160") } == true
    val primaryText = ManiFlixColors.textPrimary()
    val secondaryText = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()
    val liquidBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.30f),
            borderColor,
            Color.White.copy(alpha = 0.08f)
        )
    )

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
                .background(ManiFlixColors.surfaceElevated())
                .border(0.85.dp, liquidBorder, cardShape)
        ) {
            val primaryUrl = movie.displayPosterUrl.ifBlank { movie.displayThumbUrl }
            val secondaryUrl = movie.displayThumbUrl.ifBlank { movie.displayPosterUrl }
            var useFallbackUrl by remember(movie.slug) { mutableStateOf(false) }
            val resolvedUrl = if (!useFallbackUrl) primaryUrl else secondaryUrl

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolvedUrl)
                    .size(320, 480)
                    .precision(Precision.INEXACT)
                    .crossfade(true)
                    .build(),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                onError = {
                    if (!useFallbackUrl && secondaryUrl.isNotBlank() && secondaryUrl != primaryUrl) {
                        useFallbackUrl = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!badgeText.isNullOrBlank()) {
                val pillBg = if (is4kBadge) {
                    Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)))
                } else {
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.74f), Color.Black.copy(alpha = 0.64f)))
                }
                Text(
                    text = badgeText,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (is4kBadge) Color(0xFF090D16) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillBg)
                        .border(0.5.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(6.dp))
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
                color = primaryText,
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
                        tint = secondaryText,
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
                color = secondaryText,
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
    val textPrimary = ManiFlixColors.textPrimary()
    val dividerColor = ManiFlixColors.cardBorder()
    val dangerColor = Color(0xFFFF453A)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = ManiFlixColors.surfaceElevated()
    ) {
        DropdownMenuItem(
            text = { Text("Xem ngay", color = textPrimary) },
            leadingIcon = {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = textPrimary)
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
                    color = textPrimary
                )
            },
            leadingIcon = {
                Icon(
                    if (isFavorite) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd,
                    contentDescription = null,
                    tint = if (isFavorite) ManiFlixColors.accent() else textPrimary
                )
            },
            onClick = {
                onDismiss()
                scope.launch { AppState.toggleFavoriteItem(movie) }
            }
        )

        HorizontalDivider(color = dividerColor)

        DropdownMenuItem(
            text = { Text("Thích", color = textPrimary) },
            leadingIcon = {
                Icon(Icons.Rounded.ThumbUp, contentDescription = null, tint = textPrimary)
            },
            onClick = {
                onDismiss()
                AppState.likeMovie(movie)
            }
        )

        DropdownMenuItem(
            text = { Text("Đề xuất ít lại", color = textPrimary) },
            leadingIcon = {
                Icon(Icons.Rounded.ThumbDown, contentDescription = null, tint = textPrimary)
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
                    color = if (isBlocked) textPrimary else dangerColor
                )
            },
            leadingIcon = {
                Icon(
                    if (isBlocked) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    tint = if (isBlocked) textPrimary else dangerColor
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
            HorizontalDivider(color = dividerColor)
            DropdownMenuItem(
                text = { Text("Xoá khỏi lịch sử", color = dangerColor) },
                leadingIcon = {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = dangerColor)
                },
                onClick = {
                    onDismiss()
                    scope.launch { AppState.deleteHistory(movie.slug) }
                }
            )
        }

        HorizontalDivider(color = dividerColor)

        DropdownMenuItem(
            text = { Text("Chia sẻ liên kết", color = textPrimary) },
            leadingIcon = {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = textPrimary)
            },
            onClick = {
                onDismiss()
                AppState.shareMovie(context, movie.name, movie.slug)
            }
        )
    }
}
