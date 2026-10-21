package org.jetbrains.bazel.python.sync

import com.intellij.python.pyproject.model.evolution.evoPyProjects
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainNull
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * A sync sets an interpreter on every Python module. The modules of one Python binary share one interpreter, because
 * the importer builds one interpreter for each binary.
 */
@BazelTestApplication
class PythonInterpreterSyncTest {

  // Its Python targets all use the default toolchain, so they share one Python binary.
  private val project by bazelProjectFixture("redcodes/python_imports")

  @Test
  fun `modules of one Python binary share one interpreter`(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    val interpreters = project.evoPyProjects().map { it.interpreter }.toList()

    interpreters shouldHaveAtLeastSize 2
    interpreters.shouldNotContainNull()
    interpreters.distinct() shouldHaveSize 1
  }
}
