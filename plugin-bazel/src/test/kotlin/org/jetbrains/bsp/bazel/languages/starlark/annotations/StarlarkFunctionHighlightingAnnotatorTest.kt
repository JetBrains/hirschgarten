package org.jetbrains.bsp.bazel.languages.starlark.annotations

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StarlarkFunctionHighlightingAnnotatorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/starlark/annotations"

    fun testAnnotator() {
        myFixture.configureByFile("FunctionHighlightingAnnotatorTestData.bzl")
        myFixture.checkHighlighting(true, true, true)
    }
}
