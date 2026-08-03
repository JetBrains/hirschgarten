package org.jetbrains.bazel.python.gutter

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.jetbrains.python.testing.PyTestLineMarkerContributor
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class PyTestLineMarkerContributorTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)
  private val pyRunLineMarkerContributor = PyTestLineMarkerContributor()

  @Test
  fun `check that run gutters are shown for test methods`() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/python_test_gutters")
    fixture.performBazelSync()

    withContext(Dispatchers.EDT) {
      fixture.openFileInEditor(fixture.project.rootDir.findChild("test_sample.py")!!)
      fixture.editor.caretModel.moveToLogicalPosition(LogicalPosition(0, 4))
      pyRunLineMarkerContributor.getInfo((fixture.elementAtCaret as PsiNameIdentifierOwner).nameIdentifier!!).shouldNotBeNull()
    }

    withContext(Dispatchers.EDT) {
      fixture.openFileInEditor(fixture.project.rootDir.findChild("unittest_test.py")!!)
      fixture.editor.caretModel.moveToLogicalPosition(LogicalPosition(3, 8))
      pyRunLineMarkerContributor.getInfo((fixture.elementAtCaret as PsiNameIdentifierOwner).nameIdentifier!!).shouldNotBeNull()
    }
    Unit
  }
}
