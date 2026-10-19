package sg.smartwaste.app.util

import java.security.MessageDigest

object Hashing {
    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** Shows only the last 4 characters of a code, e.g. "•••• 7F2K". */
    fun mask(code: String): String =
        if (code.length <= 4) code else "•••• " + code.takeLast(4)
}
