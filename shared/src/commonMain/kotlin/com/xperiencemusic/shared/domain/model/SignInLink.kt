package com.xperiencemusic.shared.domain.model

/**
 * The link of the sign-in e-mail, `xperiencemusic://connexion?email=…&code=…`: tapping it opens the
 * app with the code already filled in.
 */
data class SignInLink(val email: String, val code: String) {
    companion object {
        const val SCHEME = "xperiencemusic"
        const val HOST = "connexion"

        /** Null for anything that is not a well-formed sign-in link. */
        fun parse(url: String): SignInLink? {
            val prefix = "$SCHEME://$HOST"
            if (!url.startsWith(prefix, ignoreCase = true)) return null
            if (url.length > prefix.length && url[prefix.length] !in "/?") return null
            val query = url.substringAfter('?', missingDelimiterValue = "")
            val params = query.split('&').mapNotNull { pair ->
                val key = pair.substringBefore('=', missingDelimiterValue = "")
                if (key.isEmpty()) null else key to percentDecode(pair.substringAfter('='))
            }.toMap()
            val email = params["email"]?.trim().orEmpty()
            val code = params["code"]?.trim().orEmpty()
            if (email.isEmpty() || !code.all(Char::isDigit) || code.isEmpty()) return null
            return SignInLink(email, code)
        }

        /** %XX only: a "+" stays a "+", as in "prenom+groupe@gmail.com". */
        private fun percentDecode(value: String): String {
            val bytes = ArrayList<Byte>(value.length)
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '%' && i + 2 <= value.lastIndex) {
                    val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
                    if (hex != null) {
                        bytes += hex.toByte()
                        i += 3
                        continue
                    }
                }
                c.toString().encodeToByteArray().forEach { bytes += it }
                i++
            }
            return bytes.toByteArray().decodeToString()
        }
    }
}
