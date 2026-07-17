package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.jetbrains.python.inspections.unresolvedReference.PyUnresolvedReferencesInspection
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class PythonGeneratedCodeTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testGeneratedPythonSourceResolves() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/python_generated_code")
    fixture.performBazelSync(buildProject = true)
    withContext(Dispatchers.EDT) {
      fixture.enableInspections(PyUnresolvedReferencesInspection())
      fixture.checkHighlighting("genpy/consumer.py")
    }
  }
}
