package org.zell.idea.structure

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
import org.zell.idea.lang.ZellFile
import org.zell.idea.lang.ZellIcons
import org.zell.idea.psi.ZellTypes
import org.zell.idea.psi.impl.ZellDeclMixin
import javax.swing.Icon

/**
 * Structure view: the file node shows the namespace; below it the top-level
 * declarations; structs list fields, embeds and methods, interfaces their
 * methods and embeds, enums their variants and methods. Built from the PSI
 * only (names match zell-lsp document symbols, which use the same syntax).
 */
class ZellStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is ZellFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel = ZellStructureViewModel(psiFile, editor)
        }
    }
}

class ZellStructureViewModel(file: ZellFile, editor: Editor?) :
    StructureViewModelBase(file, editor, ZellStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(ZellDeclMixin::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        val e = element as? ZellStructureViewElement ?: return false
        return e.element !is ZellFile && e.node?.elementType !in CONTAINERS
    }

    private companion object {
        val CONTAINERS = setOf(ZellTypes.STRUCT_DECL, ZellTypes.INTERFACE_DECL, ZellTypes.ENUM_DECL)
    }
}

/** A structure node over a PSI element; what it shows depends on the grammar rule. */
class ZellStructureViewElement(element: PsiElement) : PsiTreeElementBase<PsiElement>(element) {
    val node: ASTNode?
        get() = element?.node

    override fun getPresentableText(): String? {
        val element = element ?: return null
        if (element is ZellFile) return element.namespaceName?.let { "namespace $it" } ?: element.name
        val node = element.node
        return when (node.elementType) {
            ZellTypes.FUNC_DECL -> node.findChildByType(ZellTypes.FUNC_SIGNATURE)?.let(::signature)
            ZellTypes.FUNC_SIGNATURE -> signature(node)
            ZellTypes.EXTERN_FUNC_DECL -> node.findChildByType(ZellTypes.FUNC_SIGNATURE)?.let(::signature)
            ZellTypes.FIELD_DECL, ZellTypes.VAR_DECL -> compact(node.text.substringBefore('=').removePrefix("var "))
            ZellTypes.EMBED_DECL, ZellTypes.NAMED_TYPE -> compact(node.text)
            ZellTypes.ENUM_VARIANT -> node.findChildByType(ZellTypes.IDENTIFIER)?.let { v ->
                val params = node.findChildByType(ZellTypes.PARAM_LIST)
                if (params != null) "${v.text}(${compact(params.text)})" else v.text
            }
            else -> (element as? ZellDeclMixin)?.name
        }
    }

    override fun getIcon(open: Boolean): Icon? {
        val element = element ?: return null
        if (element is ZellFile) return ZellIcons.FILE
        return when (element.node.elementType) {
            ZellTypes.FIELD_DECL -> AllIcons.Nodes.Field
            ZellTypes.EMBED_DECL, ZellTypes.NAMED_TYPE -> AllIcons.Nodes.Interface
            ZellTypes.ENUM_VARIANT -> AllIcons.Nodes.Field
            ZellTypes.VAR_DECL -> AllIcons.Nodes.Variable
            ZellTypes.FUNC_SIGNATURE, ZellTypes.EXTERN_FUNC_DECL -> AllIcons.Nodes.Method
            ZellTypes.FUNC_DECL ->
                if (element.node.treeParent?.elementType == ZellTypes.TOP_LEVEL_DECL) AllIcons.Nodes.Function else AllIcons.Nodes.Method
            else -> (element as? ZellDeclMixin)?.kind?.icon
        }
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val element = element ?: return emptyList()
        val node = element.node
        val children = ArrayList<PsiElement>()
        when {
            element is ZellFile -> for (top in node.getChildren(null)) {
                if (top.elementType != ZellTypes.TOP_LEVEL_DECL) continue
                top.getChildren(null).firstOrNull { it.elementType in TOP_LEVEL }?.psi?.let(children::add)
            }
            node.elementType == ZellTypes.STRUCT_DECL -> for (member in node.getChildren(null)) {
                if (member.elementType != ZellTypes.STRUCT_MEMBER) continue
                val m = member.getChildren(null).firstOrNull { it.elementType in MEMBERS } ?: continue
                // method_decl ::= STATIC? func_decl
                val target = if (m.elementType == ZellTypes.METHOD_DECL) m.findChildByType(ZellTypes.FUNC_DECL) else m
                target?.psi?.let(children::add)
            }
            node.elementType == ZellTypes.INTERFACE_DECL -> for (c in node.getChildren(null)) {
                if (c.elementType == ZellTypes.FUNC_SIGNATURE || c.elementType == ZellTypes.NAMED_TYPE) children.add(c.psi)
            }
            node.elementType == ZellTypes.ENUM_DECL -> for (c in node.getChildren(null)) {
                when (c.elementType) {
                    ZellTypes.ENUM_VARIANT -> children.add(c.psi)
                    ZellTypes.METHOD_DECL -> c.findChildByType(ZellTypes.FUNC_DECL)?.psi?.let(children::add)
                    ZellTypes.FUNC_DECL -> children.add(c.psi)
                }
            }
        }
        return children.map(::ZellStructureViewElement)
    }

    private companion object {
        val TOP_LEVEL: Set<IElementType> = setOf(
            ZellTypes.STRUCT_DECL, ZellTypes.INTERFACE_DECL, ZellTypes.ENUM_DECL, ZellTypes.FUNC_DECL,
            ZellTypes.EXTERN_FUNC_DECL, ZellTypes.VAR_DECL, ZellTypes.CONST_DECL,
        )
        val MEMBERS: Set<IElementType> = setOf(ZellTypes.METHOD_DECL, ZellTypes.FIELD_DECL, ZellTypes.EMBED_DECL)

        /** `Area() float` for `func Area() float` (the signature without `async func`). */
        fun signature(sig: ASTNode): String {
            val name = sig.findChildByType(ZellTypes.IDENTIFIER) ?: return compact(sig.text)
            val text = sig.text.substring(name.startOffset - sig.startOffset)
            return compact(text)
        }

        fun compact(text: String): String = text.trim().replace(Regex("\\s+"), " ")
    }
}
