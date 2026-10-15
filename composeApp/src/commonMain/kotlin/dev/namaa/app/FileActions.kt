package dev.namaa.app

/** What only the platform can do with files outside the data folder: the system "save as" and "open" screens (no permission needed). */
interface FileActions {
    /** Lets the user choose where to save [bytes] under [suggestedName]; [onDone] says whether it was written. */
    fun saveAs(suggestedName: String, bytes: ByteArray, onDone: (Boolean) -> Unit)

    /** Lets the user choose a file and returns its bytes, or null when cancelled or unreadable. */
    fun pick(onPicked: (ByteArray?) -> Unit)
}
