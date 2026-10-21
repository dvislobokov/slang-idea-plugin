package org.slang.idea.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/** `.sl` files. */
object SlangFileType : LanguageFileType(SlangLanguage) {
    override fun getName(): String = "Slang"

    override fun getDescription(): String = "Slang source file"

    override fun getDefaultExtension(): String = "sl"

    override fun getIcon(): Icon = SlangIcons.FILE
}
