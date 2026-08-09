package org.jetbrains.bazel.python.gutter

import com.intellij.execution.PsiLocation
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.ModuleFixture
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.ui.gutters.BazelContainingTargetsLocationsProvider
import org.jetbrains.bazel.ui.gutters.NonImportedBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path
import kotlin.io.path.Path

@RunWith(JUnit4::class)
internal class BazelPyRunConfigurationProducerTest : CodeInsightFixtureTestCase<ModuleFixtureBuilder<ModuleFixture>>() {
  private val bazelPyRunConfigurationProducer = BazelPyRunConfigurationProducer()

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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldNotBeNull()
  }

  @Test
  fun `should show bazel-related run actions when the py file is implicitly the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile(LABEL.targetName + ".py")

    project.addPyBinaryTarget(label = LABEL, mainFile = null)
    project.addFileToTarget(mainFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(mainFile, myFixture.caretOffset)!!
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldNotBeNull()
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldBeNull()
  }

  @Test
  fun `should hide bazel-related run actions when the py file does not belong to any target`() {
    // GIVEN
    val notInTargetFile = myFixture.runnablePythonFile("not_in_target_file.py")

    project.addPyBinaryTarget(label = LABEL, mainFile = null)
    project.removeFileFromAllTargets(notInTargetFile)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(notInTargetFile, myFixture.caretOffset)!!
    val bazelRunLocations = BazelContainingTargetsLocationsProvider().getAlternativeLocations(PsiLocation(elementAtCaret))

    // THEN
    bazelRunLocations.shouldBeEmpty()
  }

  @Test
  fun `should hide bazel-related run actions when the main module is defined`() {
    // GIVEN
    val runnableFile = myFixture.runnablePythonFile(LABEL.targetName + ".py")

    project.addPyBinaryTarget(label = LABEL, mainModule = "some.module")
    project.addFileToTarget(runnableFile, LABEL)

    // WHEN
    val elementAtCaret = PsiUtilCore.getElementAtOffset(runnableFile, myFixture.caretOffset)!!
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldBeNull()
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldNotBeNull()
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldNotBeNull()
    gutterAction.programArguments shouldBe listOf("test_sample.py::test_passes")
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldNotBeNull()
    gutterAction.programArguments shouldBe listOf("test_sample.py::TestSample::test_passes")
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldNotBeNull()
    gutterAction.programArguments shouldBe listOf("SampleTest.test_passes")
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldNotBeNull()
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldBeNull()
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
    val gutterAction = bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget)

    // THEN
    gutterAction.shouldBeNull()
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
    targetStorage.setTargets(
      listOf(
        TestBuildTarget(
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
    targetStorage.addFileToTargetIdEntry(file.virtualFile.toNioPath(), listOf(target))
  }

  private fun Project.removeFileFromAllTargets(file: PsiFile) {
    targetStorage.removeFileToTargetIdEntry(file.virtualFile.toNioPath())
  }

  companion object {
    private val LABEL = Label.parse("//foo:bar")
    private val executableTarget =
      NonImportedBuildTarget(LABEL, TargetKind("py_test", setOf(PythonLanguageClass.PYTHON), RuleType.TEST), Path("base/directory"))
  }
}
