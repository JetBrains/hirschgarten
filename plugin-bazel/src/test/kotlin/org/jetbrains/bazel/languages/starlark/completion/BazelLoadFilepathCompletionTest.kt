package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.references.getCanonicalRepoNameToBzlFiles
import org.jetbrains.bazel.languages.starlark.repomapping.injectCanonicalRepoNameToApparentName
import org.jetbrains.bazel.languages.starlark.repomapping.injectCanonicalRepoNameToPath
import org.jetbrains.bazel.sdkcompat.indexingTestUtilForceSkipWaiting
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectEntitySource
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelLoadFilepathCompletionTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  @Test
  fun `should complete basic filePath`() {
    myFixture.tempDirFixture.createFile("defs_one.bzl", "")
    myFixture.tempDirFixture.createFile("defs_dir/defs_two.bzl", "")

    val myRootPath = myFixture.tempDirFixture.tempDirPath.toNioPathOrNull()!!

    val newRepoNameToPathMap = mapOf("" to myRootPath)
    project.injectCanonicalRepoNameToPath(newRepoNameToPathMap)
    getCanonicalRepoNameToBzlFiles(project)
    indexingTestUtilForceSkipWaiting()

    myFixture.configureByText("BUILD.bazel", """load("//:<caret>)""")
    myFixture.type("d")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll listOf("\"//:defs_one.bzl\"", "\"//defs_dir:defs_two.bzl\"")
  }

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `should complete external filePath`() {
    val rulesDir = tempFolder.newFolder("rules_kotlin")
    tempFolder.newFile("rules_kotlin/MODULE.bazel")
    tempFolder.newFile("rules_kotlin/BUILD.bazel")
    tempFolder.newFolder("rules_kotlin/package")
    tempFolder.newFile("rules_kotlin/kt_jvm_library.bzl")
    tempFolder.newFile("rules_kotlin/package/kt_jvm_binary.bzl")

    val newRepoNameToPathMap = mapOf("rules_kotlin" to rulesDir.path.toNioPathOrNull()!!)
    val newCanonicalRepoNameToApparentName = mapOf("rules_kotlin" to "rules_kotlin")
    project.injectCanonicalRepoNameToPath(newRepoNameToPathMap)
    project.injectCanonicalRepoNameToApparentName(newCanonicalRepoNameToApparentName)
    getCanonicalRepoNameToBzlFiles(project)

    myFixture.configureByText("BUILD.bazel", """load("//:<caret>)""")
    myFixture.type("kt")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll
      listOf(
        "\"@rules_kotlin//:kt_jvm_library.bzl\"",
        "\"@rules_kotlin//package:kt_jvm_binary.bzl\"",
      )
  }

  @Test
  fun `should complete for use_extension`() {
    val rulesDir = tempFolder.newFolder("rules_jvm_external")
    tempFolder.newFile("rules_jvm_external/MODULE.bazel")
    tempFolder.newFile("rules_jvm_external/BUILD.bazel")
    tempFolder.newFile("rules_jvm_external/defs.bzl")
    tempFolder.newFile("rules_jvm_external/extensions.bzl")

    val newRepoNameToPathMap = mapOf("rules_jvm_external" to rulesDir.path.toNioPathOrNull()!!)
    val newCanonicalRepoNameToApparentName = mapOf("rules_jvm_external" to "rules_jvm_external")
    project.injectCanonicalRepoNameToPath(newRepoNameToPathMap)
    project.injectCanonicalRepoNameToApparentName(newCanonicalRepoNameToApparentName)
    getCanonicalRepoNameToBzlFiles(project)

    myFixture.configureByText("MODULE.bazel", """maven = use_extension("<caret>)""")
    myFixture.type("@ru")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll
      listOf(
        "\"@rules_jvm_external//:extensions.bzl\"",
        "\"@rules_jvm_external//:defs.bzl\"",
      )
  }


  override fun setUp() {
    super.setUp()
    prepareProject()
  }

  fun prepareProject() {
    project.isBazelProject = true
    val myRootPath = myFixture.tempDirFixture.tempDirPath.toNioPathOrNull()!!

    val myRootPathUrl = myRootPath.toVirtualFileUrl(project.workspaceModel.getVirtualFileUrlManager())

    val entity =
      BazelProjectDirectoriesEntity(
        projectRoot = myRootPathUrl,
        includedRoots = listOf(myRootPathUrl.append("excluded")),
        excludedRoots = emptyList(),
        indexAllFilesInIncludedRoots = false,
        indexAdditionalFiles = emptyList(),
        entitySource = BazelProjectEntitySource,
      )

    WriteCommandAction.runWriteCommandAction(project) {
      project.workspaceModel.updateProjectModel("add BazelProjectDirectoriesEntity") { it.addEntity(entity) }
    }

    indexingTestUtilForceSkipWaiting()
  }
}
