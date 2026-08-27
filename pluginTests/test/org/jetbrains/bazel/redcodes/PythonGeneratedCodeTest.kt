package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.jetbrains.python.inspections.unresolvedReference.PyUnresolvedReferencesInspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class PythonGeneratedCodeTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/python_generated_code",
    buildProject = true,
  )

  @Test
  fun testGeneratedPythonSourceResolves() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.enableInspections(PyUnresolvedReferencesInspection())
      fixture.checkHighlighting("genpy/consumer.py")
    }
  }
}
