package org.zell.idea.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.zell.idea.psi.ZellTypes
import org.zell.idea.psi.impl.ZellDeclMixin

/**
 * Purely syntactic highlighting of declaration names, attributes and
 * namespace paths (one parent check per leaf; no resolve). Everything that
 * needs semantics comes from zell-lsp semantic tokens.
 */
class ZellHighlightingAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val node = element.node ?: return
        val key = when (node.elementType) {
            ZellTypes.AT ->
                // `@` that starts an attribute (an `@` between operands is MatMul).
                if (node.treeParent?.elementType == ZellTypes.ATTRIBUTE) ZellColors.ATTRIBUTE else null
            ZellTypes.QUALIFIED_NAME -> when (node.treeParent?.elementType) {
                ZellTypes.ATTRIBUTE -> ZellColors.ATTRIBUTE
                ZellTypes.NAMESPACE_DECL, ZellTypes.IMPORT_DECL -> ZellColors.NAMESPACE
                else -> null
            }
            ZellTypes.IDENTIFIER, ZellTypes.DEFAULT -> declarationKey(element)
            else -> null
        } ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(key).create()
    }

    private fun declarationKey(element: PsiElement): TextAttributesKey? {
        val parent = element.parent ?: return null
        if (parent.node.elementType == ZellTypes.FUNC_SIGNATURE) {
            // `func Name(...)` of a func_decl, a method or an interface method.
            return if (parent.node.findChildByType(ZellTypes.IDENTIFIER) == element.node) ZellColors.FUNCTION_DECLARATION else null
        }
        val decl = parent as? ZellDeclMixin ?: return null
        if (decl.nameIdentifier != element) return null
        return when (decl.node.elementType) {
            ZellTypes.STRUCT_DECL, ZellTypes.ENUM_DECL -> ZellColors.TYPE_NAME
            ZellTypes.INTERFACE_DECL -> ZellColors.INTERFACE_NAME
            ZellTypes.CONST_DECL -> ZellColors.CONST_DECLARATION
            else -> null
        }
    }
}
