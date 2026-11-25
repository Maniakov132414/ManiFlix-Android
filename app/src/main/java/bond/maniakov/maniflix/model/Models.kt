package bond.maniakov.maniflix.model

object ImageUrlResolver {
    fun resolve(raw: String?): String {
        var trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.drop(1)
        }
        if (trimmed.startsWith("upload/") || trimmed.startsWith("uploads/") || trimmed.contains("/")) {
            return "https://phimimg.com/$trimmed"
        }
        return "https://phimimg.com/upload/vod/$trimmed"
    }
}

data class MovieTag(
    val id: String = "",
    val name: String = "",
    val slug: String = ""
)

data class MovieItem(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val originName: String = "",
    val posterUrl: String = "",
    val thumbUrl: String = "",
    val year: Int = 0,
    val type: String = "",
    val quality: String = "",
    val lang: String = "",
    val episodeCurrent: String = "",
    val time: String = "",
    val category: List<MovieTag> = emptyList(),
    val country: List<MovieTag> = emptyList()
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(posterUrl.ifBlank { thumbUrl })

    val displayThumbUrl: String
        get() = ImageUrlResolver.resolve(thumbUrl.ifBlank { posterUrl })
}

data class MovieDetail(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val originName: String = "",
    val content: String = "",
    val type: String = "",
    val status: String = "",
    val posterUrl: String = "",
    val thumbUrl: String = "",
    val trailerUrl: String = "",
    val time: String = "",
    val episodeCurrent: String = "",
    val episodeTotal: String = "",
    val quality: String = "",
    val lang: String = "",
    val year: Int = 0,
    val actor: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val category: List<MovieTag> = emptyList(),
    val country: List<MovieTag> = emptyList()
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(posterUrl.ifBlank { thumbUrl })

    val displayThumbUrl: String
        get() = ImageUrlResolver.resolve(thumbUrl.ifBlank { posterUrl })

    val plainSynopsis: String
        get() = content
            .replace(HTML_TAG_REGEX, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()

    fun toMovieItem(): MovieItem = MovieItem(
        id = id.ifBlank { slug },
        name = name,
        slug = slug,
        originName = originName,
        posterUrl = posterUrl,
        thumbUrl = thumbUrl,
        year = year,
        type = type,
        quality = quality,
        lang = lang,
        episodeCurrent = episodeCurrent,
        time = time,
        category = category,
        country = country
    )

    companion object {
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
    }
}

data class EpisodeItem(
    val name: String = "",
    val slug: String = "",
    val filename: String = "",
    val linkEmbed: String = "",
    val linkM3u8: String = ""
)

data class EpisodeServer(
    val serverName: String = "",
    val isAi: Boolean = false,
    val serverData: List<EpisodeItem> = emptyList()
)

data class MovieDetailResult(
    val movie: MovieDetail? = null,
    val episodes: List<EpisodeServer> = emptyList()
)

data class FranchiseSeason(
    val name: String = "",
    val fullTitle: String = "",
    val originName: String = "",
    val slug: String = "",
    val year: Int = 0,
    val seasonNumber: Int = 0,
    val poster: String = "",
    val quality: String = "",
    val lang: String = "",
    val isRecap: Boolean = false,
    val episodeCurrent: String = ""
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(poster)

    val displayFullTitle: String
        get() {
            val full = fullTitle.trim()
            if (full.isNotEmpty()) return full
            val shortName = name.trim()
            return if (shortName.isNotEmpty()) shortName else slug
        }
}

data class AdMarker(
    val id: String = "",
    val type: String = "",
    val mode: String = "",
    val start: Double = 0.0,
    val end: Double = 0.0,
    val openEnd: Boolean = false,
    val label: String = "",
    val source: String = ""
)

data class HistoryEntry(
    val slug: String = "",
    val name: String = "",
    val poster: String = "",
    val episodeIndex: Int = 0,
    val episodeName: String = "",
    val currentTime: Double = 0.0,
    val duration: Double = 0.0,
    val updatedAt: Long = 0L
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(poster)

    val progressRatio: Float
        get() = if (duration > 1.0) (currentTime / duration).toFloat().coerceIn(0f, 1f) else 0f

    fun toMovieItem(): MovieItem = MovieItem(
        id = slug,
        name = name.ifBlank { slug },
        slug = slug,
        posterUrl = poster,
        thumbUrl = poster,
        episodeCurrent = episodeName
    )
}

data class FavoriteEntry(
    val slug: String = "",
    val name: String = "",
    val poster: String = "",
    val year: Int = 0,
    val quality: String = "",
    val lang: String = "",
    val addedAt: Long = 0L
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(poster)

    fun toMovieItem(): MovieItem = MovieItem(
        id = slug,
        name = name.ifBlank { slug },
        slug = slug,
        posterUrl = poster,
        thumbUrl = poster,
        year = year,
        quality = quality,
        lang = lang
    )
}

data class BlockedMovieRecord(
    val slug: String = "",
    val name: String = "",
    val poster: String = ""
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(poster)
}

data class UserThemeConfig(
    val presetId: String = "",
    val accent: String = "",
    val accent2: String = "",
    val bg: String = "",
    val surface: String = ""
)

data class UserInfo(
    val username: String = "",
    val displayName: String = "",
    val favoritesCount: Int = 0,
    val historyCount: Int = 0,
    val themeConfig: UserThemeConfig? = null
)

data class AuthResult(
    val success: Boolean = false,
    val token: String = "",
    val user: UserInfo? = null,
    val error: String = ""
)

data class AppVariantInfo(
    val key: String = "",
    val name: String = "",
    val arch: String = "",
    val minSdk: Int = 24,
    val downloadUrl: String = ""
)

data class AndroidVersionConfig(
    val latestVersion: String = "1.0.0",
    val minRequiredVersion: String = "1.0.0",
    val mode: String = "soft",
    val forceUpdate: Boolean = false,
    val title: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val releasePageUrl: String = "",
    val variants: List<AppVariantInfo> = emptyList()
)
