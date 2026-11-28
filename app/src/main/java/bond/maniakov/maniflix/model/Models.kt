package bond.maniakov.maniflix.model

import java.text.Normalizer

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
    val subLangs: List<String> = emptyList(),
    val viName: String = "",
    val enName: String = "",
    val romajiName: String = "",
    val episodeCurrent: String = "",
    val time: String = "",
    val category: List<MovieTag> = emptyList(),
    val country: List<MovieTag> = emptyList()
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(posterUrl.ifBlank { thumbUrl })

    val displayThumbUrl: String
        get() = ImageUrlResolver.resolve(thumbUrl.ifBlank { posterUrl })

    fun localizedPrimaryTitle(uiLang: String): String =
        TitleLocalizer.resolve(
            uiLang = uiLang,
            name = name,
            originName = originName,
            viName = viName,
            enName = enName,
            romajiName = romajiName,
            slug = slug
        ).first

    fun localizedSecondaryTitle(uiLang: String): String =
        TitleLocalizer.resolve(
            uiLang = uiLang,
            name = name,
            originName = originName,
            viName = viName,
            enName = enName,
            romajiName = romajiName,
            slug = slug
        ).second
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
    val subLangs: List<String> = emptyList(),
    val viName: String = "",
    val enName: String = "",
    val romajiName: String = "",
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

    fun localizedPrimaryTitle(uiLang: String): String =
        TitleLocalizer.resolve(
            uiLang = uiLang,
            name = name,
            originName = originName,
            viName = viName,
            enName = enName,
            romajiName = romajiName,
            slug = slug
        ).first

    fun localizedSecondaryTitle(uiLang: String): String =
        TitleLocalizer.resolve(
            uiLang = uiLang,
            name = name,
            originName = originName,
            viName = viName,
            enName = enName,
            romajiName = romajiName,
            slug = slug
        ).second

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
        subLangs = subLangs,
        viName = viName,
        enName = enName,
        romajiName = romajiName,
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
    val sortIndex: Int = -1,
    val name: String = "",
    val nameVi: String = "",
    val nameEn: String = "",
    val fullTitle: String = "",
    val originName: String = "",
    val slug: String = "",
    val year: Int = 0,
    val seasonNumber: Int = 0,
    val poster: String = "",
    val quality: String = "",
    val lang: String = "",
    val isRecap: Boolean = false,
    val isLiveAction: Boolean = false,
    val episodeCurrent: String = "",
    val baseTitle: String = "",
    val baseTitleVi: String = "",
    val baseTitleEn: String = "",
    val baseTitleRomaji: String = ""
) {
    val displayPosterUrl: String
        get() = ImageUrlResolver.resolve(poster)

    val displayFullTitle: String
        get() {
            val shortName = name.trim()
            if (shortName.isNotEmpty()) return shortName
            val full = fullTitle.trim()
            return if (full.isNotEmpty()) full else slug
        }

    fun localizedLabel(uiLang: String, allSeasons: List<FranchiseSeason> = emptyList()): String {
        val isEn = uiLang == "en"
        val explicit = (if (isEn) nameEn.ifBlank { name } else nameVi.ifBlank { name }).trim()
        val fallbackRaw = explicit.ifEmpty { fullTitle.trim().ifEmpty { slug } }
        val hasSeason2 = allSeasons.any { it.seasonNumber >= 2 }
        return TitleLocalizer.stripFranchisePrefix(
            label = fallbackRaw,
            uiLang = uiLang,
            prefixes = listOf(baseTitleVi, baseTitleEn, baseTitleRomaji, baseTitle),
            hasSeason2 = hasSeason2,
            isLiveAction = isLiveAction
        )
    }

    companion object {
        fun localizedHeader(uiLang: String, seasons: List<FranchiseSeason>): String {
            val isEn = uiLang == "en"
            val first = seasons.firstOrNull()
            val rawBase = if (isEn) {
                first?.baseTitleEn?.trim().orEmpty().ifEmpty { first?.baseTitle?.trim().orEmpty() }
            } else {
                first?.baseTitleVi?.trim().orEmpty().ifEmpty { first?.baseTitle?.trim().orEmpty() }
            }
            val cleanBase = TitleLocalizer.cleanFranchiseTitle(rawBase)
            val count = seasons.size
            return if (cleanBase.isEmpty()) {
                if (isEn) "Franchise ($count parts)" else "Các phần phim ($count)"
            } else {
                if (isEn) "Franchise: $cleanBase • $count parts" else "Các phần phim: $cleanBase • $count phần"
            }
        }
    }
}

