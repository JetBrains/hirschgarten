package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkInspectionTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkLoadCycleInspectionTest : StarlarkInspectionTestCase() {
  private val description = StarlarkBundle.message("inspection.description.load.cycle")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkLoadCycleInspection())
  }

  @Test
  fun `self load cycle should be highlighted`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    myFixture.configureByText(
      "self_load.bzl",
      """
      load(<error descr="$description">"//:self_load.bzl"</error>, "a")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `two file load cycle should be highlighted`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    myFixture.addFileToProject(
      "cycle_B.bzl",
      """
      load("//:cycle_A.bzl", "a")
      b = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "cycle_A.bzl",
      """
      load(<error descr="$description">"//:cycle_B.bzl"</error>, "b")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `three file load cycle should be highlighted`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    myFixture.addFileToProject(
      "cycle_B.bzl",
      """
      load("//:cycle_C.bzl", "c")
      b = 1
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "cycle_C.bzl",
      """
      load("//:cycle_A.bzl", "a")
      c = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "cycle_A.bzl",
      """
      load(<error descr="$description">"//:cycle_B.bzl"</error>, "b")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `non cyclic load should not be highlighted`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    myFixture.addFileToProject(
      "no_cycle_A.bzl",
      """
      a = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "no_cycle_B.bzl",
      """
      load("//:no_cycle_A.bzl", "a")
      b = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `diamond dependency should not be highlighted`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    myFixture.addFileToProject(
      "diamond_B.bzl",
      """
      load("//:diamond_D.bzl", "d")
      b = 1
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "diamond_C.bzl",
      """
      load("//:diamond_D.bzl", "d")
      c = 1
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "diamond_D.bzl",
      """
      d = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "diamond_A.bzl",
      """
      load("//:diamond_B.bzl", "b")
      load("//:diamond_C.bzl", "c")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `only cyclic load label should be highlighted`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    myFixture.addFileToProject(
      "cyclic_B.bzl",
      """
      load("//:cyclic_A.bzl", "a")
      b = 1
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "non_cyclic.bzl",
      """
      c = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "cyclic_A.bzl",
      """
      load(<error descr="$description">"//:cyclic_B.bzl"</error>, "b")
      load("//:non_cyclic.bzl", "c")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `load cycle should be highlighted after adding load edge`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    val b = myFixture.addFileToProject(
      "added_cycle_B.bzl",
      """
      b = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "added_cycle_A.bzl",
      """
      load("//:added_cycle_B.bzl", "b")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)

    myFixture.saveText(
      b.virtualFile,
      """
      load("//:added_cycle_A.bzl", "a")
      b = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "added_cycle_A.bzl",
      """
      load(<error descr="$description">"//:added_cycle_B.bzl"</error>, "b")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `load cycle should stop being highlighted after removing load edge`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    val b = myFixture.addFileToProject(
      "removed_cycle_B.bzl",
      """
      load("//:removed_cycle_A.bzl", "a")
      b = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "removed_cycle_A.bzl",
      """
      load(<error descr="$description">"//:removed_cycle_B.bzl"</error>, "b")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)

    myFixture.saveText(
      b.virtualFile,
      """
      b = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "removed_cycle_A.bzl",
      """
      load("//:removed_cycle_B.bzl", "b")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `load label should stay highlighted when only one of two cycles is removed`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")

    myFixture.addFileToProject(
      "shared_cycle_B.bzl",
      """
      load("//:shared_cycle_A.bzl", "a")
      b = 1
      """.trimIndent(),
    )
    val c = myFixture.addFileToProject(
      "shared_cycle_C.bzl",
      """
      load("//:shared_cycle_A.bzl", "a")
      c = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "shared_cycle_A.bzl",
      """
      load(<error descr="$description">"//:shared_cycle_B.bzl"</error>, "b")
      load(<error descr="$description">"//:shared_cycle_C.bzl"</error>, "c")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)

    myFixture.saveText(
      c.virtualFile,
      """
      c = 1
      """.trimIndent(),
    )

    myFixture.configureByText(
      "shared_cycle_A.bzl",
      """
      load(<error descr="$description">"//:shared_cycle_B.bzl"</error>, "b")
      load("//:shared_cycle_C.bzl", "c")
      a = 1
      """.trimIndent(),
    )

    waitUntilIndexesAreReady()

    myFixture.checkHighlighting(true, false, false)
  }

  private fun waitUntilIndexesAreReady() {
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    FileDocumentManager.getInstance().saveAllDocuments()
    IndexingTestUtil.waitUntilIndexesAreReady(project)
  }
}
