package org.zell.idea.stubs

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.zell.idea.psi.ZellNamedElement

/** Top-level declaration names (struct, interface, enum, func, const) -> declarations. */
class ZellDeclNameIndex : StringStubIndexExtension<ZellNamedElement>() {
    override fun getKey(): StubIndexKey<String, ZellNamedElement> = KEY

    override fun getVersion(): Int = super.getVersion() + 1

    companion object {
        @JvmField
        val KEY: StubIndexKey<String, ZellNamedElement> = StubIndexKey.createIndexKey("zell.decl.name")
    }
}
