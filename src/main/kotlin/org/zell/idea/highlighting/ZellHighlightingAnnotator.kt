package org.slang.idea.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.slang.idea.psi.SlangTypes
import org.slang.idea.psi.impl.SlangDeclMixin

/**
 * Purely syntactic highlighting of declaration names, attributes and
 * namespace paths (one parent check per leaf; no resolve). Everything that
 * needs semantics comes from slang-lsp semantic tokens.
 */
class SlangHighlightingAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val node = element.node ?: return
        val key = when (node.elementType) {
            SlangTypes.AT ->
                // `@` that starts an attribute (an `@` between operands is MatMul).
                if (node.treeParent?.elementType == SlangTypes.ATTRIBUTE) SlangColors.ATTRIBUTE else null
            SlangTypes.QUALIFIED_NAME -> when (node.treeParent?.elementType) {
                SlangTypes.ATTRIBUTE -> SlangColors.ATTRIBUTE
                SlangTypes.NAMESPACE_DECL, SlangTypes.IMPORT_DECL -> SlangColors.NAMESPACE
                else -> null
            }
            SlangTypes.IDENTIFIER, SlangTypes.DEFAULT -> declarationKey(element)
            else -> null
        } ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(key).create()
    }

    private fun declarationKey(element: PsiElement): TextAttributesKey? {
        val parent = element.parent ?: return null
        if (parent.node.elementType == SlangTypes.FUNC_SIGNATURE) {
            // `func Name(...)` of a func_decl, a method or an interface method.
            return if (parent.node.findChildByType(SlangTypes.IDENTIFIER) == element.node) SlangColors.FUNCTION_DECLARATION else null
        }
        val decl = parent as? SlangDeclMixin ?: return null
        if (decl.nameIdentifier != element) return null
        return when (decl.node.elementType) {
            SlangTypes.STRUCT_DECL, SlangTypes.ENUM_DECL -> SlangColors.TYPE_NAME
            SlangTypes.INTERFACE_DECL -> SlangColors.INTERFACE_NAME
            SlangTypes.CONST_DECL -> SlangColors.CONST_DECLARATION
            else -> null
        }
    }
}
