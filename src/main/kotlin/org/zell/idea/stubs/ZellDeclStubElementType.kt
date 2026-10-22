package org.zell.idea.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.zell.idea.lang.ZellLanguage
import org.zell.idea.psi.ZellNamedElement
import org.zell.idea.psi.ZellTypes
import org.zell.idea.psi.impl.ZellConstDeclImpl
import org.zell.idea.psi.impl.ZellEnumDeclImpl
import org.zell.idea.psi.impl.ZellFuncDeclImpl
import org.zell.idea.psi.impl.ZellInterfaceDeclImpl
import org.zell.idea.psi.impl.ZellStructDeclImpl

/**
 * Stub element type of the declaration rules. [debugName] equals the
 * constant name in the generated [ZellTypes] (`STRUCT_DECL`, ...), and the
 * external id is `zell.` + debugName, as `stubElementTypeHolder` with
 * `externalIdPrefix="zell."` requires (plugin.xml).
 */
class ZellDeclStubElementType(debugName: String) :
    IStubElementType<ZellDeclStub, ZellNamedElement>(debugName, ZellLanguage) {

    override fun getExternalId(): String = "zell.$debugName"

    // TODO(verify): the generated class names (Grammar-Kit: psiClassPrefix + rule name in
    // CamelCase + psiImplClassSuffix) and their (stub, IStubElementType) constructors,
    // after `./gradlew generateZellParser` (src/main/gen/org/zell/idea/psi/impl).
    override fun createPsi(stub: ZellDeclStub): ZellNamedElement = when (debugName) {
        "STRUCT_DECL" -> ZellStructDeclImpl(stub, this)
        "INTERFACE_DECL" -> ZellInterfaceDeclImpl(stub, this)
        "ENUM_DECL" -> ZellEnumDeclImpl(stub, this)
        "FUNC_DECL" -> ZellFuncDeclImpl(stub, this)
        "CONST_DECL" -> ZellConstDeclImpl(stub, this)
        else -> error("unexpected stub element type $debugName")
    }

    override fun createStub(psi: ZellNamedElement, parentStub: StubElement<out PsiElement>?): ZellDeclStub =
        ZellDeclStub(parentStub, this, psi.name)

    /** Local `const` declarations inside function bodies get no stub. */
    override fun shouldCreateStub(node: ASTNode): Boolean =
        node.elementType != ZellTypes.CONST_DECL || node.treeParent?.elementType == ZellTypes.TOP_LEVEL_DECL

    override fun serialize(stub: ZellDeclStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): ZellDeclStub =
        ZellDeclStub(parentStub, this, dataStream.readNameString())

    /** Only top-level declarations are indexed (methods have the struct's stub as parent). */
    override fun indexStub(stub: ZellDeclStub, sink: IndexSink) {
        val name = stub.name ?: return
        if (stub.parentStub is PsiFileStub<*>) {
            sink.occurrence(ZellDeclNameIndex.KEY, name)
        }
    }
}
