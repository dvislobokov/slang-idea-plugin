package org.slang.idea.stubs

import com.intellij.icons.AllIcons
import com.intellij.psi.tree.IElementType
import org.slang.idea.psi.SlangTypes
import javax.swing.Icon

/** Kind of a named declaration, by its element type. */
enum class SlangDeclKind(val icon: Icon) {
    STRUCT(AllIcons.Nodes.Class),
    INTERFACE(AllIcons.Nodes.Interface),
    ENUM(AllIcons.Nodes.Enum),
    FUNC(AllIcons.Nodes.Function),
    CONST(AllIcons.Nodes.Constant),
    ;

    companion object {
        @JvmStatic
        fun of(type: IElementType): SlangDeclKind = when (type) {
            SlangTypes.STRUCT_DECL -> STRUCT
            SlangTypes.INTERFACE_DECL -> INTERFACE
            SlangTypes.ENUM_DECL -> ENUM
            SlangTypes.CONST_DECL -> CONST
            else -> FUNC
        }
    }
}
