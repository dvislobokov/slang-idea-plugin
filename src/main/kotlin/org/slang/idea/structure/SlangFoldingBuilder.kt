package org.slang.idea.structure

import com.intellij.codeInsight.folding.CodeFoldingSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.slang.idea.psi.SlangTokenTypes
import org.slang.idea.psi.SlangTypes

/**
 * Folds multi-line `{ ... }` bodies (blocks, struct/interface/enum bodies,
 * switch bodies), multi-line block comments and runs of two or more imports.
 */
class SlangFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val out = ArrayList<FoldingDescriptor>()
        collect(root.node, document, out)
        imports(root.node, out)
        return out.toTypedArray()
    }

    private fun collect(node: ASTNode, document: Document, out: MutableList<FoldingDescriptor>) {
        val type = node.elementType
        when {
            type == SlangTokenTypes.BLOCK_COMMENT -> foldIfMultiline(node, node.textRange, document, out)
            type in BRACED -> {
                val l = node.findChildByType(SlangTypes.LBRACE)
                val r = node.findChildByType(SlangTypes.RBRACE)
                if (l != null && r != null && r.startOffset > l.startOffset) {
                    foldIfMultiline(node, TextRange(l.startOffset, r.textRange.endOffset), document, out)
                }
            }
        }
        var child = node.firstChildNode
        while (child != null) {
            collect(child, document, out)
            child = child.treeNext
        }
    }

    private fun foldIfMultiline(node: ASTNode, range: TextRange, document: Document, out: MutableList<FoldingDescriptor>) {
        if (range.length < 2) return
        if (document.getLineNumber(range.startOffset) == document.getLineNumber(range.endOffset)) return
        out.add(FoldingDescriptor(node, range))
    }

    /** `import a` `import b` ... folds into `import ...` (from the first path to the end of the last import). */
    private fun imports(file: ASTNode, out: MutableList<FoldingDescriptor>) {
        val imports = file.getChildren(TokenSet.create(SlangTypes.IMPORT_DECL))
        if (imports.size < 2) return
        val first = imports.first()
        val start = first.findChildByType(SlangTypes.QUALIFIED_NAME)?.startOffset ?: return
        // import_decl ends with its (zero-width, automatic) semicolon.
        val end = imports.last().textRange.endOffset
        if (end <= start) return
        val collapsed = CodeFoldingSettings.getInstance().COLLAPSE_IMPORTS
        out.add(FoldingDescriptor(first, TextRange(start, end), null, "...", collapsed, emptySet()))
    }

    override fun getPlaceholderText(node: ASTNode): String =
        if (node.elementType == SlangTokenTypes.BLOCK_COMMENT) "/*...*/" else "{...}"

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private companion object {
        val BRACED = setOf(
            SlangTypes.BLOCK,
            SlangTypes.STRUCT_DECL,
            SlangTypes.INTERFACE_DECL,
            SlangTypes.ENUM_DECL,
            SlangTypes.SWITCH_STMT,
        )
    }
}
