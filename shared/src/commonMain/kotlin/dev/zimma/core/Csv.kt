package dev.zimma.core

/** The small CSV dialect the money files use: commas, double quotes, `\n` line ends. */
object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        val t = text.replace("\r\n", "\n")
        while (i < t.length) {
            val c = t[i]
            when {
                quoted && c == '"' && i + 1 < t.length && t[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { row += cell.toString(); cell.clear() }
                c == '\n' && !quoted -> { row += cell.toString(); cell.clear(); if (row.any { it.isNotEmpty() } || row.size > 1) rows += row; row = ArrayList() }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
        return rows
    }

    fun format(rows: List<List<String>>): String = rows.joinToString("") { row -> row.joinToString(",") { quote(it) } + "\n" }

    private fun quote(v: String) = if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
}
