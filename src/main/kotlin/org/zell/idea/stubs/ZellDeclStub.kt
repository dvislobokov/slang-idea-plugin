package org.slang.idea.stubs

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStubBase
import com.intellij.psi.stubs.StubElement
import org.slang.idea.psi.SlangNamedElement

/** Stub of a named declaration: only its name (structure and search by name). */
class SlangDeclStub(parent: StubElement<*>?, type: IStubElementType<*, *>, name: String?) :
    NamedStubBase<SlangNamedElement>(parent, type, name)
