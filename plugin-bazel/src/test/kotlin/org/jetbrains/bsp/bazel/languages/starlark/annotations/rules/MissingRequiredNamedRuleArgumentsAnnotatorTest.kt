package org.jetbrains.bsp.bazel.languages.starlark.annotations.rules

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bsp.bazel.languages.starlark.build.info.ProjectBuildLanguageInfoService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MissingRequiredNamedRuleArgumentsAnnotatorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/starlark/annotations/rules"

    @Before
    fun beforeTest() {
        ProjectBuildLanguageInfoService.getInstance(project).info = Build.BuildLanguage.newBuilder()
            .addRule(Build.RuleDefinition.newBuilder()
                .setName("rule")
                .addAttribute(Build.AttributeDefinition.newBuilder()
                    .setName("mandatoryArg")
                    .setMandatory(true)
                    .setType(Build.Attribute.Discriminator.LABEL)
                    .build()
                )
                .addAttribute(Build.AttributeDefinition.newBuilder()
                    .setName("nonMandatoryArg")
                    .setMandatory(false)
                    .setType(Build.Attribute.Discriminator.LABEL)
                    .build()
                )
                .build()
            ).build()
    }

    @Test
    fun testAnnotator() {
        myFixture.configureByFile("MissingRequiredNamedRuleArgumentsAnnotatorTestData.bzl")
        myFixture.checkHighlighting(true, false, true, false)
    }
}
