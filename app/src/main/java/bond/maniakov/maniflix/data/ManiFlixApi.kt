package bond.maniakov.maniflix.data

import bond.maniakov.maniflix.model.AdMarker
import bond.maniakov.maniflix.model.AndroidVersionConfig
import bond.maniakov.maniflix.model.AppVariantInfo
import bond.maniakov.maniflix.model.AuthResult
import bond.maniakov.maniflix.model.EpisodeItem
import bond.maniakov.maniflix.model.EpisodeServer
import bond.maniakov.maniflix.model.FavoriteEntry
import bond.maniakov.maniflix.model.FranchiseSeason
import bond.maniakov.maniflix.model.HistoryEntry
import bond.maniakov.maniflix.model.MovieDetail
import bond.maniakov.maniflix.model.MovieDetailResult
import bond.maniakov.maniflix.model.MovieItem
import bond.maniakov.maniflix.model.MovieTag
import bond.maniakov.maniflix.model.TitleLocalizer
import bond.maniakov.maniflix.model.UserInfo
import bond.maniakov.maniflix.model.UserThemeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class ApiHttpException(val statusCode: Int, override val message: String) : Exception(message)

data class SpeedtestPingResult(
    val pingMs: Int,
    val jitterMs: Int
)

object ManiFlixApi {
    private const val CONNECT_TIMEOUT_MS = 18_000
    private const val READ_TIMEOUT_MS = 18_000
    private const val USER_AGENT = "ManiFlix-Android/2.0.0"

    private const val CF_PING_URL = "https://speed.cloudflare.com/__down?bytes=0"
    private const val CF_DOWN_5MB_URL = "https://speed.cloudflare.com/__down?bytes=5000000"
    private const val CF_DOWN_1MB_URL = "https://speed.cloudflare.com/__down?bytes=1000000"
    private const val CF_UP_URL = "https://speed.cloudflare.com/__up"

