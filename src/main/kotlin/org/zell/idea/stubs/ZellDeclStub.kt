package org.zell.idea.stubs

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStubBase
import com.intellij.psi.stubs.StubElement
import org.zell.idea.psi.ZellNamedElement

/** Stub of a named declaration: only its name (structure and search by name). */
class ZellDeclStub(parent: StubElement<*>?, type: IStubElementType<*, *>, name: String?) :
    NamedStubBase<ZellNamedElement>(parent, type, name)
