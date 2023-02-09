package org.jetbrains.bsp.bazel.languages.starlark.annotations.rules

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DuplicatedNamedRuleArgumentAnnotatorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/starlark/annotations/rules"

    fun testAnnotator() {
        myFixture.configureByFile("DuplicatedNamedRuleArgumentAnnotatorTestData.bzl")
        myFixture.checkHighlighting(true, false, true, false)
    }
}