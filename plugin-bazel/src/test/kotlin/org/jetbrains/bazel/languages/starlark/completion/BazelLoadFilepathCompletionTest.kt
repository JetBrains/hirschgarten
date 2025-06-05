package org.jetbrains.bazel.languages.starlark.completion

import ai.grazie.annotation.TestOnly
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.repomapping.injectCanonicalRepoNameToPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelLoadFilepathCompletionTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {

  @OptIn(TestOnly::class)
  @Test
  fun `should complete basic filePath`() {
    myFixture.project.isBazelProject = true

    myFixture.tempDirFixture.createFile("defs_one.bzl", "")
    myFixture.tempDirFixture.createFile("defs_dir/defs_two.bzl", "")

    val myRootPath = myFixture.tempDirFixture.tempDirPath.toNioPathOrNull()!!
    val newRepoNameToPathMap = mapOf("" to myRootPath)
    myFixture.project.injectCanonicalRepoNameToPath(newRepoNameToPathMap)

    myFixture.configureByText("BUILD.bazel", """load("//:<caret>)""")
    myFixture.type("d")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"//:defs_one.bzl\"", "\"//defs_dir:defs_two.bzl\"")
  }
}
