package org.jetbrains.bazel.languages.projectview.completion

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.projectDirectories
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@BazelTestApplication
class ProjectViewSectionItemCompletionContributorTest {
  private val codeInsightFixture by bazelCodeInsightFixture()

  @BeforeEach
  fun addProjectDirectories() {
    codeInsightFixture.testBazel.projectDirectories()
  }

  @Test
  fun `should complete sharding approach variants`() {
    codeInsightFixture.configureByText(".bazelproject", "sharding_approach: <caret>")
    codeInsightFixture.type("a")

    val lookups = codeInsightFixture.completeBasicLookupStrings()
    lookups shouldContainAll listOf("expand_and_shard", "query_and_shard", "shard_only")
  }

  @Test
  fun `should complete build flags variants`() {
    codeInsightFixture.configureByText(".bazelproject", "build_flags:\n  <caret>")
    codeInsightFixture.type("action")

    val lookups = codeInsightFixture.completeBasicLookupStrings()
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

    val lookups = codeInsightFixture.completeBasicLookupStrings()
    val expectedFlags =
      Flag
        .all()
        .filter {
          it.key.contains("b") &&
            it.value.option.commands.any { cmd ->
              cmd == "build"
            }
        }.map { it.key }

    lookups shouldContainExactlyInAnyOrder expectedFlags
  }

  @Test
  fun `should complete debug flags variants`() {
    codeInsightFixture.configureByText(".bazelproject", "debug_flags:\n  <caret>")
    codeInsightFixture.type("action")

    val lookups = codeInsightFixture.completeBasicLookupStrings()
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

    val lookups = codeInsightFixture.completeBasicLookupStrings()
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

    val lookups = codeInsightFixture.completeBasicLookupStrings()
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
    val lookups = codeInsightFixture.completeBasicLookupStrings()

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
    val lookups = codeInsightFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder listOf("subpackage/sub.bazelproject", "otherDir/module.bazelproject")
  }

  @Test
  fun `should complete import run configuration section`() {
    codeInsightFixture.addFileToProject("subpackage/config.xml", "")
    codeInsightFixture.addFileToProject("otherDir/other_config.xml", "")

    codeInsightFixture.configureByText(".bazelproject", "import_run_configurations:\n  <caret>")
    codeInsightFixture.type("x")
    val lookups = codeInsightFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder listOf("subpackage/config.xml", "otherDir/other_config.xml")
  }

  @Test
  fun `should complete build flags in dumb mode`() {
    codeInsightFixture.configureByText(".bazelproject", "build_flags:\n  <caret>")
    codeInsightFixture.type("action")

    codeInsightFixture.completeBasicLookupStrings(dumbMode = true) shouldContainAll listOf("--action_env")
  }

  @Test
  fun `should complete boolean sections in dumb mode`() {
    codeInsightFixture.configureByText(".bazelproject", "shard_sync: <caret>")

    codeInsightFixture.completeBasicLookupStrings(dumbMode = true) shouldContainExactlyInAnyOrder listOf("true", "false")
  }

  @Test
  fun `should complete directories in dumb mode`() {
    codeInsightFixture.addFileToProject("main/BUILD", "some text")
    codeInsightFixture.addFileToProject("module1/BUILD", "some text")
    codeInsightFixture.configureByText(".bazelproject", "directories:\n  m<caret>")

    codeInsightFixture.completeBasicLookupStrings(dumbMode = true) shouldContainAll listOf("main", "module1")
  }

  @Test
  fun `should complete import run configuration section in dumb mode`() {
    codeInsightFixture.addFileToProject("subpackage/config.xml", "")
    codeInsightFixture.addFileToProject("otherDir/other_config.xml", "")
    codeInsightFixture.configureByText(".bazelproject", "import_run_configurations:\n  <caret>")
    codeInsightFixture.type("x")

    val lookups = codeInsightFixture.completeBasicLookupStrings(dumbMode = true)
    lookups shouldContainExactlyInAnyOrder listOf("subpackage/config.xml", "otherDir/other_config.xml")
  }

  @Test
  fun `should complete import section in dumb mode`() {
    codeInsightFixture.addFileToProject("subpackage/sub.bazelproject", "")
    codeInsightFixture.configureByText(".bazelproject", "import <caret>")
    codeInsightFixture.type("baz")

    codeInsightFixture.completeBasicLookupStrings(dumbMode = true) shouldContainAll listOf("subpackage/sub.bazelproject")
  }
}
