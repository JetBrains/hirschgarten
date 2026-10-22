package org.jetbrains.bazel.python.gutter

import com.intellij.execution.PsiLocation
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.TestBazelProject
import org.jetbrains.bazel.test.framework.testproject.pythonTarget
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.ui.gutters.BazelContainingTargetsLocationsProvider
import org.jetbrains.bazel.ui.gutters.NonImportedBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.testLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.StringsCollection
import org.junit.jupiter.api.Test
import java.nio.file.Path

@BazelTestApplication
internal class BazelPyRunConfigurationProducerTest {
  private val myFixture by bazelCodeInsightFixture()
  private val testBazel: TestBazelProject get() = myFixture.testBazel

  private val bazelPyRunConfigurationProducer = BazelPyRunConfigurationProducer()

  @Test
  fun `should show bazel-related run actions when the py file is the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile("main_file.py")

    addPyBinaryTarget(label = LABEL, mainFile = mainFile, sources = listOf(mainFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(mainFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

    // THEN
    gutterAction.shouldNotBeNull()
  }

  @Test
  fun `should show bazel-related run actions when the py file is implicitly the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile(LABEL.targetName + ".py")

    addPyBinaryTarget(label = LABEL, mainFile = null, sources = listOf(mainFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(mainFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

    // THEN
    gutterAction.shouldNotBeNull()
  }

  @Test
  fun `should hide bazel-related run actions when the py file is not the main file`() {
    // GIVEN
    val mainFile = myFixture.runnablePythonFile("main_file.py")
    val otherFile = myFixture.runnablePythonFile("other_file.py")

    addPyBinaryTarget(label = LABEL, mainFile = mainFile, sources = listOf(mainFile, otherFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(otherFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

    // THEN
    gutterAction.shouldBeNull()
  }

  @Test
  fun `should hide bazel-related run actions when the py file does not belong to any target`() {
    // GIVEN
    val notInTargetFile = myFixture.runnablePythonFile("not_in_target_file.py")

    addPyBinaryTarget(label = LABEL, mainFile = null)

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(notInTargetFile, myFixture.caretOffset) }
    val bazelRunLocations = runReadActionBlocking {
      BazelContainingTargetsLocationsProvider().getAlternativeLocations(PsiLocation(elementAtCaret))
    }

    // THEN
    bazelRunLocations.shouldBeEmpty()
  }

  @Test
  fun `should hide bazel-related run actions when the main module is defined`() {
    // GIVEN
    val runnableFile = myFixture.runnablePythonFile(LABEL.targetName + ".py")

    addPyBinaryTarget(label = LABEL, mainModule = "some.module", sources = listOf(runnableFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(runnableFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

    addPyTestTarget(label = LABEL, sources = listOf(testFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

    addPyTestTarget(label = LABEL, sources = listOf(testFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

    addPyTestTarget(label = LABEL, sources = listOf(testFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

    addPyTestTarget(label = LABEL, sources = listOf(testFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

    addPyTestTarget(label = LABEL, sources = listOf(testFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

    addPyTestTarget(label = LABEL, sources = listOf(testFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

    addPyTestTarget(label = LABEL, sources = listOf(testFile))

    // WHEN
    val elementAtCaret = runReadActionBlocking { PsiUtilCore.getElementAtOffset(testFile, myFixture.caretOffset) }
    val gutterAction = runReadActionBlocking { bazelPyRunConfigurationProducer.getGutterAction(elementAtCaret, executableTarget) }

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

  private fun addPyBinaryTarget(
    label: Label,
    mainFile: PsiFile? = null,
    mainModule: String? = null,
    sources: List<PsiFile> = emptyList(),
  ) {
    addPythonTarget(
      label = label,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      mainFile = mainFile,
      mainModule = mainModule,
      sources = sources,
    )
  }

  private fun addPyTestTarget(label: Label, sources: List<PsiFile>) {
    addPythonTarget(
      label = label,
      ruleKind = "py_test",
      ruleType = RuleType.TEST,
      mainFile = null,
      mainModule = null,
      sources = sources,
    )
  }

  private fun addPythonTarget(
    label: Label,
    ruleKind: String,
    ruleType: RuleType,
    mainFile: PsiFile?,
    mainModule: String?,
    sources: List<PsiFile>,
  ) {
    testBazel.sync {
      targets(
        pythonTarget(label, ruleKind, ruleType) {
          srcs(*sources.map { it.name }.toTypedArray())
          data(
            PythonBuildTarget(
              version = "3.8",
              interpreter = testLocation(Path.of(myFixture.tempDirPath, "python3")),
              StringsCollection.EMPTY,
              OutputLocationCollection.EMPTY,
              OutputLocationCollection.EMPTY,
              mainFile = mainFile?.virtualFile?.toNioPath()?.let(::testLocation),
              mainModule = mainModule,
            ),
          )
        },
      )
    }
  }

  companion object {
    private val LABEL = Label.parse("//foo:bar")
    private val executableTarget =
      NonImportedBuildTarget(LABEL, TargetKind("py_test", setOf(PythonLanguageClass.PYTHON), RuleType.TEST))
  }
}
