package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StarlarkCodeInsightTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/starlark"

    fun testAnnotator() {
        myFixture.configureByFile("AnnotatorTestData.bzl")
        myFixture.checkHighlighting(true, true, true)
    }
}