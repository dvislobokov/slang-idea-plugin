package org.zell.idea.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/** `.zl` files. */
object ZellFileType : LanguageFileType(ZellLanguage) {
    override fun getName(): String = "Zell"

    override fun getDescription(): String = "Zell source file"

    override fun getDefaultExtension(): String = "zl"

    override fun getIcon(): Icon = ZellIcons.FILE
}
