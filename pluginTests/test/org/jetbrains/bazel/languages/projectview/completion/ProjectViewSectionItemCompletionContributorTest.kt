package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.collections.filter

@RunWith(JUnit4::class)
class ProjectViewSectionItemCompletionContributorTest : BasePlatformTestCase() {
  @Before
  fun setupRootDir() {
    val project = myFixture.project
    project.isBazelProject = true
    project.rootDir = myFixture.tempDirFixture.getFile(".")!!
    if (myFixture.project.bazelProjectDirectoriesEntity() == null) {
      val workspaceModel = project.workspaceModel
      val workspaceModelUrlManager = workspaceModel.getVirtualFileUrlManager()
      runWriteAction {
        workspaceModel.updateProjectModel("Add bazel project directories entity") { storage ->
          storage.addEntity(
            BazelProjectDirectoriesEntity(
              myFixture.project.rootDir.toVirtualFileUrl(workspaceModelUrlManager),
              emptyList(),
              emptyList(),
              false,
              emptyList(),
              BazelProjectEntitySource,
            ),
          )
        }
      }
    }
  }

  @Test
  fun `should complete sharding approach variants`() {
    myFixture.configureByText(".bazelproject", "sharding_approach: <caret>")
    myFixture.type("a")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldContainAll listOf("expand_and_shard", "query_and_shard", "shard_only")
  }

  @Test
  fun `should complete build flags variants`() {
    myFixture.configureByText(".bazelproject", "build_flags:\n  <caret>")
    myFixture.type("action")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    val expectedFlags =
      Flag
        .all()
        .filter {
          it.key.contains("action") &&
            it.value.option.commands.any { cmd ->
              cmd == "build"
            }
        }.map { it.key }

    lookups shouldContainExactlyInAnyOrder expectedFlags
  }

  @Test
  fun `should complete sync flags variants`() {
    myFixture.configureByText(".bazelproject", "sync_flags:\n  <caret>")
    myFixture.type("b")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    val expectedFlags =
      Flag
        .all()
        .filter {
          it.key.contains("b") &&
            it.value.option.commands.any { cmd ->
              cmd == "sync"
            }
        }.map { it.key }

    lookups shouldContainExactlyInAnyOrder expectedFlags
  }

  @Test
  fun `should complete test flags variants`() {
    myFixture.configureByText(".bazelproject", "test_flags:\n  <caret>")
    myFixture.type("action")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    val expectedFlags =
      Flag
        .all()
        .filter {
          it.key.contains("action") &&
            it.value.option.commands.any { cmd ->
              cmd == "test"
            }
        }.map { it.key }

    lookups shouldContainExactlyInAnyOrder expectedFlags
  }

  @Test
  fun `should complete build flags variants with existing flag`() {
    myFixture.configureByText(".bazelproject", "build_flags:\n  --action_cache\n  <caret>")
    myFixture.type("action")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldNotContain "--action_cache"
  }

  @Test
  fun `should complete boolean sections`() {
    myFixture.configureByText(".bazelproject", "shard_sync: <caret>")
    myFixture.type("t")
    myFixture.completeBasic()

    myFixture.checkResult("shard_sync: true")
  }

  @Test
  fun `should complete directories sections`() {
    myFixture.addFileToProject("main/BUILD", "some text")
    myFixture.addFileToProject("module1/BUILD", "some text")
    myFixture.addFileToProject("module2/src/BUILD", "some text")

    myFixture.configureByText(".bazelproject", "directories:\n  <caret>")
    myFixture.type("m")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll listOf("main", "module1", "module2", "module2/src")
  }

  @Test
  fun `should complete excluded directories`() {
    myFixture.addFileToProject("main/BUILD", "some text")
    myFixture.addFileToProject("module1/BUILD", "some text")
    myFixture.addFileToProject("module2/src/BUILD", "some text")

    myFixture.configureByText(".bazelproject", "directories:\n  <caret>")
    myFixture.type("-m")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll listOf("-main", "-module1", "-module2", "-module2/src")
  }

  @Test
  fun `should complete import section`() {
    myFixture.addFileToProject("subpackage/sub.bazelproject", "")
    myFixture.addFileToProject("otherDir/module.bazelproject", "")

    myFixture.configureByText(".bazelproject", "import <caret>")
    myFixture.type("baz")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("subpackage/sub.bazelproject", "otherDir/module.bazelproject")
  }

  @Test
  fun `should complete import run configuration section`() {
    myFixture.addFileToProject("subpackage/config.xml", "")
    myFixture.addFileToProject("otherDir/other_config.xml", "")

    myFixture.configureByText(".bazelproject", "import_run_configurations:\n  <caret>")
    myFixture.type("x")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("subpackage/config.xml", "otherDir/other_config.xml")
  }
}
