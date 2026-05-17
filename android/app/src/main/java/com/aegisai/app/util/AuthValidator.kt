package com.aegisai.app.util

object AuthValidator {
    fun isGmail(email: String): Boolean {
        val e = email.trim().lowercase()
        if (!e.contains("@")) return false
        val domain = e.substringAfter("@")
        return domain == "gmail.com" || domain == "googlemail.com"
    }

    fun mapAuthError(message: String?): String {
        val m = message?.lowercase().orEmpty()
        return when {
            m.contains("already registered") -> "An account already exists for this Gmail. Try signing in."
            m.contains("invalid login") -> "Incorrect email or password."
            m.contains("email not confirmed") -> "Verify your Gmail with the code we sent."
            else -> message ?: "Something went wrong. Try again."
        }
    }
}
