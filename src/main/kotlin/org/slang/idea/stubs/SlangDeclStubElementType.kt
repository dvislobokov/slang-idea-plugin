package org.slang.idea.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.slang.idea.lang.SlangLanguage
import org.slang.idea.psi.SlangNamedElement
import org.slang.idea.psi.SlangTypes
import org.slang.idea.psi.impl.SlangConstDeclImpl
import org.slang.idea.psi.impl.SlangEnumDeclImpl
import org.slang.idea.psi.impl.SlangFuncDeclImpl
import org.slang.idea.psi.impl.SlangInterfaceDeclImpl
import org.slang.idea.psi.impl.SlangStructDeclImpl

/**
 * Stub element type of the declaration rules. [debugName] equals the
 * constant name in the generated [SlangTypes] (`STRUCT_DECL`, ...), and the
 * external id is `slang.` + debugName, as `stubElementTypeHolder` with
 * `externalIdPrefix="slang."` requires (plugin.xml).
 */
class SlangDeclStubElementType(debugName: String) :
    IStubElementType<SlangDeclStub, SlangNamedElement>(debugName, SlangLanguage) {

    override fun getExternalId(): String = "slang.$debugName"

    override fun createPsi(stub: SlangDeclStub): SlangNamedElement = when (debugName) {
        "STRUCT_DECL" -> SlangStructDeclImpl(stub, this)
        "INTERFACE_DECL" -> SlangInterfaceDeclImpl(stub, this)
        "ENUM_DECL" -> SlangEnumDeclImpl(stub, this)
        "FUNC_DECL" -> SlangFuncDeclImpl(stub, this)
        "CONST_DECL" -> SlangConstDeclImpl(stub, this)
        else -> error("unexpected stub element type $debugName")
    }

    override fun createStub(psi: SlangNamedElement, parentStub: StubElement<out PsiElement>?): SlangDeclStub =
        SlangDeclStub(parentStub, this, psi.name)

    /** Local `const` declarations inside function bodies get no stub. */
    override fun shouldCreateStub(node: ASTNode): Boolean =
        node.elementType != SlangTypes.CONST_DECL || node.treeParent?.elementType == SlangTypes.TOP_LEVEL_DECL

    override fun serialize(stub: SlangDeclStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): SlangDeclStub =
        SlangDeclStub(parentStub, this, dataStream.readNameString())

    /** Only top-level declarations are indexed (methods have the struct's stub as parent). */
    override fun indexStub(stub: SlangDeclStub, sink: IndexSink) {
        val name = stub.name ?: return
        if (stub.parentStub is PsiFileStub<*>) {
            sink.occurrence(SlangDeclNameIndex.KEY, name)
        }
    }
}
