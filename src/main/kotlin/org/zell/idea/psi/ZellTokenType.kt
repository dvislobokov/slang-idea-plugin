package org.zell.idea.psi

import com.intellij.psi.tree.IElementType
import org.zell.idea.lang.ZellLanguage

/** Token type of the Zell lexer (`tokenTypeClass` of the grammar). */
class ZellTokenType(debugName: String) : IElementType(debugName, ZellLanguage) {
    override fun toString(): String = "ZellTokenType." + super.toString()
}

/** Composite (non-stub) element type of the Zell PSI (`elementTypeClass` of the grammar). */
class ZellElementType(debugName: String) : IElementType(debugName, ZellLanguage)
