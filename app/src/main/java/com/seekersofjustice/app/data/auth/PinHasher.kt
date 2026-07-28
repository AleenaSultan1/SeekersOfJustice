package com.seekersofjustice.app.data.auth

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted PIN hashing. This gates the UI and keeps raw PINs off disk; it is NOT
 * a substitute for encrypting the data itself, which remains a separate step.
 */
object PinHasher {

    private const val ITERATIONS = 10_000

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var data = (saltHex + pin).toByteArray()
        repeat(ITERATIONS) { data = digest.digest(data) }
        return data.toHex()
    }

    fun verify(pin: String, saltHex: String, expectedHash: String): Boolean =
        constantTimeEquals(hash(pin, saltHex), expectedHash)

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
