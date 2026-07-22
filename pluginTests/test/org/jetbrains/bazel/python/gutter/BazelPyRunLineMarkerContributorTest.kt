package org.jetbrains.bazel.python.gutter

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
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
    initializeBazelProject(project, myFixture.tempDirPath)
  }

  @Test
  fun `should show bazel-related run actions when the py file is the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile("main_file.py")

    project.addPyBinaryTarget(label = LABEL, mainFile = mainFile)
    project.addFileToTarget(mainFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(mainFile, myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)!!

    // THEN
    runLineMarkerInfo.icon.shouldBeNull()
    runLineMarkerInfo.actions.shouldHaveSize(2)
  }

  @Test
  fun `should show bazel-related run actions when the py file is implicitly the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile(LABEL.targetName + ".py")

    project.addPyBinaryTarget(label = LABEL, mainFile = null)
    project.addFileToTarget(mainFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(mainFile, myFixture.caretOffset)!!
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
    val elementAtCaret = PsiUtilCore.getElementAtOffset(otherFile, myFixture.caretOffset)!!
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
    val elementAtCaret = PsiUtilCore.getElementAtOffset(notInTargetFile, myFixture.caretOffset)!!
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
    val elementAtCaret = PsiUtilCore.getElementAtOffset(runnableFile, myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)

    // THEN
    runLineMarkerInfo.shouldBeNull()
  }

  @Test
  fun `should show bazel-related run actions for pytest function`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "test_sample.py",
        """
        def <caret>test_passes():
            assert True
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)!!

    // THEN
    runLineMarkerInfo.icon shouldBe AllIcons.Actions.Execute
    runLineMarkerInfo.actions.shouldHaveSize(3)
  }

  @Test
  fun `should not calculate slow info for pytest function`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "test_sample.py",
        """
        def <caret>test_passes():
            assert True
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
    val slowRunLineMarkerInfo = bazelPyRunLineMarkerContributor.getSlowInfo(elementAtCaret)

    // THEN
    slowRunLineMarkerInfo.shouldBeNull()
  }

  @Test
  fun `should pass pytest node id as test executable argument for pytest function`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "test_sample.py",
        """
        def <caret>test_passes():
            assert True
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
    val extraProgramArguments = bazelPyRunLineMarkerContributor.getGutterAction(elementAtCaret)?.runnerActionDescriptor?.programArguments

    // THEN
    extraProgramArguments shouldBe listOf("test_sample.py::test_passes")
  }

  @Test
  fun `should pass pytest node id as test executable argument for pytest class method`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "test_sample.py",
        """
        class TestSample:
            def <caret>test_passes(self):
                assert True
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
    val extraProgramArguments = bazelPyRunLineMarkerContributor.getGutterAction(elementAtCaret)?.runnerActionDescriptor?.programArguments

    // THEN
    extraProgramArguments shouldBe listOf("test_sample.py::TestSample::test_passes")
  }

  @Test
  fun `should pass unittest method name as test executable argument for unittest method`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "unittest_test.py",
        """
        import unittest

        class SampleTest(unittest.TestCase):
            def <caret>test_passes(self):
                self.assertTrue(True)
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
    val extraProgramArguments = bazelPyRunLineMarkerContributor.getGutterAction(elementAtCaret)?.runnerActionDescriptor?.programArguments

    // THEN
    extraProgramArguments shouldBe listOf("SampleTest.test_passes")
  }

  @Test
  fun `should show bazel-related run actions for unittest method`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "unittest_test.py",
        """
        from unittest import TestCase

        class SampleTest(TestCase):
            def <caret>test_passes(self):
                self.assertTrue(True)
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)!!

    // THEN
    runLineMarkerInfo.icon shouldBe AllIcons.Actions.Execute
    runLineMarkerInfo.actions.shouldHaveSize(3)
  }

  @Test
  fun `should hide bazel-related run actions for pytest class name`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "test_sample.py",
        """
        class <caret>TestSample:
            def test_passes(self):
                assert True
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
    val runLineMarkerInfo = bazelPyRunLineMarkerContributor.getInfo(elementAtCaret)

    // THEN
    runLineMarkerInfo.shouldBeNull()
  }

  @Test
  fun `should hide bazel-related run actions for non-test function in py test target`() {
    // GIVEN
    val testFile =
      myFixture.pythonFile(
        "test_sample.py",
        """
        def <caret>helper():
            return True
        """.trimIndent(),
      )

    project.addPyTestTarget(label = LABEL)
    project.addFileToTarget(testFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset)!!
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

  private fun CodeInsightTestFixture.pythonFile(fileName: String, content: String): PsiFile =
    configureByText(fileName, content)

  private fun Project.addPyBinaryTarget(
    label: Label,
    mainFile: PsiFile? = null,
    mainModule: String? = null,
  ) {
    addPythonTarget(
      label = label,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      mainFile = mainFile,
      mainModule = mainModule,
    )
  }

  private fun Project.addPyTestTarget(label: Label) {
    addPythonTarget(
      label = label,
      ruleKind = "py_test",
      ruleType = RuleType.TEST,
      mainFile = null,
      mainModule = null,
    )
  }

  private fun Project.addPythonTarget(
    label: Label,
    ruleKind: String,
    ruleType: RuleType,
    mainFile: PsiFile?,
    mainModule: String?,
  ) {
    targetUtils.setTargets(
      listOf(
        RawBuildTarget(
          key = WorkspaceTargetKey(label = label),
          dependencies = emptyList(),
          kind =
            TargetKind(
              kind = ruleKind,
              languageClasses = setOf(PythonLanguageClass.PYTHON),
              ruleType = ruleType,
            ),
          sources = SourceFileCollection.EMPTY,
          generatedSources = SourceFileCollection.EMPTY,
          resources = SourceFileCollection.EMPTY,
          baseDirectory = Path.of(myFixture.tempDirPath, "base_dir"),
          data = listOf(
            PythonBuildTarget(
              version = "3.8",
              interpreter = Path.of(myFixture.tempDirPath, "python3"),
              listOf(),
              SourceFileCollection.EMPTY,
              SourceFileCollection.EMPTY,
              mainFile = mainFile?.virtualFile?.toNioPath(),
              mainModule = mainModule,
            ),
          ),
        ),
      ),
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
