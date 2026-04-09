package com.cappielloantonio.tempo.util

import java.io.UnsupportedEncodingException
import java.lang.Boolean
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.function.Predicate
import kotlin.Any
import kotlin.Char
import kotlin.NullPointerException
import kotlin.String

object Util {
    @JvmStatic
    fun <T> distinctByKey(keyExtractor: Function<in T?, Any?>): Predicate<T?>? {
        try {
            val uniqueMap: MutableMap<Any?, Boolean?> = ConcurrentHashMap<Any?, Boolean?>()
            return Predicate<T?> { t: T? ->
                uniqueMap.putIfAbsent(
                    keyExtractor.apply(t),
                    java.lang.Boolean.TRUE as Boolean?
                ) == null
            }
        } catch (exception: NullPointerException) {
            return null
        }
    }

    fun toPascalCase(name: String?): String? {
        if (name == null || name.isEmpty()) {
            return name
        }

        var pascalCase = StringBuilder()

        var newChar: Char
        var toUpper = false
        val charArray = name.toCharArray()

        for (ctr in 0..charArray.size - 1) {
            if (ctr == 0) {
                newChar = charArray[ctr].uppercaseChar()
                pascalCase = StringBuilder(newChar.toString())
                continue
            }

            if (charArray[ctr] == '_') {
                toUpper = true
                continue
            }

            if (toUpper) {
                newChar = charArray[ctr].uppercaseChar()
                pascalCase.append(newChar)
                toUpper = false
                continue
            }

            pascalCase.append(charArray[ctr])
        }

        return pascalCase.toString()
    }

    @JvmStatic
    fun encode(value: String?): String? {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        } catch (ex: UnsupportedEncodingException) {
            return value
        }
    }
}
