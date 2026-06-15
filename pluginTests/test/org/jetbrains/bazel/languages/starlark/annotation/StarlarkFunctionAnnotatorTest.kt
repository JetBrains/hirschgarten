package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject

class StarlarkFunctionAnnotatorTest : StarlarkAnnotatorTestCase() {
  fun testFunctionAnnotator() {
    myFixture.configureByFile("FunctionAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, true, true)
  }

  fun testUnresolvedNamedArgumentsAnnotator() {
    myFixture.configureByFile("UnresolvedNamedArgumentsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }

  fun testUnresolvedNameInGlobalFunction() {
    initializeBazelProject(project, myFixture.tempDirPath)

    myFixture.addFileToProject(
      "MODULE.bazel",
      """
        module(name = "null", version = "0.1.0")
        bazel_dep(name = "rules_java", version = "8.10.0")
      """.trimIndent(),
    )
    myFixture.configureByText(
      "BUILD",
      """
        load("@rules_java//java:defs.bzl", "java_library")

        java_library(
            name = "example",
            srcs = glob(["*"]),
            <error descr="Cannot find a parameter with this name: my_custom_param">my_custom_param = True</error>,
            deps = [],
        )
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  fun testCustomMacroPseudoCollision() {
    initializeBazelProject(project, myFixture.tempDirPath)

    myFixture.addFileToProject(
      "def.bzl",
      """
        load("@rules_java//java:defs.bzl", _alias = "java_library")

        def java_library(
                my_custom_param = False,
                 **kwargs):
            #     my awesome logic based on 'my_custom_param' param value
            _alias(**kwargs)        
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "MODULE.bazel",
      """
        module(name = "null", version = "0.1.0")
        bazel_dep(name = "rules_java", version = "8.10.0")
      """.trimIndent(),
    )
    myFixture.configureByText(
      "BUILD",
      """
        load("//:def.bzl", "java_library")

        java_library(
            name = "example",
            srcs = glob(["*"]),
            my_custom_param = True,
            deps = [],
        )
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  fun testHighlightingForCustomMacro() {
    initializeBazelProject(project, myFixture.tempDirPath)

    myFixture.addFileToProject(
      "def.bzl",
      """
        load("@rules_java//java:defs.bzl", _alias = "java_library")

        def java_library(
                my_custom_param = False,
                 **kwargs):
            #     my awesome logic based on 'my_custom_param' param value
            _alias(**kwargs)        
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "MODULE.bazel",
      """
        module(name = "null", version = "0.1.0")
        bazel_dep(name = "rules_java", version = "8.10.0")
      """.trimIndent(),
    )
    myFixture.configureByText(
      "BUILD",
      """
        load("//:def.bzl", "java_library")

        <info descr="null" textAttributesKey="STARLARK_FUNCTION_DECLARATION">java_library</info>(
            <info descr="null" textAttributesKey="STARLARK_NAMED_ARGUMENT">name</info> <info descr="null" textAttributesKey="STARLARK_NAMED_ARGUMENT">=</info> "example",
            <info descr="null" textAttributesKey="STARLARK_NAMED_ARGUMENT">my_custom_param</info> <info descr="null" textAttributesKey="STARLARK_NAMED_ARGUMENT">=</info> True,
        )
      """.trimIndent(),
    )

    myFixture.checkHighlighting(false, true, false)
  }
}
