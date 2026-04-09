package com.cappielloantonio.tempo.subsonic.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object StringUtil {
    fun tokenize(s: String): String {
        val MD5 = "MD5"
        try {
            val digest = MessageDigest.getInstance(MD5)
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            val hexString = StringBuilder()
            for (aMessageDigest in messageDigest) {
                val h = StringBuilder(Integer.toHexString(0xFF and aMessageDigest.toInt()))
                while (h.length < 2) {
                    h.insert(0, "0")
                }
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }
}