object TitleLocalizer {
    private val VI_DIACRITICS_REGEX = Regex("[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]", RegexOption.IGNORE_CASE)
    private val CJK_CHAR_REGEX = Regex("[\\u3040-\\u30ff\\u3400-\\u9fff\\uac00-\\ud7af]")
    private val PURE_CJK_REGEX = Regex("^[\\u3040-\\u30ff\\u3400-\\u9fff\\uac00-\\ud7af\\s\\d:!\\-–·.,'()]+$")
    private val ROMAJI_TOKEN_REGEX = Regex("\\b(no|wa|ga|wo|ni|kara|made|tachi|monogatari|kyoukai|kanata|shoujo|shounen|sekai|kimi|boku|ore|mahou|tensei|isekai|kuromukuro|shinonome|gekijou|saiban|kako|mirai|yaiba|kimetsu|shingeki|kyojin|jujutsu|kaisen|shippuuden|sousou|frieren|kusuriya|hitorigoto|dandadan)\\b", RegexOption.IGNORE_CASE)
    private val SPLIT_DELIM_REGEX = Regex("\\s+(?:/|\\||•|;)\\s+")
    private val COMBINING_MARKS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val NON_ALNUM_REGEX = Regex("[^a-zA-Z0-9]+")
    private val PHAN_PREFIX_REGEX = Regex("^Phần\\s+(\\d+)", RegexOption.IGNORE_CASE)

    fun hasViDiacritics(text: String): Boolean = VI_DIACRITICS_REGEX.containsMatchIn(text)

    fun isPureCJK(text: String): Boolean {
        val s = text.trim()
        if (s.isEmpty()) return false
        return CJK_CHAR_REGEX.containsMatchIn(s) && PURE_CJK_REGEX.matches(s)
    }

    fun looksRomaji(text: String): Boolean {
        val s = text.trim()
        if (s.isEmpty() || hasViDiacritics(s) || isPureCJK(s)) return false
        return ROMAJI_TOKEN_REGEX.containsMatchIn(s)
    }

