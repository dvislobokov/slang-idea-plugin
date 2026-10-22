package org.slang.idea.stubs

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.slang.idea.psi.SlangNamedElement

/** Top-level declaration names (struct, interface, enum, func, const) -> declarations. */
class SlangDeclNameIndex : StringStubIndexExtension<SlangNamedElement>() {
    override fun getKey(): StubIndexKey<String, SlangNamedElement> = KEY

    override fun getVersion(): Int = super.getVersion() + 1

    companion object {
        @JvmField
        val KEY: StubIndexKey<String, SlangNamedElement> = StubIndexKey.createIndexKey("slang.decl.name")
    }
}
