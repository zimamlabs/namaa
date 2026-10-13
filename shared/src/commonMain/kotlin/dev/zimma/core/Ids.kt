package dev.zimma.core

import kotlin.random.Random

/** Short random ids that are safe in file names and easy to read in plain-text files. */
fun newId(random: Random = Random.Default): String {
    val chars = "abcdefghjkmnpqrstuvwxyz23456789"
    return buildString { repeat(8) { append(chars[random.nextInt(chars.length)]) } }
}
