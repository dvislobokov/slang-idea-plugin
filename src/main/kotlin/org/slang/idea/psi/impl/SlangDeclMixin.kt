package org.slang.idea.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.IncorrectOperationException
import org.slang.idea.lang.SlangFile
import org.slang.idea.psi.SlangNamedElement
import org.slang.idea.psi.SlangTypes
import org.slang.idea.stubs.SlangDeclKind
import org.slang.idea.stubs.SlangDeclStub
import javax.swing.Icon

/**
 * Base class of the generated `struct_decl`, `interface_decl`, `enum_decl`,
 * `func_decl` and `const_decl` PSI (`mixin` in the grammar header). The
 * three constructors are the ones Grammar-Kit generates for stub-based rules.
 */
abstract class SlangDeclMixin : StubBasedPsiElementBase<SlangDeclStub>, SlangNamedElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: SlangDeclStub, type: IStubElementType<*, *>) : super(stub, type)

    constructor(stub: SlangDeclStub?, type: IElementType?, node: ASTNode?) : super(stub, type, node)

    val kind: SlangDeclKind
        get() = SlangDeclKind.of(elementType)

    override fun getNameIdentifier(): PsiElement? = nameNode(node)?.psi

    override fun getName(): String? = greenStub?.name ?: nameIdentifier?.text

    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("Rename is provided by slang-lsp")

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    override fun getIcon(flags: Int): Icon = kind.icon

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = name

        override fun getLocationString(): String? = (containingFile as? SlangFile)?.namespaceName

        override fun getIcon(unused: Boolean): Icon = kind.icon
    }

    override fun toString(): String = "${javaClass.simpleName}(${elementType})"

    companion object {
        private val NAME_TOKENS = TokenSet.create(SlangTypes.IDENTIFIER, SlangTypes.DEFAULT)

        /**
         * The name token of a declaration node, by the shape of its rule in
         * grammar/slang.bnf (only direct children, so names inside
         * `implements_clause`, generics or bodies are never picked).
         */
        @JvmStatic
        fun nameNode(node: ASTNode): ASTNode? = when (node.elementType) {
            // struct (IDENTIFIER | DEFAULT) ...
            SlangTypes.STRUCT_DECL -> node.findChildByType(NAME_TOKENS)
            // IDENTIFIER? INTERFACE IDENTIFIER ... (the optional first one is `sealed`)
            SlangTypes.INTERFACE_DECL -> {
                var n = node.findChildByType(SlangTypes.INTERFACE)?.treeNext
                while (n != null && n.elementType != SlangTypes.IDENTIFIER && n.elementType != SlangTypes.LBRACE) {
                    n = n.treeNext
                }
                n?.takeIf { it.elementType == SlangTypes.IDENTIFIER }
            }
            // ASYNC? FUNC IDENTIFIER ... inside func_signature
            SlangTypes.FUNC_DECL -> node.findChildByType(SlangTypes.FUNC_SIGNATURE)?.findChildByType(SlangTypes.IDENTIFIER)
            // enum IDENTIFIER ... / const IDENTIFIER ...
            else -> node.findChildByType(SlangTypes.IDENTIFIER)
        }
    }
}
