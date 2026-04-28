package com.gkim.im.android.core.model

object MacroSubstitution {

    val UserForms: List<String> = listOf("{{user}}", "{user}", "<user>")
    val CharForms: List<String> = listOf("{{char}}", "{char}", "<char>")

    fun substituteMacros(
        template: String,
        userDisplayName: String,
        charDisplayName: String,
    ): String {
        var result = template
        if (userDisplayName.isNotEmpty()) {
            for (form in UserForms) {
                result = result.replace(form, userDisplayName, ignoreCase = true)
            }
        }
        if (charDisplayName.isNotEmpty()) {
            for (form in CharForms) {
                result = result.replace(form, charDisplayName, ignoreCase = true)
            }
        }
        return result
    }
}
