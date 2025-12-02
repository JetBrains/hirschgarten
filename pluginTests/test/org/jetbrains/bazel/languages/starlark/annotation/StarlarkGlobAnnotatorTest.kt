package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkGlobAnnotatorTest : StarlarkAnnotatorTestCase() {
  private val globMsg = StarlarkBundle.message("annotator.glob.empty")
  private val patternMsg = StarlarkBundle.message("annotator.glob.empty.pattern")

  private fun runWithBazel7VersionSet(action: () -> Unit) {
    fun setBazelVersion(version: String) {
      myFixture.project.getService(BazelVersionCheckerService::class.java).loadState(
        BazelVersionCheckerService.State().apply {
          currentBazelVersion = version
        }
      )
    }
    setBazelVersion("7.7.0")
    action()
    setBazelVersion("8.0.0")
  }

  @Test
  fun testNoArgsGlobAnnotator() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = <error descr="$globMsg">glob</error>(),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testNoArgsGlobAnnotatorForBazel7() {
    runWithBazel7VersionSet {
      myFixture.configureByText(
        "BUILD",
        """
      java_library(
          name = "myLib",
          srcs = glob,
      )
      """.trimIndent(),
      )
      myFixture.checkHighlighting(true, false, false)
    }
  }



  @Test
  fun testEmptyIncludesGlobAnnotator() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = <error descr="$globMsg">glob</error>(include = []),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyIncludesGlobAnnotatorForBazel7() {
    runWithBazel7VersionSet {
      myFixture.configureByText(
        "BUILD",
        """
      java_library(
          name = "myLib",
          srcs = glob(include = []),
      )
      """.trimIndent(),
      )
    }
  }

  @Test
  fun testEmptyUnnamedIncludesGlobAnnotator() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = <error descr="$globMsg">glob</error>([]),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyUnnamedIncludesGlobAnnotatorForBazel7() {
    runWithBazel7VersionSet {
      myFixture.configureByText(
        "BUILD",
        """
      java_library(
          name = "myLib",
          srcs = glob([]),
      )
      """.trimIndent(),
      )
      myFixture.checkHighlighting(true, false, false)
    }
  }

  @Test
  fun testEmptyGlobAnnotator() {
    myFixture.addFileToProject("example1.java", "")
    myFixture.addFileToProject("example2.java", "")
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = <error descr="$globMsg">glob</error>(["*.java"], exclude=["example*.java"]),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyGlobAnnotatorForBazel7() {
    runWithBazel7VersionSet {
      myFixture.addFileToProject("example1.java", "")
      myFixture.addFileToProject("example2.java", "")
      myFixture.configureByText(
        "BUILD",
        """
      java_library(
          name = "myLib",
          srcs = glob(["*.java"], exclude=["example*.java"]),
      )
      """.trimIndent(),
      )
      myFixture.checkHighlighting(true, false, false)
    }
  }

  @Test
  fun testUnresolvedGlobPatternsAnnotator() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = <error descr="$globMsg">glob</error>([<warning descr="$patternMsg">"**/*.nonexistent"</warning>]),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testUnresolvedGlobPatternsAnnotatorForBazel7() {
    runWithBazel7VersionSet {
      myFixture.configureByText(
        "BUILD",
        """
      java_library(
          name = "myLib",
          srcs = glob(["**/*.nonexistent"]),
      )
      """.trimIndent(),
      )
      myFixture.checkHighlighting(true, false, false)
    }
  }

  @Test
  fun testSomeUnresolvedGlobPatternsAnnotator() {
    myFixture.addFileToProject("example1.java", "")
    myFixture.addFileToProject("example2.kt", "")
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = glob([<warning descr="$patternMsg">"**/*.nonexistent"</warning>, "*.java"]),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyGlobAllowedAnnotator() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = glob(["*.java"], ["example*.java"], allow_empty = True),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testUnresolvedGlobPatternsAllowedAnnotator() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = glob(["**/*.nonexistent"], allow_empty = True),
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun testEmptyGlobNotAllowedAnnotatorForBazel7() {
    runWithBazel7VersionSet {
      myFixture.configureByText(
        "BUILD",
        """
      java_library(
          name = "myLib",
          srcs = <error descr="$globMsg">glob</error>([<warning descr="$patternMsg">"*.java"</warning>], allow_empty = False),
      )
      """.trimIndent(),
      )
      myFixture.checkHighlighting(true, false, false)
    }
  }

  @Test
  fun testUnresolvedGlobPatternsNotAllowedAnnotatorForBazel7() {
    runWithBazel7VersionSet {
      myFixture.configureByText(
        "BUILD",
        """
      java_library(
          name = "myLib",
          srcs = <error descr="$globMsg">glob</error>([<warning descr="$patternMsg">"**/*.nonexistent"</warning>], allow_empty = False),
      )
      """.trimIndent(),
      )
      myFixture.checkHighlighting(true, false, false)
    }
  }
}
