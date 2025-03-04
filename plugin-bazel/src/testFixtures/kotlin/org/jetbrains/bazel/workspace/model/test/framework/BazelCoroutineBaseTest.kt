package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.testFramework.registerOrReplaceServiceInstance
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.coroutines.BazelCoroutineService

/**
 * Base test class for testing code which does some background operations using `BazelCoroutineService`.
 * These operations will be performed in a single thread, with `delay(...)` statements skipped.
 * Usage example:
 * ```
 * @Test
 * fun `Some test`() {
 *   bazelCoroutineTest {
 *     // when (some code with coroutines)
 *     waitForCoroutines()
 *     // then (assertions)
 *   }
 * }
 * ```
 *
 * In this example, coroutines started using `BazelCoroutineService` will be finished by the time the assertions are executed
 */
abstract class BazelCoroutineBaseTest : MockProjectBaseTest() {
  /** Runs a code block with Bazel coroutines done in a single thread and `delay(...)` statements skipped */
  protected fun bazelCoroutineTest(testBody: suspend TestScopeWrapper.() -> Unit) {
    runTest {
      project.registerOrReplaceServiceInstance(
        BazelCoroutineService::class.java,
        BazelCoroutineService(this),
        this@BazelCoroutineBaseTest,
      )
      TestScopeWrapper(this).testBody()
    }
  }

  // thanks to this wrapper, the test class does not have to import kotlinx.coroutines.test.TestScope
  // (and therefore it does not have to be added as a dependency)
  protected class TestScopeWrapper(private val testScope: TestScope) {
    /** Blocks execution until all BSP coroutine jobs are finished (with `delay` statements skipped) */
    fun waitForCoroutines() = testScope.testScheduler.advanceUntilIdle()
  }
}
