package org.slang.idea.lang

import com.intellij.lang.Language

/** The Slang language (`.sl` files). */
object SlangLanguage : Language("Slang") {
    private fun readResolve(): Any = SlangLanguage

    override fun getDisplayName(): String = "Slang"

    override fun isCaseSensitive(): Boolean = true
}
