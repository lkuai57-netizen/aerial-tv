package app.orchard.tv.settings

import android.content.SharedPreferences

/** Zero-based grid coordinates; no package, label or launch intent is stored. */
data class HomeFocusPosition(val row: Int, val column: Int) {
    init { require(row >= 0 && column >= 0) }

    companion object {
        fun read(prefs: SharedPreferences): HomeFocusPosition? {
            val row = prefs.getInt("home_focus_row", -1)
            val column = prefs.getInt("home_focus_column", -1)
            return if (row >= 0 && column >= 0) HomeFocusPosition(row, column) else null
        }

        fun write(prefs: SharedPreferences, value: HomeFocusPosition?) {
            prefs.edit().apply {
                if (value == null) { remove("home_focus_row"); remove("home_focus_column") }
                else { putInt("home_focus_row", value.row); putInt("home_focus_column", value.column) }
            }.apply()
        }
    }
}

/** An empty/missing custom slot falls back to Home's original focus policy. */
internal fun homeEntryFocusIndex(count: Int, columns: Int, position: HomeFocusPosition?): Int? {
    if (count <= 0 || columns <= 0) return null
    if (position == null) return (minOf(count, columns) - 1) / 2
    if (position.column >= columns) return null
    val index = position.row.toLong() * columns + position.column
    return index.takeIf { it < count }?.toInt()
}
