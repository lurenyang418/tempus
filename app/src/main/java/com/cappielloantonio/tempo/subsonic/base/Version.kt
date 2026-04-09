package com.cappielloantonio.tempo.subsonic.base

import kotlin.math.max

class Version private constructor(versionString: String) : Comparable<Version?> {
    val versionString: String

    init {
        require(!(versionString == null || !versionString.matches(VERSION_PATTERN.toRegex()))) { "Invalid version format" }
        this.versionString = versionString
    }

    fun isLowerThan(version: Version?): Boolean {
        return compareTo(version) < 0
    }

    override fun compareTo(that: Version?): Int {
        if (that == null) {
            return 1
        }

        val thisParts: Array<String?> =
            this.versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val thatParts: Array<String?> =
            that.versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val length = max(thisParts.size, thatParts.size)

        for (i in 0..<length) {
            val thisPart = if (i < thisParts.size) thisParts[i]!!.toInt() else 0
            val thatPart = if (i < thatParts.size) thatParts[i]!!.toInt() else 0

            if (thisPart < thatPart) {
                return -1
            }
            if (thisPart > thatPart) {
                return 1
            }
        }
        return 0
    }

    override fun toString(): String {
        return versionString
    }

    companion object {
        private const val VERSION_PATTERN = "\\d+(\\.\\d+)*"
        fun of(versionString: String): Version {
            return Version(versionString)
        }
    }
}