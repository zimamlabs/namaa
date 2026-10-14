package dev.namaa.core

private val categoryStr = mapOf(
    "food" to Str.CatFood, "transport" to Str.CatTransport, "home" to Str.CatHome, "bills" to Str.CatBills, "health" to Str.CatHealth,
    "fun" to Str.CatFun, "shopping" to Str.CatShopping, "salary" to Str.CatSalary, "other" to Str.CatOther, "transfer" to Str.CatTransfer,
)

/** A category as shown: the built-in names are translated, anything the user typed is shown as typed. Stored values stay English. */
fun categoryTitle(category: String, lang: Lang): String {
    val c = category.ifEmpty { "Other" }
    return categoryStr[c.trim().lowercase()]?.text(lang) ?: c
}
