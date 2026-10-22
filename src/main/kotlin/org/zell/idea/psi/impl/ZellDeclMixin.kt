package org.zell.idea.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.IncorrectOperationException
import org.zell.idea.lang.ZellFile
import org.zell.idea.psi.ZellNamedElement
import org.zell.idea.psi.ZellTypes
import org.zell.idea.stubs.ZellDeclKind
import org.zell.idea.stubs.ZellDeclStub
import javax.swing.Icon

/**
 * Base class of the generated `struct_decl`, `interface_decl`, `enum_decl`,
 * `func_decl` and `const_decl` PSI (`mixin` in the grammar header). The
 * three constructors are the ones Grammar-Kit generates for stub-based rules.
 */
abstract class ZellDeclMixin : StubBasedPsiElementBase<ZellDeclStub>, ZellNamedElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: ZellDeclStub, type: IStubElementType<*, *>) : super(stub, type)

    constructor(stub: ZellDeclStub?, type: IElementType?, node: ASTNode?) : super(stub, type, node)

    val kind: ZellDeclKind
        get() = ZellDeclKind.of(elementType)

    override fun getNameIdentifier(): PsiElement? = nameNode(node)?.psi

    override fun getName(): String? = greenStub?.name ?: nameIdentifier?.text

    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("Rename is provided by zell-lsp")

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    override fun getIcon(flags: Int): Icon = kind.icon

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = name

        override fun getLocationString(): String? = (containingFile as? ZellFile)?.namespaceName

        override fun getIcon(unused: Boolean): Icon = kind.icon
    }

    override fun toString(): String = "${javaClass.simpleName}(${elementType})"

    companion object {
        private val NAME_TOKENS = TokenSet.create(ZellTypes.IDENTIFIER, ZellTypes.DEFAULT)

        /**
         * The name token of a declaration node, by the shape of its rule in
         * grammar/zell.bnf (only direct children, so names inside
         * `implements_clause`, generics or bodies are never picked).
         */
        @JvmStatic
        fun nameNode(node: ASTNode): ASTNode? = when (node.elementType) {
            // struct (IDENTIFIER | DEFAULT) ...
            ZellTypes.STRUCT_DECL -> node.findChildByType(NAME_TOKENS)
            // IDENTIFIER? INTERFACE IDENTIFIER ... (the optional first one is `sealed`)
            ZellTypes.INTERFACE_DECL -> {
                var n = node.findChildByType(ZellTypes.INTERFACE)?.treeNext
                while (n != null && n.elementType != ZellTypes.IDENTIFIER && n.elementType != ZellTypes.LBRACE) {
                    n = n.treeNext
                }
                n?.takeIf { it.elementType == ZellTypes.IDENTIFIER }
            }
            // ASYNC? FUNC IDENTIFIER ... inside func_signature
            ZellTypes.FUNC_DECL -> node.findChildByType(ZellTypes.FUNC_SIGNATURE)?.findChildByType(ZellTypes.IDENTIFIER)
            // enum IDENTIFIER ... / const IDENTIFIER ...
            else -> node.findChildByType(ZellTypes.IDENTIFIER)
        }
    }
}
