package org.slang.idea.lang

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Plugin icons; `slang_dark.svg` is picked automatically in dark themes. */
object SlangIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/slang.svg", SlangIcons::class.java)
}