    private val speedtestClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 2, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun measureSpeedtestPingAndJitter(
        samples: Int = 4,
        includeWarmup: Boolean = true
    ): SpeedtestPingResult? = withContext(Dispatchers.IO) {
        fun probeUrl(targetUrl: String): List<Int> {
            if (includeWarmup) {
                runCatching {
                    val warmReq = Request.Builder()
                        .url(targetUrl)
                        .header("Cache-Control", "no-store")
                        .header("User-Agent", USER_AGENT)
                        .get()
                        .build()
                    speedtestClient.newCall(warmReq).execute().use { resp ->
                        resp.body?.source()?.readByteString()
                    }
                }
            }
            val list = ArrayList<Int>(samples.coerceAtLeast(1))
            repeat(samples.coerceAtLeast(1)) {
                runCatching {
                    val req = Request.Builder()
                        .url(targetUrl)
                        .header("Cache-Control", "no-store")
                        .header("User-Agent", USER_AGENT)
                        .get()
                        .build()
                    val t0 = System.nanoTime()
                    speedtestClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.source()?.readByteString()
                            val dtMs = ((System.nanoTime() - t0) / 1_000_000L).toInt().coerceAtLeast(1)
                            list.add(dtMs)
                        }
                    }
                }
            }
            return list
        }

        var pings = probeUrl(CF_PING_URL)
        if (pings.isEmpty()) {
            pings = probeUrl("${AppPreferences.BASE_URL}/api/net-probe?kb=1")
        }
        if (pings.isEmpty()) return@withContext null

        val avgPing = (pings.sum().toDouble() / pings.size).roundToInt().coerceAtLeast(1)
        val jitter = if (pings.size >= 2) {
            var diffSum = 0
            for (i in 1 until pings.size) {
                diffSum += abs(pings[i] - pings[i - 1])
            }
            diffSum / (pings.size - 1)
        } else {
            0
        }
        SpeedtestPingResult(pingMs = avgPing, jitterMs = jitter)
    }

    suspend fun measureSpeedtestDownloadMbps(
        streams: Int = 4,
        bytesPerStream: Long = 5_000_000L,
        fallbackKb: Int = 2048
    ): Double {
        val cfUrl = when (bytesPerStream) {
            5_000_000L -> CF_DOWN_5MB_URL
            1_000_000L -> CF_DOWN_1MB_URL
            else -> "https://speed.cloudflare.com/__down?bytes=$bytesPerStream"
        }
        val cfMbps = runParallelDownloadOkHttp(cfUrl, streams)
        if (cfMbps > 0.0) return cfMbps

        return runParallelDownloadOkHttp(
            "${AppPreferences.BASE_URL}/api/net-probe?kb=$fallbackKb",
            streams
        )
    }

    private suspend fun runParallelDownloadOkHttp(url: String, streams: Int): Double = coroutineScope {
        val count = streams.coerceAtLeast(1)
        val results = (0 until count).map {
            async(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder()
                        .url(url)
                        .header("Cache-Control", "no-store")
                        .header("User-Agent", USER_AGENT)
                        .get()
                        .build()
                    speedtestClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@runCatching null
                        val body = resp.body ?: return@runCatching null
                        val source = body.source()
                        val buffer = Buffer()
                        val tStart = System.nanoTime()
                        var totalBytes = 0L
                        while (true) {
                            val read = source.read(buffer, 65_536L)
                            if (read == -1L) break
                            totalBytes += read
                            buffer.clear()
                        }
                        val tEnd = System.nanoTime()
                        if (totalBytes <= 0L) null else Triple(tStart, tEnd, totalBytes)
                    }
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()

        if (results.isEmpty()) return@coroutineScope 0.0
        val totalBytes = results.sumOf { it.third }
        if (totalBytes <= 0L) return@coroutineScope 0.0
        val minStart = results.minOf { it.first }
        val maxEnd = results.maxOf { it.second }
        val maxStreamNs = results.maxOf { it.second - it.first }
        val dtSec = max(0.005, max(maxEnd - minStart, maxStreamNs) / 1_000_000_000.0)
        (totalBytes * 8.0) / dtSec / 1_000_000.0
    }

    suspend fun measureSpeedtestUploadMbps(streams: Int = 2): Double {
        val cfMbps = runParallelUploadOkHttp(CF_UP_URL, payloadBytes = 1_000_000, streams = streams)
        if (cfMbps > 0.0) return cfMbps
        return runParallelUploadOkHttp(
            "${AppPreferences.BASE_URL}/api/net-up",
            payloadBytes = 512 * 1024,
            streams = streams
        )
    }

    private suspend fun runParallelUploadOkHttp(
        url: String,
        payloadBytes: Int,
        streams: Int
    ): Double = coroutineScope {
        val count = streams.coerceAtLeast(1)
        val payload = ByteArray(payloadBytes) { idx -> ((idx * 29) and 0xFF).toByte() }
        val mediaType = "application/octet-stream".toMediaType()
        val t0 = System.nanoTime()
        val uploadedList = (0 until count).map {
            async(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder()
                        .url(url)
                        .header("Cache-Control", "no-store")
                        .header("User-Agent", USER_AGENT)
                        .post(payload.toRequestBody(mediaType))
                        .build()
                    speedtestClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@runCatching 0L
                        resp.body?.source()?.readByteString()
                        payloadBytes.toLong()
                    }
                }.getOrDefault(0L)
            }
        }.awaitAll()
        val totalUploaded = uploadedList.sum()
        if (totalUploaded <= 0L) return@coroutineScope 0.0
        val dtSec = max(0.005, (System.nanoTime() - t0) / 1_000_000_000.0)
        (totalUploaded * 8.0) / dtSec / 1_000_000.0
    }

    fun cleanM3u8Url(rawM3u8: String, extraParams: Map<String, String> = emptyMap()): String {
        val trimmed = rawM3u8.trim()
        if (trimmed.isEmpty()) return ""
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val sub = URLEncoder.encode(AppPreferences.defaultSubtitleLang, "UTF-8")
        val sb = StringBuilder("${AppPreferences.BASE_URL}/api/clean-m3u8?url=$encoded&sub=$sub")
        for ((k, v) in extraParams) {
            sb.append("&").append(URLEncoder.encode(k, "UTF-8")).append("=").append(URLEncoder.encode(v, "UTF-8"))
        }
        return sb.toString()
    }

    suspend fun fetchMovies(page: Int = 1): List<MovieItem> {
        val obj = requestJsonObject("/api/movies?page=$page")
        return parseMovieList(obj.optJSONArray("items"))
    }

    private val LIST_CATEGORY_SLUGS = setOf(
        "phim-bo",
        "phim-le",
        "hoat-hinh",
        "tv-shows",
        "phim-chieu-rap",
        "phim-sap-chieu",
        "phim-thuyet-minh",
        "phim-long-tieng",
        "phim-vietsub",
        "phim-bo-dang-chieu",
        "phim-bo-hoan-thanh"
    )

    suspend fun fetchCategory(type: String, page: Int = 1): List<MovieItem> {
        val normalized = type.trim().lowercase()
        val encoded = encodeQuery(normalized)
        val path = when {
            normalized.startsWith("genre:") ->
                "/api/category?genre=${encodeQuery(normalized.removePrefix("genre:"))}&page=$page"
            normalized.startsWith("country:") ->
                "/api/category?country=${encodeQuery(normalized.removePrefix("country:"))}&page=$page"
            normalized.startsWith("year:") ->
                "/api/category?year=${encodeQuery(normalized.removePrefix("year:"))}&page=$page"
            normalized in LIST_CATEGORY_SLUGS ->
                "/api/category?type=$encoded&page=$page"
            else ->
                "/api/category?genre=$encoded&page=$page"
        }
        val obj = requestJsonObject(path)
        return parseMovieList(obj.optJSONArray("items"))
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<MovieItem> {
        val encodedQuery = encodeQuery(query)
        val obj = requestJsonObject("/api/search?q=$encodedQuery&page=$page")
        return parseMovieList(obj.optJSONArray("items"))
    }

    suspend fun fetchMovieDetail(slug: String): MovieDetailResult {
        val encodedSlug = encodePathSegment(slug)
        val obj = requestJsonObject("/api/movie/$encodedSlug")
        val movieObj = obj.optJSONObject("movie")
        val movie = movieObj?.let { parseMovieDetail(it) }
        val episodes = parseEpisodeServers(obj.optJSONArray("episodes"))
        return MovieDetailResult(movie = movie, episodes = episodes)
    }

    suspend fun fetchFranchise(slug: String, title: String = ""): List<FranchiseSeason> {
        val sb = StringBuilder("/api/franchise?slug=${encodeQuery(slug)}")
        if (title.isNotBlank()) {
            sb.append("&title=${encodeQuery(title)}")
        }
        val obj = requestJsonObject(sb.toString())
        val baseTitle = obj.optString("baseTitle", "")
        val baseTitleVi = obj.optString("baseTitleVi", "")
        val baseTitleEn = obj.optString("baseTitleEn", "")
        val baseTitleRomaji = obj.optString("baseTitleRomaji", "")
        val arr = obj.optJSONArray("seasons") ?: return emptyList()
        val result = ArrayList<FranchiseSeason>(arr.length())
        for (i in 0 until arr.length()) {
            val s = arr.optJSONObject(i) ?: continue
            val sSlug = s.optString("slug", "").trim()
            if (sSlug.isEmpty()) continue
            result.add(
                FranchiseSeason(
                    sortIndex = parseFlexInt(s.opt("sortIndex"), -1),
                    name = s.optString("name", ""),
                    nameVi = s.optString("nameVi", ""),
                    nameEn = s.optString("nameEn", ""),
                    fullTitle = s.optString("fullTitle", ""),
                    originName = s.optString("originName", ""),
                    slug = sSlug,
                    year = parseFlexInt(s.opt("year")),
                    seasonNumber = parseFlexInt(s.opt("seasonNumber")),
                    poster = s.optString("poster", ""),
                    quality = s.optString("quality", ""),
                    lang = s.optString("lang", ""),
                    isRecap = s.optBoolean("isRecap", false),
                    episodeCurrent = s.optString("episodeCurrent", ""),
                    baseTitle = baseTitle,
                    baseTitleVi = baseTitleVi,
                    baseTitleEn = baseTitleEn,
                    baseTitleRomaji = baseTitleRomaji
                )
            )
        }
        return TitleLocalizer.sortFranchiseSeasons(result, baseTitle)
    }

    suspend fun fetchAdsMarkers(slug: String, ep: String, titleHint: String = ""): List<AdMarker> {
        val digitsOnly = ep.replace(Regex("[^0-9]"), "")
        val cleanEp = digitsOnly.toIntOrNull()?.toString() ?: ep.ifBlank { "1" }
        val path = StringBuilder("/api/ads/${encodePathSegment(slug)}/${encodePathSegment(cleanEp)}")
        if (titleHint.isNotBlank()) {
            path.append("?title=${encodeQuery(titleHint)}")
        }
        val obj = requestJsonObject(path.toString())
        val arr = obj.optJSONArray("markers") ?: return emptyList()
        val result = ArrayList<AdMarker>(arr.length())
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            val type = m.optString("type", "")
            val start = parseFlexDouble(m.opt("start"))
            val end = parseFlexDouble(m.opt("end"))
            val id = m.optString("id", "").ifBlank { "${type.ifBlank { "mark" }}-$start-$end" }
            result.add(
                AdMarker(
                    id = id,
                    type = type,
                    mode = m.optString("mode", ""),
                    start = start,
                    end = end,
                    openEnd = m.optBoolean("openEnd", false),
                    label = m.optString("label", ""),
                    source = m.optString("source", "")
                )
            )
        }
        return result
    }

    suspend fun login(username: String, password: String): AuthResult {
        val body = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        return executeAuthRequest("/api/auth/login", body)
    }

    suspend fun register(username: String, password: String, displayName: String): AuthResult {
        val body = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("displayName", displayName)
        }
        return executeAuthRequest("/api/auth/register", body)
    }

    suspend fun fetchMe(token: String): UserInfo? {
        val obj = requestJsonObject("/api/auth/me", token = token)
        return obj.optJSONObject("user")?.let { parseUserInfo(it) }
    }

    suspend fun fetchHistory(token: String): List<HistoryEntry> {
        val obj = requestJsonObject("/api/user/history", token = token)
        return parseHistoryList(obj.optJSONArray("history"))
    }

    suspend fun saveHistory(
        token: String,
        slug: String,
        name: String,
        poster: String,
        episodeIndex: Int,
        episodeName: String,
        currentTime: Double,
        duration: Double
    ) {
        val body = JSONObject().apply {
            put("slug", slug)
            put("name", name)
            put("poster", poster)
            put("episodeIndex", episodeIndex)
            put("episodeName", episodeName)
            put("currentTime", currentTime)
            put("duration", duration)
        }
        runCatching {
            requestJsonObject("/api/user/history", method = "POST", body = body, token = token)
        }
    }

    suspend fun deleteHistory(token: String, slug: String) {
        val body = JSONObject().apply { put("slug", slug) }
        runCatching {
            requestJsonObject("/api/user/history/delete", method = "POST", body = body, token = token)
        }
    }

    suspend fun fetchFavorites(token: String): List<FavoriteEntry> {
        val obj = requestJsonObject("/api/user/favorites", token = token)
        return parseFavoriteList(obj.optJSONArray("favorites"))
    }

    suspend fun toggleFavorite(token: String, movie: MovieDetail): List<FavoriteEntry>? {
        val body = JSONObject().apply {
            put("slug", movie.slug)
            put("name", movie.name)
            put("poster", movie.posterUrl.ifBlank { movie.thumbUrl })
            put("year", movie.year)
            put("quality", movie.quality.ifBlank { "HD" })
            put("lang", movie.lang.ifBlank { "Vietsub" })
        }
        val obj = requestJsonObject("/api/user/favorites/toggle", method = "POST", body = body, token = token)
        val arr = obj.optJSONArray("favorites") ?: return null
        return parseFavoriteList(arr)
    }

    suspend fun toggleFavoriteItem(token: String, item: MovieItem): List<FavoriteEntry>? {
        val body = JSONObject().apply {
            put("slug", item.slug)
            put("name", item.name)
            put("poster", item.posterUrl.ifBlank { item.thumbUrl })
            put("year", item.year)
            put("quality", item.quality.ifBlank { "HD" })
            put("lang", item.lang.ifBlank { "Vietsub" })
        }
        val obj = requestJsonObject("/api/user/favorites/toggle", method = "POST", body = body, token = token)
        val arr = obj.optJSONArray("favorites") ?: return null
        return parseFavoriteList(arr)
    }

    suspend fun saveUserTheme(
        token: String,
        presetId: String,
        accentHex: String,
        bgHex: String,
        surfaceHex: String
    ): UserThemeConfig? {
        val themeObj = JSONObject().apply {
            put("presetId", presetId)
            put("accent", accentHex)
            put("accent2", accentHex)
            put("bg", bgHex)
            put("surface", surfaceHex)
        }
        val body = JSONObject().apply {
            put("themeConfig", themeObj)
        }
        val obj = requestJsonObject("/api/user/theme", method = "POST", body = body, token = token)
        return obj.optJSONObject("themeConfig")?.let { parseThemeConfig(it) }
    }

    suspend fun fetchAndroidVersion(): AndroidVersionConfig {
        val obj = requestJsonObject("/api/android/version")
        val variantsArr = obj.optJSONArray("variants")
        val variants = ArrayList<AppVariantInfo>()
        if (variantsArr != null) {
            for (i in 0 until variantsArr.length()) {
                val v = variantsArr.optJSONObject(i) ?: continue
                variants.add(
                    AppVariantInfo(
                        key = v.optString("key", ""),
                        name = v.optString("name", ""),
                        arch = v.optString("arch", ""),
                        minSdk = parseFlexInt(v.opt("minSdk"), 24),
                        downloadUrl = v.optString("downloadUrl", "")
                    )
                )
            }
        }
        return AndroidVersionConfig(
            latestVersion = obj.optString("latestVersion", "1.0.0"),
            minRequiredVersion = obj.optString("minRequiredVersion", "1.0.0"),
            mode = obj.optString("mode", "soft"),
            forceUpdate = obj.optBoolean("forceUpdate", false),
            title = obj.optString("title", ""),
            releaseNotes = obj.optString("releaseNotes", ""),
            downloadUrl = obj.optString("downloadUrl", ""),
            releasePageUrl = obj.optString("releasePageUrl", ""),
            variants = variants
        )
    }

    private suspend fun executeAuthRequest(path: String, body: JSONObject): AuthResult {
        return try {
            val obj = requestJsonObject(path, method = "POST", body = body)
            val token = obj.optString("token", "")
            val user = obj.optJSONObject("user")?.let { parseUserInfo(it) }
            val error = obj.optString("error", "")
            val success = obj.optBoolean("success", token.isNotBlank())
            AuthResult(
                success = success && token.isNotBlank(),
                token = token,
                user = user,
                error = error
            )
        } catch (e: ApiHttpException) {
            val msg = runCatching {
                JSONObject(e.message).optString("error", e.message)
            }.getOrDefault(e.message)
            AuthResult(success = false, error = msg.ifBlank { "Lỗi HTTP ${e.statusCode}" })
        } catch (e: Exception) {
            AuthResult(success = false, error = e.message ?: "Lỗi kết nối máy chủ")
        }
    }

    private suspend fun requestJsonObject(
        pathAndQuery: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val url = URL("${AppPreferences.BASE_URL}$pathAndQuery")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = stream?.let { s ->
                BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (code !in 200..299) {
                throw ApiHttpException(code, text.ifBlank { "HTTP $code" })
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseMovieList(arr: JSONArray?): List<MovieItem> {
        if (arr == null) return emptyList()
        val list = ArrayList<MovieItem>(arr.length())
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            val slug = m.optString("slug", "").trim()
            if (slug.isEmpty()) continue
            list.add(
                MovieItem(
                    id = m.optString("_id", "").ifBlank { slug },
                    name = m.optString("name", slug),
                    slug = slug,
                    originName = m.optString("origin_name", ""),
                    posterUrl = m.optString("poster_url", ""),
                    thumbUrl = m.optString("thumb_url", ""),
                    year = parseFlexInt(m.opt("year")),
                    type = m.optString("type", ""),
                    quality = m.optString("quality", ""),
                    lang = m.optString("lang", ""),
                    subLangs = parseStringList(m.opt("sub_langs")),
                    viName = m.optString("vi_name", ""),
                    enName = m.optString("en_name", ""),
                    romajiName = m.optString("romaji_name", ""),
                    episodeCurrent = m.optString("episode_current", ""),
                    time = m.optString("time", ""),
                    category = parseTagList(m.optJSONArray("category")),
                    country = parseTagList(m.optJSONArray("country"))
                )
            )
        }
        return list
    }

    private fun parseMovieDetail(m: JSONObject): MovieDetail {
        val slug = m.optString("slug", "")
        return MovieDetail(
            id = m.optString("_id", "").ifBlank { slug },
            name = m.optString("name", slug),
            slug = slug,
            originName = m.optString("origin_name", ""),
            content = m.optString("content", ""),
            type = m.optString("type", ""),
            status = m.optString("status", ""),
            posterUrl = m.optString("poster_url", ""),
            thumbUrl = m.optString("thumb_url", ""),
            trailerUrl = m.optString("trailer_url", ""),
            time = m.optString("time", ""),
            episodeCurrent = m.optString("episode_current", ""),
            episodeTotal = parseFlexString(m.opt("episode_total")),
            quality = m.optString("quality", ""),
            lang = m.optString("lang", ""),
            subLangs = parseStringList(m.opt("sub_langs")),
            viName = m.optString("vi_name", ""),
            enName = m.optString("en_name", ""),
            romajiName = m.optString("romaji_name", ""),
            year = parseFlexInt(m.opt("year")),
            actor = parseStringList(m.opt("actor")),
            director = parseStringList(m.opt("director")),
            category = parseTagList(m.optJSONArray("category")),
            country = parseTagList(m.optJSONArray("country"))
        )
    }

    private fun parseEpisodeServers(arr: JSONArray?): List<EpisodeServer> {
        if (arr == null) return emptyList()
        val servers = ArrayList<EpisodeServer>(arr.length())
        for (i in 0 until arr.length()) {
            val s = arr.optJSONObject(i) ?: continue
            val dataArr = s.optJSONArray("server_data")
            val episodes = ArrayList<EpisodeItem>()
            if (dataArr != null) {
                for (j in 0 until dataArr.length()) {
                    val ep = dataArr.optJSONObject(j) ?: continue
                    episodes.add(
                        EpisodeItem(
                            name = ep.optString("name", ""),
                            slug = ep.optString("slug", ""),
                            filename = ep.optString("filename", ""),
                            linkEmbed = ep.optString("link_embed", ""),
                            linkM3u8 = ep.optString("link_m3u8", "")
                        )
                    )
                }
            }
            servers.add(
                EpisodeServer(
                    serverName = s.optString("server_name", "Server ${i + 1}"),
                    isAi = s.optBoolean("is_ai", false),
                    serverData = episodes
                )
            )
        }
        return servers
    }

    private fun parseTagList(arr: JSONArray?): List<MovieTag> {
        if (arr == null) return emptyList()
        val list = ArrayList<MovieTag>(arr.length())
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            val slug = t.optString("slug", "")
            val name = t.optString("name", "")
            val id = parseFlexString(t.opt("id")).ifBlank { slug.ifBlank { name } }
            list.add(MovieTag(id = id, name = name, slug = slug))
        }
        return list
    }

    private fun parseStringList(raw: Any?): List<String> {
        return when (raw) {
            is JSONArray -> buildList {
                for (i in 0 until raw.length()) {
                    val v = raw.optString(i, "").trim()
                    if (v.isNotEmpty() && v != "null") add(v)
                }
            }
            is String -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    private fun parseUserInfo(u: JSONObject): UserInfo {
        return UserInfo(
            username = u.optString("username", ""),
            displayName = u.optString("displayName", ""),
            favoritesCount = parseFlexInt(u.opt("favoritesCount")),
            historyCount = parseFlexInt(u.opt("historyCount")),
            themeConfig = u.optJSONObject("themeConfig")?.let { parseThemeConfig(it) }
        )
    }

    private fun parseThemeConfig(t: JSONObject): UserThemeConfig {
        return UserThemeConfig(
            presetId = t.optString("presetId", ""),
            accent = t.optString("accent", ""),
            accent2 = t.optString("accent2", ""),
            bg = t.optString("bg", ""),
            surface = t.optString("surface", "")
        )
    }

    private fun parseHistoryList(arr: JSONArray?): List<HistoryEntry> {
        if (arr == null) return emptyList()
        val list = ArrayList<HistoryEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val h = arr.optJSONObject(i) ?: continue
            val slug = h.optString("slug", "").trim()
            if (slug.isEmpty()) continue
            list.add(
                HistoryEntry(
                    slug = slug,
                    name = h.optString("name", slug),
                    poster = h.optString("poster", ""),
                    episodeIndex = parseFlexInt(h.opt("episodeIndex")),
                    episodeName = h.optString("episodeName", ""),
                    currentTime = parseFlexDouble(h.opt("currentTime")),
                    duration = parseFlexDouble(h.opt("duration")),
                    updatedAt = parseFlexLong(h.opt("updatedAt"))
                )
            )
        }
        return list
    }

    private fun parseFavoriteList(arr: JSONArray?): List<FavoriteEntry> {
        if (arr == null) return emptyList()
        val list = ArrayList<FavoriteEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val slug = f.optString("slug", "").trim()
            if (slug.isEmpty()) continue
            list.add(
                FavoriteEntry(
                    slug = slug,
                    name = f.optString("name", slug),
                    poster = f.optString("poster", ""),
                    year = parseFlexInt(f.opt("year")),
                    quality = f.optString("quality", ""),
                    lang = f.optString("lang", ""),
                    addedAt = parseFlexLong(f.opt("addedAt"))
                )
            )
        }
        return list
    }

    private fun parseFlexString(raw: Any?): String {
        return when (raw) {
            null, JSONObject.NULL -> ""
            is String -> raw
            is Number -> raw.toString()
            else -> raw.toString()
        }
    }

    private fun parseFlexInt(raw: Any?, fallback: Int = 0): Int {
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun parseFlexLong(raw: Any?, fallback: Long = 0L): Long {
        return when (raw) {
            is Number -> raw.toLong()
            is String -> raw.trim().toLongOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun parseFlexDouble(raw: Any?, fallback: Double = 0.0): Double {
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private fun encodePathSegment(segment: String): String =
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
}
