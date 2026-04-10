package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@TestApplication
class ProjectViewSectionItemCompletionContributorTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture, openAfterCreation = true)
  private val project by projectFixture

  // Module fixture is unused by the test code, but the code insight fixture requires it to work.
  private val moduleFixture = projectFixture.moduleFixture()

  private val codeInsightFixture by codeInsightFixture(projectFixture, tempDirFixture)

  @BeforeEach
  fun setupRootDir() {
    project.isBazelProject = true
    project.rootDir = tempDir.refreshAndFindVirtualDirectory()!!
    if (project.bazelProjectDirectoriesEntity() == null) {
      val workspaceModel = project.workspaceModel
      val workspaceModelUrlManager = workspaceModel.getVirtualFileUrlManager()
      runWriteAction {
        workspaceModel.updateProjectModel("Add bazel project directories entity") { storage ->
          storage.addEntity(
            BazelProjectDirectoriesEntity(
              project.rootDir.toVirtualFileUrl(workspaceModelUrlManager),
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
    codeInsightFixture.configureByText(".bazelproject", "sharding_approach: <caret>")
    codeInsightFixture.type("a")

    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldContainAll listOf("expand_and_shard", "query_and_shard", "shard_only")
  }

  @Test
  fun `should complete build flags variants`() {
    codeInsightFixture.configureByText(".bazelproject", "build_flags:\n  <caret>")
    codeInsightFixture.type("action")

    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }
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
    codeInsightFixture.configureByText(".bazelproject", "sync_flags:\n  <caret>")
    codeInsightFixture.type("b")

    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }
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
    codeInsightFixture.configureByText(".bazelproject", "test_flags:\n  <caret>")
    codeInsightFixture.type("action")

    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }
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
  fun `should complete debug flags variants`() {
    codeInsightFixture.configureByText(".bazelproject", "debug_flags:\n  <caret>")
    codeInsightFixture.type("action")

    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }
    val expectedFlags =
      Flag
        .all()
        .filter {
          it.key.contains("action") &&
          it.value.option.commands.any { cmd ->
            cmd == "run"
          } &&
          it.value.option.commands.any { cmd ->
            cmd == "test"
          }
        }.map { it.key }

    lookups shouldContainExactlyInAnyOrder expectedFlags
  }


  @Test
  fun `should complete python debug flags variants`() {
    codeInsightFixture.configureByText(".bazelproject", "python_debug_flags:\n  <caret>")
    codeInsightFixture.type("action")

    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }
    val expectedFlags =
      Flag
        .all()
        .filter {
          it.key.contains("action") &&
          it.value.option.commands.any { cmd ->
            cmd == "run"
          } &&
          it.value.option.commands.any { cmd ->
            cmd == "test"
          }
        }.map { it.key }

    lookups shouldContainExactlyInAnyOrder expectedFlags
  }

  @Test
  fun `should complete build flags variants with existing flag`() {
    codeInsightFixture.configureByText(".bazelproject", "build_flags:\n  --action_cache\n  <caret>")
    codeInsightFixture.type("action")

    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldNotContain "--action_cache"
  }

  @Test
  fun `should complete boolean sections`() {
    codeInsightFixture.configureByText(".bazelproject", "shard_sync: <caret>")
    codeInsightFixture.type("t")
    codeInsightFixture.completeBasic()

    codeInsightFixture.checkResult("shard_sync: true")
  }

  @ParameterizedTest
  @ValueSource(strings = ["m", "-m"])
  fun `should provide completion suggestions in directories section`(incompletePath: String) {
    // GIVEN
    codeInsightFixture.addFileToProject("main/BUILD", "some text")
    codeInsightFixture.addFileToProject("module1/BUILD", "some text")
    codeInsightFixture.addFileToProject("module2/src/BUILD", "some text")
    codeInsightFixture.configureByText(".bazelproject", "directories:\n  $incompletePath<caret>")

    // WHEN code completion returned list of suggestions
    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }

    // THEN
    lookups shouldContainAll listOf("main", "module1", "module2", "module2/src")
  }

  @ParameterizedTest
  @CsvSource(value = [
    "rub, ruby",
    "-rub, -ruby",
    ".claud, .claude",
    "-.claud, -.claude",
  ])
  fun `should autocomplete paths in directories section`(incompletePath: String, expectedPath: String) {
    // GIVEN
    codeInsightFixture.addFileToProject(".claude/README.md", "some text")
    codeInsightFixture.addFileToProject("ruby/BUILD", "some text")
    codeInsightFixture.configureByText(".bazelproject", "directories:\n  $incompletePath<caret>")

    // WHEN code completion applied the only suggestion to the file
    assertNull(codeInsightFixture.completeBasic())

    // THEN
    codeInsightFixture.checkResult("directories:\n  $expectedPath")
  }

  @Test
  fun `should not suggest already present directories`() {
    // GIVEN
    codeInsightFixture.addFileToProject("js-graphql/BUILD", "some text")
    codeInsightFixture.addFileToProject("js-test-common/BUILD", "some text")
    codeInsightFixture.configureByText(
      ".bazelproject",
      """
      directories:
        js-graphql
        js-<caret>
      """.trimIndent()
    )

    // WHEN code completion applied the only suggestion to the file
    assertNull(codeInsightFixture.completeBasic())

    // THEN
    codeInsightFixture.checkResult(
      """
      directories:
        js-graphql
        js-test-common
      """.trimIndent()
    )
  }

  @Test
  fun `should complete import section`() {
    codeInsightFixture.addFileToProject("subpackage/sub.bazelproject", "")
    codeInsightFixture.addFileToProject("otherDir/module.bazelproject", "")

    codeInsightFixture.configureByText(".bazelproject", "import <caret>")
    codeInsightFixture.type("baz")
    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("subpackage/sub.bazelproject", "otherDir/module.bazelproject")
  }

  @Test
  fun `should complete import run configuration section`() {
    codeInsightFixture.addFileToProject("subpackage/config.xml", "")
    codeInsightFixture.addFileToProject("otherDir/other_config.xml", "")

    codeInsightFixture.configureByText(".bazelproject", "import_run_configurations:\n  <caret>")
    codeInsightFixture.type("x")
    val lookups = codeInsightFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("subpackage/config.xml", "otherDir/other_config.xml")
  }
}
