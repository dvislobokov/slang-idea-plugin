package org.slang.idea.psi

import com.intellij.psi.tree.IElementType
import org.slang.idea.lang.SlangLanguage

/** Token type of the Slang lexer (`tokenTypeClass` of the grammar). */
class SlangTokenType(debugName: String) : IElementType(debugName, SlangLanguage) {
    override fun toString(): String = "SlangTokenType." + super.toString()
}

/** Composite (non-stub) element type of the Slang PSI (`elementTypeClass` of the grammar). */
class SlangElementType(debugName: String) : IElementType(debugName, SlangLanguage)
