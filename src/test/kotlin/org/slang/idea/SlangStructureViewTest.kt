package org.slang.idea

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.util.Disposer
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.slang.idea.lang.SlangFile
import org.slang.idea.psi.SlangNamedElement
import org.slang.idea.stubs.SlangDeclNameIndex
import org.slang.idea.structure.SlangStructureViewModel

/** Structure view tree and the top-level declaration stub index. */
class SlangStructureViewTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/structure"

    fun testStructure() {
        val file = myFixture.configureByFile("shapes.sl") as SlangFile
        val model = SlangStructureViewModel(file, null)
        try {
            val actual = StringBuilder()
            dump(model.root, 0, actual)
            assertEquals(
                """
                namespace examples.shapes
                  Limit
                  Shape
                    Area() float
                    Named
                  Point
                    X float
                    Y float
                    Area() float
                    Origin() Point
                  Color
                    Red
                    Rgb(r, g, b int)
                    Name() string
                  main()
                """.trimIndent(),
                actual.toString().trimEnd(),
            )
        } finally {
            Disposer.dispose(model)
        }
    }

    fun testTopLevelDeclarationsAreIndexed() {
        myFixture.configureByFile("shapes.sl")
        val scope = GlobalSearchScope.allScope(project)
        val keys = StubIndex.getInstance().getAllKeys(SlangDeclNameIndex.KEY, project)
        for (name in listOf("Limit", "Shape", "Point", "Color", "main")) {
            assertTrue("$name not indexed: $keys", name in keys)
            val found = StubIndex.getElements(SlangDeclNameIndex.KEY, name, project, scope, SlangNamedElement::class.java)
            assertEquals(name, found.single().name)
        }
        // Methods are not top-level.
        assertFalse("Area" in keys)
    }

    private fun dump(element: StructureViewTreeElement, depth: Int, out: StringBuilder) {
        out.append("  ".repeat(depth)).append(element.presentation.presentableText).append('\n')
        for (child in element.children) dump(child as StructureViewTreeElement, depth + 1, out)
    }
}
