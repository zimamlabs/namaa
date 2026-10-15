package dev.namaa.app

/** The phone's own screen lock (PIN, pattern, fingerprint), used to guard the app; only the platform can ask for it. */
interface AppLock {
    /** True when the phone has a screen lock set up. */
    val available: Boolean

    /** Shows the phone's lock screen; [onResult] is true when the user got through. */
    fun unlock(onResult: (Boolean) -> Unit)
}
