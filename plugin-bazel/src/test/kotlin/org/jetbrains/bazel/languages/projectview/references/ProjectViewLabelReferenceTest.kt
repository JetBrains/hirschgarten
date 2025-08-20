package org.jetbrains.bazel.languages.projectview.references

import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.ModuleFixture
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewLabelReferenceTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  @Test
  fun `targets should return a reference and resolve`() {
    project.isBazelProject = true
    project.rootDir = myFixture.tempDirFixture.getFile(".")!!

    myFixture.addFileToProject(
      "BUILD",
      """
      java_binary(name = "some_target")
      """.trimIndent(),
    )
    myFixture.configureByText(
      ".bazelproject",
      """
      targets:
        //:some_<caret>target
      """.trimIndent(),
    )

    val reference = myFixture.getReferenceAtCaretPosition()
    assertNotNull(reference)
    assertTrue(reference is ProjectViewLabelReference)
    assertNotNull(reference?.resolve())
  }

  @Test
  fun `should not resolve non-existing targets`() {
    project.isBazelProject = true
    project.rootDir = myFixture.tempDirFixture.getFile(".")!!

    myFixture.configureByText(
      ".bazelproject",
      """
      targets:
        //:some_<caret>target
      """.trimIndent(),
    )

    val reference = myFixture.getReferenceAtCaretPosition()
    assertNotNull(reference)
    assertTrue(reference is ProjectViewLabelReference)
    assertNull(reference?.resolve())
  }
}