    fun splitCandidates(raw: String?): List<String> {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return emptyList()
        return s.split(SPLIT_DELIM_REGEX).map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun normKey(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        val stripped = COMBINING_MARKS_REGEX.replace(nfd, "")
            .replace('đ', 'd')
            .replace('Đ', 'd')
        return NON_ALNUM_REGEX.replace(stripped, "").lowercase()
    }

    fun resolve(
        uiLang: String,
        name: String?,
        originName: String?,
        viName: String? = null,
        enName: String? = null,
        romajiName: String? = null,
        slug: String = ""
    ): Pair<String, String> {
        val rawName = name?.trim().orEmpty()
        val origParts = splitCandidates(originName)
        val explicitVi = viName?.trim().orEmpty()
        val explicitEn = enName?.trim().orEmpty()
        val explicitRomaji = romajiName?.trim().orEmpty()

        val viTitle = when {
            explicitVi.isNotEmpty() -> explicitVi
            hasViDiacritics(rawName) -> rawName
            else -> origParts.firstOrNull { hasViDiacritics(it) }.orEmpty()
        }

        val romajiTitle = when {
            explicitRomaji.isNotEmpty() -> explicitRomaji
            looksRomaji(rawName) -> rawName
            else -> origParts.firstOrNull { looksRomaji(it) }.orEmpty()
        }

        val allCands = (listOf(rawName) + origParts).filter { it.isNotEmpty() }
        val enTitle = when {
            explicitEn.isNotEmpty() && !isPureCJK(explicitEn) -> explicitEn
            else -> allCands.firstOrNull { !hasViDiacritics(it) && !isPureCJK(it) && !looksRomaji(it) }
                ?: romajiTitle
        }

        val safeFallback = (listOf(
            enTitle,
            romajiTitle,
            viTitle,
            if (isPureCJK(rawName)) "" else rawName
        ) + origParts.filter { !isPureCJK(it) } + listOf(rawName, slug)).firstOrNull { it.isNotEmpty() } ?: slug

        val isEn = uiLang == "en"
        val primary: String
        val secCandidates: List<String>

        if (isEn) {
            primary = enTitle.ifEmpty { romajiTitle.ifEmpty { safeFallback } }
            secCandidates = listOf(romajiTitle, viTitle) + origParts + listOf(rawName)
        } else {
            if (viTitle.isNotEmpty()) {
                primary = viTitle
                secCandidates = listOf(enTitle, romajiTitle) + origParts + listOf(rawName)
            } else {
                primary = enTitle.ifEmpty { romajiTitle.ifEmpty { safeFallback } }
                secCandidates = listOf(romajiTitle) + origParts + listOf(rawName)
            }
        }

        val seen = HashSet<String>()
        seen.add(normKey(primary))
        val picked = ArrayList<String>(2)
        for (cand in secCandidates) {
            val trimmed = cand.trim()
            if (trimmed.isEmpty() || isPureCJK(trimmed)) continue
            val k = normKey(trimmed)
            if (k.isEmpty() || !seen.add(k)) continue
            picked.add(trimmed)
            if (picked.size >= 2) break
        }
        return primary to picked.joinToString(" • ")
    }

    private val TRIM_FRANCHISE_SEP_REGEX = Regex("^[\\s:\\-–—•|·/.,]+|[\\s:\\-–—•|·/]+$")
    private val MULTI_SPACE_REGEX = Regex("\\s{2,}")
    private val TV_SERIES_ONLY_REGEX = Regex("^tv\\s*series$", RegexOption.IGNORE_CASE)
    private val MOVIE_TAG_REGEX = Regex("\\s*\\((?:Movie|Phim\\s*Lẻ)\\)\\s*", RegexOption.IGNORE_CASE)
    private val LIVE_ACTION_TAG_REGEX = Regex("\\(\\s*live[\\s\\-]*action\\s*\\)", RegexOption.IGNORE_CASE)

    fun cleanFranchiseTitle(raw: String?): String =
        MULTI_SPACE_REGEX.replace(TRIM_FRANCHISE_SEP_REGEX.replace(raw.orEmpty(), ""), " ").trim()

    private fun buildOrthoPattern(prefix: String): String {
        val clean = cleanFranchiseTitle(prefix)
        if (clean.length < 3) return ""
        val sb = StringBuilder()
        var i = 0
        while (i < clean.length) {
            val ch = clean[i]
            when {
                ch.isWhitespace() -> {
                    while (i + 1 < clean.length && clean[i + 1].isWhitespace()) i++
                    sb.append("[\\s:\\-–—•|·]+")
                }
                ch == '\'' || ch == '’' || ch == '‘' || ch == '`' -> sb.append("['’‘`]?")
                ch == 'ì' || ch == 'ỳ' || ch == 'Ì' || ch == 'Ỳ' -> sb.append("[ìỳÌỲ]")
                ch == 'í' || ch == 'ý' || ch == 'Í' || ch == 'Ý' -> sb.append("[íýÍÝ]")
                ch == 'ỉ' || ch == 'ỷ' || ch == 'Ỉ' || ch == 'Ỷ' -> sb.append("[ỉỷỈỶ]")
                ch == 'ĩ' || ch == 'ỹ' || ch == 'Ĩ' || ch == 'Ỹ' -> sb.append("[ĩỹĨỸ]")
                ch == 'ị' || ch == 'ỵ' || ch == 'Ị' || ch == 'Ỵ' -> sb.append("[ịỵỊỴ]")
                else -> sb.append(Regex.escape(ch.toString()))
            }
            i++
        }
        return sb.toString()
    }

    fun stripFranchisePrefix(
        label: String,
        uiLang: String,
        prefixes: List<String>,
        hasSeason2: Boolean,
        isLiveAction: Boolean = false
    ): String {
        var cur = label.trim()
        val rawPicked = cur
        if (cur.isNotEmpty()) {
            val sortedPats = prefixes
                .map { cleanFranchiseTitle(it) }
                .filter { it.length >= 3 }
                .sortedByDescending { it.length }
                .map { buildOrthoPattern(it) }
                .filter { it.isNotEmpty() }

            for (pat in sortedPats) {
                if (Regex("^$pat[\\s:!\\-–—.]*$", RegexOption.IGNORE_CASE).matches(cur)) {
                    cur = ""
                    break
                }
                val leadRe = Regex("^$pat(?:\\s*[:\\-–—!|•/]+\\s*|\\s+(?=(?:Movie|Phần|Phan|Season|SS|Mùa|OVA|ONA|Special|SP|Mini|Idol|Shinonome|Chapter|Part|Arc|Episode|Tập|\\d+|II|III|IV|V|VI)\\b))", RegexOption.IGNORE_CASE)
                if (leadRe.containsMatchIn(cur)) {
                    cur = leadRe.replaceFirst(cur, "").trim()
                    break
                }
                val trailRe = Regex("(?:\\s*[:\\-–—|•]+\\s*)$pat(\\s*\\((?:Live-Action|Movie|OVA|ONA|Special|TV)\\))?[\\s.!]*$", RegexOption.IGNORE_CASE)
                if (trailRe.containsMatchIn(cur)) {
                    val cand = trailRe.replace(cur, "$1").trim()
                    if (cand.length >= 2) {
                        cur = cand
                        break
                    }
                }
            }
        }
        cur = Regex("^[:\\-–—•|\\s]+|[:\\-–—•|\\s]+$").replace(cur, "").trim()
        cur = if (cur.isEmpty() || TV_SERIES_ONLY_REGEX.matches(cur)) {
            if (TV_SERIES_ONLY_REGEX.matches(rawPicked)) "TV Series"
            else if (hasSeason2) (if (uiLang == "en") "Season 1" else "Phần 1") else "TV Series"
        } else if (uiLang == "en") {
            PHAN_PREFIX_REGEX.replace(cur, "Season $1")
        } else {
            cur
        }
        if (isLiveAction) {
            cur = MULTI_SPACE_REGEX.replace(MOVIE_TAG_REGEX.replace(cur, " "), " ").trim()
            if (!LIVE_ACTION_TAG_REGEX.containsMatchIn(cur)) {
                cur = "$cur (Live-Action)"
            }
        }
        return cur
    }

    fun sortFranchiseSeasons(seasons: List<FranchiseSeason>, baseTitle: String?): List<FranchiseSeason> {
        if (seasons.size <= 1) return seasons
        if (seasons.any { it.sortIndex >= 0 }) {
            return seasons.withIndex().sortedWith(
                compareBy<IndexedValue<FranchiseSeason>> { it.value.isLiveAction }
                    .thenBy { if (it.value.sortIndex >= 0) it.value.sortIndex else (1000 + it.index) }
                    .thenBy { it.index }
            ).map { it.value }
        }
        val baseNorm = normKey(baseTitle.orEmpty())
        return seasons.withIndex().sortedWith { a, b ->
            if (a.value.isLiveAction != b.value.isLiveAction) {
                return@sortedWith if (a.value.isLiveAction) 1 else -1
            }
            val ay = if (a.value.year > 0) a.value.year else 9999
            val by = if (b.value.year > 0) b.value.year else 9999
            if (ay != by) return@sortedWith ay.compareTo(by)
            val aRoot = baseNorm.isNotEmpty() && (normKey(a.value.fullTitle) == baseNorm || a.value.name == "TV Series")
            val bRoot = baseNorm.isNotEmpty() && (normKey(b.value.fullTitle) == baseNorm || b.value.name == "TV Series")
            if (aRoot != bRoot) return@sortedWith if (aRoot) -1 else 1
            a.index.compareTo(b.index)
        }.map { it.value }
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
