package org.zell.idea

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Fold regions: bodies, multi-line comments, import groups (`<fold>` markers in the fixture). */
class ZellFoldingTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/folding"

    fun testFolding() {
        myFixture.testFolding("$testDataPath/folding.zl")
    }
}
