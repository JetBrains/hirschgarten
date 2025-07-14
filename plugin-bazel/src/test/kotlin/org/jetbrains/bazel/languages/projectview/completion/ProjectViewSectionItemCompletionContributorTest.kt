package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewSectionItemCompletionContributorTest : BasePlatformTestCase() {
  @Test
  fun `should complete workspace type variants`() {
    myFixture.configureByText(".bazelproject", "workspace_type: <caret>")
    myFixture.type("a")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldContainAll listOf("java", "javascript", "dart", "android")
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
    myFixture.configureByText(".bazelproject", "use_query_sync: <caret>")
    myFixture.type("t")
    myFixture.completeBasic()

    myFixture.checkResult("use_query_sync: true")
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

    myFixture.configureByText(".bazelproject", "import: <caret>")
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
