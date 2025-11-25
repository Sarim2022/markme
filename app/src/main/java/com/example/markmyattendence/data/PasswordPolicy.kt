package com.example.markmyattendence.data

// You might need to adjust the package name

/**
 * Data class to hold the results of the password policy check.
 */
data class PolicyResult(
    val isLengthValid: Boolean = false, // At least 6 chars
    val isSymbolValid: Boolean = false, // At least one symbol
    val isMixedCaseValid: Boolean = false, // At least one uppercase and one lowercase
    val isDigitValid: Boolean = false // At least one digit (0-9)
) {
    val isPolicyMet: Boolean
        get() = isLengthValid && isSymbolValid && isMixedCaseValid && isDigitValid
}

/**
 * Utility class to check password strength against defined rules.
 */
class PasswordPolicy {
    // Regex for checking if the password contains a special symbol
    private val symbolRegex = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*".toRegex()

    // Regex for checking if the password contains a digit
    private val digitRegex = ".*[0-9].*".toRegex()

    // Regex for checking if the password contains at least one lowercase letter
    private val lowercaseRegex = ".*[a-z].*".toRegex()

    // Regex for checking if the password contains at least one uppercase letter
    private val uppercaseRegex = ".*[A-Z].*".toRegex()

    fun checkPolicy(password: String): PolicyResult {
        return PolicyResult(
            isLengthValid = password.length >= 6,
            isSymbolValid = password.contains(symbolRegex),
            isDigitValid = password.contains(digitRegex),
            // Check for both uppercase AND lowercase
            isMixedCaseValid = password.contains(uppercaseRegex) && password.contains(lowercaseRegex)
        )
    }
}