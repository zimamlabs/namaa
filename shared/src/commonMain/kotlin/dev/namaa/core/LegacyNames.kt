package dev.namaa.core

/**
 * The literal folder and file-name patterns that earlier versions used. They are kept only so old data can be FOUND and copied
 * into the current folder; no screen shows them. Do not use these for anything new.
 */
object LegacyNames {
    /** Folder name used inside the app's private storage, on the desktop home, and as the "(from ...)" label of copied clashes. */
    const val FOLDER = "Zimma"
    /** The shared folder of earlier versions, relative to the phone's storage root or the desktop home. */
    const val SHARED_FOLDER = "Zimam/$FOLDER"
    /** Where the desktop build kept its settings file. */
    const val DESKTOP_CONFIG_DIR = "zimma"
}
