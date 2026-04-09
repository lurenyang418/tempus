package com.cappielloantonio.tempo.subsonic

import com.cappielloantonio.tempo.subsonic.utils.StringUtil
import java.util.UUID

class SubsonicPreferences {
    var serverUrl: String? = null
    var username: String? = null
    var clientName: String? = "Tempus"
    var authentication: SubsonicAuthentication? = null
        private set

    fun setAuthentication(
        password: String?,
        token: String?,
        salt: String?,
        isLowSecurity: Boolean
    ) {
        if (password != null) {
            this.authentication = SubsonicAuthentication(password, isLowSecurity)
        }

        if (token != null && salt != null) {
            this.authentication = SubsonicAuthentication(token, salt)
        }
    }

    class SubsonicAuthentication {
        var password: String? = null
            private set
        var salt: String? = null
            private set
        var token: String? = null
            private set

        constructor(password: String?, isLowSecurity: Boolean) {
            if (isLowSecurity) {
                this.password = password
            } else {
                update(password)
            }
        }

        constructor(token: String?, salt: String?) {
            this.token = token
            this.salt = salt
        }

        fun update(password: String?) {
            this.salt = UUID.randomUUID().toString()
            this.token = StringUtil.tokenize(password + salt)
        }
    }
}
