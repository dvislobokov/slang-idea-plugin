package org.slang.idea

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Fold regions: bodies, multi-line comments, import groups (`<fold>` markers in the fixture). */
class SlangFoldingTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/folding"

    fun testFolding() {
        myFixture.testFolding("$testDataPath/folding.sl")
    }
}
