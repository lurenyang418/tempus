package com.cappielloantonio.tempo.github

import com.cappielloantonio.tempo.github.api.release.ReleaseClient

class Github {
    var releaseClient: ReleaseClient? = null
        get() {
            if (field == null) {
                field = ReleaseClient(this)
            }

            return field
        }
        private set

    val url: String
        get() = "https://api.github.com/"

    companion object {
        const val owner: String = "eddyizm"
        const val repo: String = "Tempus"
    }
}
