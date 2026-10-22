package org.zell.idea

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.util.Disposer
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.zell.idea.lang.ZellFile
import org.zell.idea.psi.ZellNamedElement
import org.zell.idea.stubs.ZellDeclNameIndex
import org.zell.idea.structure.ZellStructureViewModel

/** Structure view tree and the top-level declaration stub index. */
class ZellStructureViewTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/structure"

    fun testStructure() {
        val file = myFixture.configureByFile("shapes.zl") as ZellFile
        val model = ZellStructureViewModel(file, null)
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
        myFixture.configureByFile("shapes.zl")
        val scope = GlobalSearchScope.allScope(project)
        val keys = StubIndex.getInstance().getAllKeys(ZellDeclNameIndex.KEY, project)
        for (name in listOf("Limit", "Shape", "Point", "Color", "main")) {
            assertTrue("$name not indexed: $keys", name in keys)
            val found = StubIndex.getElements(ZellDeclNameIndex.KEY, name, project, scope, ZellNamedElement::class.java)
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
