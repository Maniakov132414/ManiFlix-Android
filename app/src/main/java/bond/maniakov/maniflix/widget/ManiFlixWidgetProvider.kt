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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ManiFlixWidgetProvider : AppWidgetProvider() {

    private data class WidgetMovie(
        val name: String,
        val slug: String,
        val subtitle: String,
        val quality: String,
        val posterBitmap: Bitmap?
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        executor.execute {
            try {
                val movies = fetchLatestMovies(limit = 3)
                for (appWidgetId in appWidgetIds) {
                    val views = buildRemoteViews(context, appWidgetIds, movies)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetIds: IntArray,
        movies: List<WidgetMovie>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_maniflix)

        val refreshIntent = Intent(context, ManiFlixWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        val refreshPending = PendingIntent.getBroadcast(
            context,
            9001,
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

            if (movie.posterBitmap != null) {
                views.setImageViewBitmap(posterIds[i], movie.posterBitmap)
            } else {
                views.setImageViewResource(posterIds[i], R.drawable.widget_poster_bg)
            }

            val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("maniflix://movie/${Uri.encode(movie.slug)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPending = PendingIntent.getActivity(
                context,
                100 + i,
                deepLinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(itemIds[i], clickPending)
        }

        return views
    }

    private fun fetchLatestMovies(limit: Int): List<WidgetMovie> {
        return runCatching {
            val url = URL("${AppPreferences.BASE_URL}/api/movies?page=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ManiFlixWidget-Android/1.0")
            }
            val text = try {
                if (conn.responseCode !in 200..299) return emptyList()
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } finally {
                conn.disconnect()
            }

            val items = JSONObject(text).optJSONArray("items") ?: return emptyList()
            val out = ArrayList<WidgetMovie>(limit)
            val count = minOf(limit, items.length())
            for (i in 0 until count) {
                val m = items.optJSONObject(i) ?: continue
                val slug = m.optString("slug", "").trim()
                val name = m.optString("name", "").trim()
                if (slug.isEmpty() || name.isEmpty()) continue
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
                        posterBitmap = bmp
                    )
                )
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun downloadBitmap(imageUrl: String): Bitmap? {
        return runCatching {
            val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "ManiFlixWidget-Android/1.0")
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
        private val executor = Executors.newSingleThreadExecutor()

        fun requestRefresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ManiFlixWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, ManiFlixWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
