package ec.edu.uce.appproductosfinal.utils

import java.security.MessageDigest

object SecurityUtils {
    /**
     * Convierte una cadena de texto a un hash SHA-256.
     * Es un proceso de una sola vía (no se puede descifrar).
     */
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
