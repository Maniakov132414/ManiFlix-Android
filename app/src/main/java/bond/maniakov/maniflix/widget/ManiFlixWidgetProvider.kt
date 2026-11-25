package bond.maniakov.maniflix.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import bond.maniakov.maniflix.MainActivity
import bond.maniakov.maniflix.R
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.model.ImageUrlResolver
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class ManiFlixWidgetProvider : AppWidgetProvider() {

    private enum class WidgetMode {
        AUTO_SMART,
        CONTINUE,
        FOR_YOU,
        LATEST
    }

    private data class WidgetMovie(
        val name: String,
        val slug: String,
        val subtitle: String,
        val quality: String,
        val progressPercent: Int?,
        val deepLinkUri: String,
        val posterBitmap: Bitmap?
    )

    private data class LocalHistoryRecord(
        val slug: String,
        val name: String,
        val poster: String,
        val episodeIndex: Int,
        val episodeName: String,
        val currentTime: Double,
        val duration: Double
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_MODE) {
            val rawMode = intent.getStringExtra(EXTRA_WIDGET_MODE)
            val mode = runCatching { WidgetMode.valueOf(rawMode.orEmpty()) }.getOrNull()
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val manager = AppWidgetManager.getInstance(context)
            val targetIds = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                intArrayOf(widgetId)
            } else {
                manager.getAppWidgetIds(ComponentName(context, ManiFlixWidgetProvider::class.java))
            }
            if (mode != null) {
                val widgetPrefs = context.applicationContext.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                val editor = widgetPrefs.edit()
                for (id in targetIds) {
                    editor.putString(KEY_MODE_PREFIX + id, mode.name)
                }
                editor.apply()
            }
            if (targetIds.isNotEmpty()) {
                onUpdate(context, manager, targetIds)
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        executor.execute {
            try {
                val appCtx = context.applicationContext
                val statePrefs = appCtx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
                val widgetPrefs = appCtx.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)

                val blockedSlugs = loadBlockedSlugs(statePrefs.getString(KEY_BLOCKED, null))
                val historyList = loadLocalHistory(statePrefs.getString(KEY_LOCAL_HISTORY, null))
                    .filter { it.slug !in blockedSlugs }
                val favoriteNames = loadFavoriteNames(statePrefs.getString(KEY_LOCAL_FAVORITES, null))
                val likedSlugs = statePrefs.getStringSet(KEY_LIKED, emptySet())?.toSet() ?: emptySet()

                val cachedContinue by lazy { buildContinueMovies(historyList) }
                val cachedForYou by lazy {
                    fetchForYouMovies(
                        limit = 3,
                        blockedSlugs = blockedSlugs,
                        historyList = historyList,
                        favoriteNames = favoriteNames,
                        likedSlugs = likedSlugs
                    )
                }
                val cachedLatest by lazy { fetchLatestMovies(limit = 3, blockedSlugs = blockedSlugs) }

                for (appWidgetId in appWidgetIds) {
                    val savedMode = runCatching {
                        WidgetMode.valueOf(
                            widgetPrefs.getString(KEY_MODE_PREFIX + appWidgetId, WidgetMode.AUTO_SMART.name)
                                ?: WidgetMode.AUTO_SMART.name
                        )
                    }.getOrDefault(WidgetMode.AUTO_SMART)

                    val effectiveMode = when (savedMode) {
                        WidgetMode.AUTO_SMART -> if (historyList.isNotEmpty()) WidgetMode.CONTINUE else WidgetMode.FOR_YOU
                        else -> savedMode
                    }

                    val movies = when (effectiveMode) {
                        WidgetMode.CONTINUE -> cachedContinue.ifEmpty { cachedForYou.ifEmpty { cachedLatest } }
                        WidgetMode.FOR_YOU -> cachedForYou.ifEmpty { cachedLatest }
                        WidgetMode.LATEST -> cachedLatest
                        WidgetMode.AUTO_SMART -> cachedForYou.ifEmpty { cachedLatest }
                    }

                    val views = buildRemoteViews(
                        context = appCtx,
                        appWidgetId = appWidgetId,
                        appWidgetIds = appWidgetIds,
                        activeMode = effectiveMode,
                        movies = movies
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        appWidgetIds: IntArray,
        activeMode: WidgetMode,
        movies: List<WidgetMovie>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_maniflix)

        // Update 3 interactive tab pill styles + PendingIntents
        bindTabPill(
            context = context,
            views = views,
            viewId = R.id.widget_tab_continue,
            appWidgetId = appWidgetId,
            mode = WidgetMode.CONTINUE,
            active = activeMode == WidgetMode.CONTINUE,
            reqOffset = 1
        )
        bindTabPill(
            context = context,
            views = views,
            viewId = R.id.widget_tab_foryou,
            appWidgetId = appWidgetId,
            mode = WidgetMode.FOR_YOU,
            active = activeMode == WidgetMode.FOR_YOU,
            reqOffset = 2
        )
        bindTabPill(
            context = context,
            views = views,
            viewId = R.id.widget_tab_latest,
            appWidgetId = appWidgetId,
            mode = WidgetMode.LATEST,
            active = activeMode == WidgetMode.LATEST,
            reqOffset = 3
        )

        val refreshIntent = Intent(context, ManiFlixWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        val refreshPending = PendingIntent.getBroadcast(
            context,
            9001 + (appWidgetId % 500),
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)

        if (movies.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setViewVisibility(R.id.widget_row, View.GONE)
            views.setTextViewText(R.id.widget_empty, "Không tải được danh sách phim. Nhấn Làm mới.")
            return views
        }

        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setViewVisibility(R.id.widget_row, View.VISIBLE)

        val itemIds = intArrayOf(R.id.widget_item_1, R.id.widget_item_2, R.id.widget_item_3)
        val posterIds = intArrayOf(R.id.widget_poster_1, R.id.widget_poster_2, R.id.widget_poster_3)
        val progressIds = intArrayOf(R.id.widget_progress_1, R.id.widget_progress_2, R.id.widget_progress_3)
        val qualityIds = intArrayOf(R.id.widget_quality_1, R.id.widget_quality_2, R.id.widget_quality_3)
        val titleIds = intArrayOf(R.id.widget_title_1, R.id.widget_title_2, R.id.widget_title_3)
        val subIds = intArrayOf(R.id.widget_sub_1, R.id.widget_sub_2, R.id.widget_sub_3)

        for (i in 0 until 3) {
            val movie = movies.getOrNull(i)
            if (movie == null) {
                views.setViewVisibility(itemIds[i], View.INVISIBLE)
                continue
            }
            views.setViewVisibility(itemIds[i], View.VISIBLE)
            views.setTextViewText(titleIds[i], movie.name)
            views.setTextViewText(subIds[i], movie.subtitle)
            views.setTextViewText(qualityIds[i], movie.quality)

            if (movie.progressPercent != null) {
                views.setViewVisibility(progressIds[i], View.VISIBLE)
                views.setProgressBar(progressIds[i], 100, movie.progressPercent.coerceIn(0, 100), false)
            } else {
                views.setViewVisibility(progressIds[i], View.GONE)
            }

            if (movie.posterBitmap != null) {
                views.setImageViewBitmap(posterIds[i], movie.posterBitmap)
            } else {
                views.setImageViewResource(posterIds[i], R.drawable.widget_poster_bg)
            }

            val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(movie.deepLinkUri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPending = PendingIntent.getActivity(
                context,
                (appWidgetId * 10) + 100 + i,
                deepLinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(itemIds[i], clickPending)
        }

        return views
    }

    private fun bindTabPill(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        appWidgetId: Int,
        mode: WidgetMode,
        active: Boolean,
        reqOffset: Int
    ) {
        views.setInt(
            viewId,
            "setBackgroundResource",
            if (active) R.drawable.widget_pill_active else R.drawable.widget_pill_inactive
        )
        views.setTextColor(viewId, if (active) 0xFFFFFFFF.toInt() else 0xFFD1D1D6.toInt())

        val intent = Intent(context, ManiFlixWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_MODE
            data = Uri.parse("maniflixwidget://mode/$appWidgetId/${mode.name}")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_WIDGET_MODE, mode.name)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            8000 + (appWidgetId * 10) + reqOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pi)
    }

    private fun buildContinueMovies(historyList: List<LocalHistoryRecord>): List<WidgetMovie> {
        if (historyList.isEmpty()) return emptyList()
        return historyList.take(3).map { entry ->
            val pct = if (entry.duration > 1.0) {
                ((entry.currentTime / entry.duration) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                0
            }
            val epLabel = entry.episodeName.ifBlank { "Tập ${entry.episodeIndex + 1}" }
            val resolvedImg = ImageUrlResolver.resolve(entry.poster)
            val bmp = if (resolvedImg.isNotBlank()) downloadBitmap(resolvedImg) else null
            WidgetMovie(
                name = entry.name.ifBlank { entry.slug },
                slug = entry.slug,
                subtitle = "$epLabel · $pct%",
                quality = "Đang xem",
                progressPercent = pct,
                deepLinkUri = "maniflix://movie/${Uri.encode(entry.slug)}?resume=1",
                posterBitmap = bmp
            )
        }
    }

    private fun fetchLatestMovies(limit: Int, blockedSlugs: Set<String>): List<WidgetMovie> {
        val rawItems = fetchMovieJsonItems("${AppPreferences.BASE_URL}/api/movies?page=1")
        val out = ArrayList<WidgetMovie>(limit)
        for (m in rawItems) {
            if (out.size >= limit) break
            val slug = m.optString("slug", "").trim()
            val name = m.optString("name", "").trim()
            if (slug.isEmpty() || name.isEmpty() || slug in blockedSlugs) continue
            val epCur = m.optString("episode_current", "").trim()
            val origin = m.optString("origin_name", "").trim()
            val quality = m.optString("quality", "").ifBlank { "HD" }
            val rawImg = m.optString("poster_url", "").ifBlank { m.optString("thumb_url", "") }
            val resolvedImg = ImageUrlResolver.resolve(rawImg)
            val bmp = if (resolvedImg.isNotBlank()) downloadBitmap(resolvedImg) else null
            out.add(
                WidgetMovie(
                    name = name,
                    slug = slug,
                    subtitle = epCur.ifBlank { origin.ifBlank { "Mới cập nhật" } },
                    quality = quality,
                    progressPercent = null,
                    deepLinkUri = "maniflix://movie/${Uri.encode(slug)}",
                    posterBitmap = bmp
                )
            )
        }
        return out
    }

    private fun fetchForYouMovies(
        limit: Int,
        blockedSlugs: Set<String>,
        historyList: List<LocalHistoryRecord>,
        favoriteNames: List<String>,
        likedSlugs: Set<String>
    ): List<WidgetMovie> {
        val watchedSlugs = historyList.mapTo(HashSet()) { it.slug }
        val tasteTokens = HashSet<String>()
        (historyList.map { it.name } + favoriteNames + likedSlugs.map { it.replace("-", " ") })
            .forEach { raw ->
                raw.lowercase(Locale.ROOT)
                    .split(Regex("[^\\p{L}0-9]+"))
                    .filter { it.length >= 2 }
                    .forEach { tasteTokens.add(it) }
            }

        val endpoints = listOf(
            "${AppPreferences.BASE_URL}/api/movies/category/phim-bo?page=1",
            "${AppPreferences.BASE_URL}/api/movies/category/hoat-hinh?page=1",
            "${AppPreferences.BASE_URL}/api/movies?page=2"
        )
        val seenSlugs = HashSet<String>()
        val candidates = ArrayList<Pair<Int, JSONObject>>()

        for (endpoint in endpoints) {
            for (m in fetchMovieJsonItems(endpoint)) {
                val slug = m.optString("slug", "").trim()
                val name = m.optString("name", "").trim()
                if (slug.isEmpty() || name.isEmpty() || slug in blockedSlugs || slug in watchedSlugs) continue
                if (!seenSlugs.add(slug)) continue

                var score = 0
                val origin = m.optString("origin_name", "")
                val year = m.optInt("year", 0)
                val quality = m.optString("quality", "").ifBlank { "HD" }
                val haystack = "$name $origin ${slug.replace('-', ' ')}".lowercase(Locale.ROOT)
                for (tok in tasteTokens) {
                    if (haystack.contains(tok)) score += 3
                }
                if (year >= 2024) score += 2
                if (quality.contains("FHD", ignoreCase = true) ||
                    quality.contains("4K", ignoreCase = true) ||
                    quality.contains("HD", ignoreCase = true)
                ) {
                    score += 1
                }
                candidates.add(score to m)
            }
        }

        val ranked = candidates.sortedByDescending { it.first }.take(limit)
        return ranked.map { (_, m) ->
            val slug = m.optString("slug", "").trim()
            val name = m.optString("name", "").trim()
            val epCur = m.optString("episode_current", "").trim()
            val origin = m.optString("origin_name", "").trim()
            val quality = m.optString("quality", "").ifBlank { "HD" }
            val rawImg = m.optString("poster_url", "").ifBlank { m.optString("thumb_url", "") }
            val resolvedImg = ImageUrlResolver.resolve(rawImg)
            val bmp = if (resolvedImg.isNotBlank()) downloadBitmap(resolvedImg) else null
            WidgetMovie(
                name = name,
                slug = slug,
                subtitle = epCur.ifBlank { origin.ifBlank { "Đề xuất cho bạn" } },
                quality = quality,
                progressPercent = null,
                deepLinkUri = "maniflix://movie/${Uri.encode(slug)}",
                posterBitmap = bmp
            )
        }
    }

    private fun fetchMovieJsonItems(endpointUrl: String): List<JSONObject> {
        return runCatching {
            val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9_000
                readTimeout = 9_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ManiFlixWidget-Android/1.1.2")
            }
            val text = try {
                if (conn.responseCode !in 200..299) return emptyList()
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val arr = JSONObject(text).optJSONArray("items") ?: return emptyList()
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadBlockedSlugs(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    val slug = arr.optJSONObject(i)?.optString("slug", "").orEmpty().trim()
                    if (slug.isNotEmpty()) add(slug)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun loadFavoriteNames(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    if (name.isNotEmpty()) add(name)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadLocalHistory(raw: String?): List<LocalHistoryRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val slug = obj.optString("slug", "").trim()
                    if (slug.isEmpty()) continue
                    add(
                        LocalHistoryRecord(
                            slug = slug,
                            name = obj.optString("name", slug),
                            poster = obj.optString("poster", ""),
                            episodeIndex = obj.optInt("episodeIndex", 0),
                            episodeName = obj.optString("episodeName", ""),
                            currentTime = obj.optDouble("currentTime", 0.0),
                            duration = obj.optDouble("duration", 0.0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun downloadBitmap(imageUrl: String): Bitmap? {
        return runCatching {
            val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "ManiFlixWidget-Android/1.1.2")
            }
            try {
                if (conn.responseCode !in 200..299) return null
                val bytes = conn.inputStream.use { it.readBytes() }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 240 || bounds.outHeight / sample > 360) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        private const val ACTION_WIDGET_MODE = "bond.maniakov.maniflix.ACTION_WIDGET_MODE"
        private const val EXTRA_WIDGET_MODE = "widget_mode"
        private const val WIDGET_PREFS = "maniflix_widget_prefs"
        private const val KEY_MODE_PREFIX = "pref_widget_mode_"

        private const val STATE_PREFS = "maniflix_state"
        private const val KEY_BLOCKED = "prefs.blockedMovies.v1"
        private const val KEY_LIKED = "prefs.likedSlugs.v1"
        private const val KEY_LOCAL_HISTORY = "prefs.localHistory.v1"
        private const val KEY_LOCAL_FAVORITES = "prefs.localFavorites.v1"

        private val executor = Executors.newSingleThreadExecutor()

        fun requestRefresh(context: Context) {
            val appCtx = context.applicationContext
            val manager = AppWidgetManager.getInstance(appCtx)
            val component = ComponentName(appCtx, ManiFlixWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(appCtx, ManiFlixWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appCtx.sendBroadcast(intent)
            }
        }
    }
}
