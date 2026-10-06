package uz.ishvaqtim.app.nfc

import java.security.MessageDigest

/** Karta UID sini SHA-256 xeshga aylantiradi. Bazada UID ning o'zi emas, xesh saqlanadi. */
object CardHasher {
    private const val SALT = "IshVaqtim|card|"

    fun hash(uid: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(SALT.toByteArray())
        val bytes = digest.digest(uid)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
