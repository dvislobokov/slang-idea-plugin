package org.zell.idea.lang

import com.intellij.lang.Language

/** The Zell language (`.zl` files). */
object ZellLanguage : Language("Zell") {
    private fun readResolve(): Any = ZellLanguage

    override fun getDisplayName(): String = "Zell"

    override fun isCaseSensitive(): Boolean = true
}
