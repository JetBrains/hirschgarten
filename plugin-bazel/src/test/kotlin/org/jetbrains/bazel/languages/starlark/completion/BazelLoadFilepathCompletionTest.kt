package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelLoadFilepathCompletionTest : BasePlatformTestCase() {
  @Test
  fun `should complete basic filePath` ()  {
    project.isBazelProject = true
    project.rootDir = myFixture.tempDirFixture.getFile(".")!!

    myFixture.addFileToProject("defs_one.bzl", "")
    myFixture.addFileToProject("defs_dir/defs_two.bzl", "")

    myFixture.configureByText("BUILD", """load("//:<caret>)""")
    myFixture.type("d")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"//:defs_one.bzl\"", "\"//defs_dir:defs_two.bzl\"")
  }
}
