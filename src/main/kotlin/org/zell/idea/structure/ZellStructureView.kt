package org.slang.idea.structure

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.slang.idea.lang.SlangFile
import org.slang.idea.lang.SlangIcons
import org.slang.idea.psi.SlangTypes
import org.slang.idea.psi.impl.SlangDeclMixin
import javax.swing.Icon

/**
 * Structure view: the file node shows the namespace; below it the top-level
 * declarations; structs list fields, embeds and methods, interfaces their
 * methods and embeds, enums their variants and methods. Built from the PSI
 * only (names match slang-lsp document symbols, which use the same syntax).
 */
class SlangStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is SlangFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel = SlangStructureViewModel(psiFile, editor)
        }
    }
}

class SlangStructureViewModel(file: SlangFile, editor: Editor?) :
    StructureViewModelBase(file, editor, SlangStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(SlangDeclMixin::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        val e = element as? SlangStructureViewElement ?: return false
        return e.element !is SlangFile && e.node?.elementType !in CONTAINERS
    }

    private companion object {
        val CONTAINERS = setOf(SlangTypes.STRUCT_DECL, SlangTypes.INTERFACE_DECL, SlangTypes.ENUM_DECL)
    }
}

/** A structure node over a PSI element; what it shows depends on the grammar rule. */
class SlangStructureViewElement(element: PsiElement) : PsiTreeElementBase<PsiElement>(element) {
    val node: ASTNode?
        get() = element?.node

    override fun getPresentableText(): String? {
        val element = element ?: return null
        if (element is SlangFile) return element.namespaceName?.let { "namespace $it" } ?: element.name
        val node = element.node
        return when (node.elementType) {
            SlangTypes.FUNC_DECL -> node.findChildByType(SlangTypes.FUNC_SIGNATURE)?.let(::signature)
            SlangTypes.FUNC_SIGNATURE -> signature(node)
            SlangTypes.EXTERN_FUNC_DECL -> node.findChildByType(SlangTypes.FUNC_SIGNATURE)?.let(::signature)
            SlangTypes.FIELD_DECL, SlangTypes.VAR_DECL -> compact(node.text.substringBefore('=').removePrefix("var "))
            SlangTypes.EMBED_DECL, SlangTypes.NAMED_TYPE -> compact(node.text)
            SlangTypes.ENUM_VARIANT -> node.findChildByType(SlangTypes.IDENTIFIER)?.let { v ->
                val params = node.findChildByType(SlangTypes.PARAM_LIST)
                if (params != null) "${v.text}(${compact(params.text)})" else v.text
            }
            else -> (element as? SlangDeclMixin)?.name
        }
    }

    override fun getIcon(open: Boolean): Icon? {
        val element = element ?: return null
        if (element is SlangFile) return SlangIcons.FILE
        return when (element.node.elementType) {
            SlangTypes.FIELD_DECL -> AllIcons.Nodes.Field
            SlangTypes.EMBED_DECL, SlangTypes.NAMED_TYPE -> AllIcons.Nodes.Interface
            SlangTypes.ENUM_VARIANT -> AllIcons.Nodes.Field
            SlangTypes.VAR_DECL -> AllIcons.Nodes.Variable
            SlangTypes.FUNC_SIGNATURE, SlangTypes.EXTERN_FUNC_DECL -> AllIcons.Nodes.Method
            SlangTypes.FUNC_DECL ->
                if (element.node.treeParent?.elementType == SlangTypes.TOP_LEVEL_DECL) AllIcons.Nodes.Function else AllIcons.Nodes.Method
            else -> (element as? SlangDeclMixin)?.kind?.icon
        }
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val element = element ?: return emptyList()
        val node = element.node
        val children = ArrayList<PsiElement>()
        when {
            element is SlangFile -> for (top in node.getChildren(null)) {
                if (top.elementType != SlangTypes.TOP_LEVEL_DECL) continue
                top.getChildren(null).firstOrNull { it.elementType in TOP_LEVEL }?.psi?.let(children::add)
            }
            node.elementType == SlangTypes.STRUCT_DECL -> for (member in node.getChildren(null)) {
                if (member.elementType != SlangTypes.STRUCT_MEMBER) continue
                val m = member.getChildren(null).firstOrNull { it.elementType in MEMBERS } ?: continue
                // method_decl ::= STATIC? func_decl
                val target = if (m.elementType == SlangTypes.METHOD_DECL) m.findChildByType(SlangTypes.FUNC_DECL) else m
                target?.psi?.let(children::add)
            }
            node.elementType == SlangTypes.INTERFACE_DECL -> for (c in node.getChildren(null)) {
                if (c.elementType == SlangTypes.FUNC_SIGNATURE || c.elementType == SlangTypes.NAMED_TYPE) children.add(c.psi)
            }
            node.elementType == SlangTypes.ENUM_DECL -> for (c in node.getChildren(null)) {
                when (c.elementType) {
                    SlangTypes.ENUM_VARIANT -> children.add(c.psi)
                    SlangTypes.METHOD_DECL -> c.findChildByType(SlangTypes.FUNC_DECL)?.psi?.let(children::add)
                    SlangTypes.FUNC_DECL -> children.add(c.psi)
                }
            }
        }
        return children.map(::SlangStructureViewElement)
    }

    private companion object {
        val TOP_LEVEL: Set<IElementType> = setOf(
            SlangTypes.STRUCT_DECL, SlangTypes.INTERFACE_DECL, SlangTypes.ENUM_DECL, SlangTypes.FUNC_DECL,
            SlangTypes.EXTERN_FUNC_DECL, SlangTypes.VAR_DECL, SlangTypes.CONST_DECL,
        )
        val MEMBERS: Set<IElementType> = setOf(SlangTypes.METHOD_DECL, SlangTypes.FIELD_DECL, SlangTypes.EMBED_DECL)

        /** `Area() float` for `func Area() float` (the signature without `async func`). */
        fun signature(sig: ASTNode): String {
            val name = sig.findChildByType(SlangTypes.IDENTIFIER) ?: return compact(sig.text)
            val text = sig.text.substring(name.startOffset - sig.startOffset)
            return compact(text)
        }

        fun compact(text: String): String = text.trim().replace(Regex("\\s+"), " ")
    }
}
