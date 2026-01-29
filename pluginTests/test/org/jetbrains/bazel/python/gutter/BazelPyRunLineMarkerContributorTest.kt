package org.jetbrains.bazel.python.gutter

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path

@RunWith(JUnit4::class)
internal class BazelPyRunLineMarkerContributorTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  private val bazelPyRunLineMarkerContributor = BazelPyRunLineMarkerContributor()

  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  @Test
  fun `should show bazel-related run actions when the py file is the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile("main_file.py")

    project.addPyBinaryTarget(label = LABEL, mainFile = mainFile)
    project.addFileToTarget(mainFile, LABEL)

    // WHEN
    val elementAtCaret = mainFile.findElementAt(myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)!!

    // THEN
    runLineMarkerInfo.actions.shouldHaveSize(2)
  }

  @Test
  fun `should show bazel-related run actions when the py file is implicitly the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile(LABEL.targetName + ".py")

    project.addPyBinaryTarget(label = LABEL, mainFile = null)
    project.addFileToTarget(mainFile, LABEL)

    // WHEN
    val elementAtCaret = mainFile.findElementAt(myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)!!

    // THEN
    runLineMarkerInfo.actions.shouldHaveSize(2)
  }

  @Test
  fun `should hide bazel-related run actions when the py file is not the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile("main_file.py")
    val otherFile = myFixture.runnablePythonFile("other_file.py")

    project.addPyBinaryTarget(label = LABEL, mainFile = mainFile)
    project.addFileToTarget(mainFile, LABEL)
    project.addFileToTarget(otherFile, LABEL)

    // WHEN
    val elementAtCaret = otherFile.findElementAt(myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)

    // THEN
    runLineMarkerInfo.shouldBeNull()
  }

  @Test
  fun `should hide bazel-related run actions when the py file does not belong to any target`() {
    // GIVEN
    val notInTargetFile = myFixture.runnablePythonFile("not_in_target_file.py")

    project.addPyBinaryTarget(label = LABEL, mainFile = null)
    project.removeFileFromAllTargets(notInTargetFile)

    // WHEN
    val elementAtCaret = notInTargetFile.findElementAt(myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)

    // THEN
    runLineMarkerInfo.shouldBeNull()
  }

  @Test
  fun `should hide bazel-related run actions when the main module is defined`() {
    // GIVEN
    val runnableFile = myFixture.runnablePythonFile(LABEL.targetName + ".py")

    project.addPyBinaryTarget(label = LABEL, mainModule = "some.module")
    project.addFileToTarget(runnableFile, LABEL)

    // WHEN
    val elementAtCaret = runnableFile.findElementAt(myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)

    // THEN
    runLineMarkerInfo.shouldBeNull()
  }

  private fun CodeInsightTestFixture.runnablePythonFile(fileName: String): PsiFile =
    configureByText(
      fileName,
      """
      <caret>if __name__ == '__main__':
          print(f'Hello world')
    """.trimIndent(),
    )

  private fun Project.addPyBinaryTarget(
    label: Label,
    mainFile: PsiFile? = null,
    mainModule: String? = null,
  ) {
    targetUtils.setTargets(
      mapOf(
        label to RawBuildTarget(
          id = label,
          tags = emptyList(),
          dependencies = emptyList(),
          kind =
            TargetKind(
              kindString = "py_binary",
              languageClasses = setOf(LanguageClass.PYTHON),
              ruleType = RuleType.BINARY,
            ),
          sources = emptyList(),
          resources = emptyList(),
          baseDirectory = Path.of(myFixture.tempDirPath, "base_dir"),
          data =
            PythonBuildTarget(
              version = "3.8",
              interpreter = Path.of(myFixture.tempDirPath, "python3"),
              listOf(),
              false,
              listOf(),
              listOf(),
              mainFile = mainFile?.virtualFile?.toNioPath(),
              mainModule = mainModule,
            ),
        )
      )
    )
  }

  private fun Project.addFileToTarget(file: PsiFile, target: Label) {
    targetUtils.addFileToTargetIdEntry(file.virtualFile.toNioPath(), listOf(target))
  }

  private fun Project.removeFileFromAllTargets(file: PsiFile) {
    targetUtils.removeFileToTargetIdEntry(file.virtualFile.toNioPath())
  }

  companion object {
    private val LABEL = Label.parse("//foo:bar")
  }
}
