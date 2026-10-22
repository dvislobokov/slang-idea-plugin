package org.zell.idea.lang

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Plugin icons; `zell_dark.svg` is picked automatically in dark themes. */
object ZellIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/zell.svg", ZellIcons::class.java)
}
