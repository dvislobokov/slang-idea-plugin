package org.zell.idea.stubs

import com.intellij.icons.AllIcons
import com.intellij.psi.tree.IElementType
import org.zell.idea.psi.ZellTypes
import javax.swing.Icon

/** Kind of a named declaration, by its element type. */
enum class ZellDeclKind(val icon: Icon) {
    STRUCT(AllIcons.Nodes.Class),
    INTERFACE(AllIcons.Nodes.Interface),
    ENUM(AllIcons.Nodes.Enum),
    FUNC(AllIcons.Nodes.Function),
    CONST(AllIcons.Nodes.Constant),
    ;

    companion object {
        @JvmStatic
        fun of(type: IElementType): ZellDeclKind = when (type) {
            ZellTypes.STRUCT_DECL -> STRUCT
            ZellTypes.INTERFACE_DECL -> INTERFACE
            ZellTypes.ENUM_DECL -> ENUM
            ZellTypes.CONST_DECL -> CONST
            else -> FUNC
        }
    }
}
