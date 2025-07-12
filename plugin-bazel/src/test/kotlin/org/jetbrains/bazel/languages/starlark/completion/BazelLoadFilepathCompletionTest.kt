package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.references.BazelBzlFileService
import org.jetbrains.bazel.languages.starlark.repomapping.injectCanonicalRepoNameToApparentName
import org.jetbrains.bazel.languages.starlark.repomapping.injectCanonicalRepoNameToPath
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelLoadFilepathCompletionTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  @Test
  fun `should complete basic filePath`() {
    myFixture.project.isBazelProject = true

    myFixture.tempDirFixture.createFile("defs_one.bzl", "")
    myFixture.tempDirFixture.createFile("defs_dir/defs_two.bzl", "")

    val myRootPath = myFixture.tempDirFixture.tempDirPath.toNioPathOrNull()!!
    val newRepoNameToPathMap = mapOf("" to myRootPath)
    myFixture.project.injectCanonicalRepoNameToPath(newRepoNameToPathMap)
    BazelBzlFileService.getInstance(myFixture.project).forceUpdateCache()

    myFixture.configureByText("BUILD.bazel", """load("//:<caret>)""")
    myFixture.type("d")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("\"//:defs_one.bzl\"", "\"//defs_dir:defs_two.bzl\"")
  }

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `should complete external filePath`() {
    myFixture.project.isBazelProject = true

    val rulesDir = tempFolder.newFolder("rules_kotlin")
    tempFolder.newFile("rules_kotlin/MODULE.bazel")
    tempFolder.newFile("rules_kotlin/BUILD.bazel")
    tempFolder.newFolder("rules_kotlin/package")
    tempFolder.newFile("rules_kotlin/kt_jvm_library.bzl")
    tempFolder.newFile("rules_kotlin/package/kt_jvm_binary.bzl")

    val newRepoNameToPathMap = mapOf("rules_kotlin" to rulesDir.path.toNioPathOrNull()!!)
    val newCanonicalRepoNameToApparentName = mapOf("rules_kotlin" to "rules_kotlin")
    myFixture.project.injectCanonicalRepoNameToPath(newRepoNameToPathMap)
    myFixture.project.injectCanonicalRepoNameToApparentName(newCanonicalRepoNameToApparentName)
    BazelBzlFileService.getInstance(myFixture.project).forceUpdateCache()

    myFixture.configureByText("BUILD.bazel", """load("//:<caret>)""")
    myFixture.type("kt")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"@rules_kotlin//:kt_jvm_library.bzl\"",
        "\"@rules_kotlin//package:kt_jvm_binary.bzl\"",
      )
  }
}
