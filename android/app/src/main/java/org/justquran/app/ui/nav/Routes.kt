package org.justquran.app.ui.nav

object Routes {
    const val HOME: String = "home"
    const val SURAHS: String = "surahs"
    const val JUZ: String = "juz"
    const val READER: String = "surah/{n}?v={v}&end={end}"
    const val SEARCH: String = "search"
    const val BOOKMARKS: String = "bookmarks"
    const val RECITATION: String = "recitation"
    const val SETTINGS: String = "settings"
    const val ABOUT: String = "about"
    const val HELP: String = "help"
    const val KHATM: String = "khatm"
    const val BOOK_HOME: String = "book"
    const val BOOK: String = "book/{lang}/{n}?v={v}"

    fun reader(surah: Int, verse: Int? = null, end: Boolean = false): String {
        val sb = StringBuilder("surah/").append(surah)
        if (verse != null) {
            sb.append("?v=").append(verse)
        }
        if (end) {
            sb.append(if (verse != null) "&end=1" else "?end=1")
        }
        return sb.toString()
    }

    fun reader(s: Int, v: Int? = null): String = reader(surah = s, verse = v, end = false)

    fun book(lang: String, n: Int, v: Int? = null): String {
        return if (v != null && v > 0) "book/$lang/$n?v=$v" else "book/$lang/$n"
    }
}
