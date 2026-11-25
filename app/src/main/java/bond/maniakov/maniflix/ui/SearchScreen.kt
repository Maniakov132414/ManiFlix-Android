package bond.maniakov.maniflix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.ManiFlixApi
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    bottomContentPadding: Dp = 16.dp,
    onMovieClick: (slug: String, seedMovie: MovieItem?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()
    val isLight = ManiFlixColors.isLight()

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    val quickTopics = remember {
        listOf("Hành động", "Khoa học viễn tưởng", "Hình sự", "Tâm lý", "Hoạt hình", "Kinh dị")
    }

    suspend fun executeSearch(term: String) {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) {
            results = emptyList()
            hasSearched = false
            isSearching = false
            return
        }
        isSearching = true
        try {
            results = ManiFlixApi.searchMovies(trimmed, 1)
        } catch (_: Throwable) {
            results = emptyList()
        } finally {
            hasSearched = true
            isSearching = false
        }
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            results = emptyList()
            hasSearched = false
            isSearching = false
            return@LaunchedEffect
        }
        delay(320L)
        executeSearch(trimmed)
    }

    val visibleResults = results.filter { !AppState.isBlocked(it.slug) }
    val gridColumns = adaptiveGridColumns(LocalConfiguration.current.screenWidthDp)
    val resultRows = remember(visibleResults, gridColumns) {
        visibleResults.chunked(gridColumns)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "search_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tìm kiếm",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = "Tên phim, diễn viên...",
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = textSecondary
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Xoá từ khoá",
                                    tint = textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            scope.launch { executeSearch(query) }
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        cursorColor = accent,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = ManiFlixColors.surface(),
                        unfocusedContainerColor = ManiFlixColors.surface()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "quick_topics") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickTopics, key = { it }) { topic ->
                    val highlighted = query.equals(topic, ignoreCase = true)
                    Box(
                        modifier = appleGlassSurfaceModifier(
                            cornerRadius = 50.dp,
                            highlighted = highlighted
                        )
                            .clickable {
                                triggerHaptic(haptic)
                                focusManager.clearFocus()
                                query = topic
                                scope.launch { executeSearch(topic) }
                            }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = topic,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (highlighted && isLight) accent else textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (isSearching) {
            item(key = "searching_loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accent, strokeWidth = 2.5.dp)
                }
            }
        } else if (visibleResults.isEmpty()) {
            item(key = "empty_search") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = if (hasSearched) {
                            "Không có kết quả cho \"$query\""
                        } else {
                            "Tìm theo tên phim, tên gốc hoặc diễn viên"
                        },
                        fontSize = 14.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = resultRows,
                key = { row -> row.joinToString("-") { it.slug } },
                contentType = { "movie_row" }
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
