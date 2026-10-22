package org.slang.idea.structure

import com.intellij.lang.Commenter

/** `//` line comments, `/* */` block comments (nestable in Slang). */
class SlangCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"

    override fun getBlockCommentPrefix(): String = "/*"

    override fun getBlockCommentSuffix(): String = "*/"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
