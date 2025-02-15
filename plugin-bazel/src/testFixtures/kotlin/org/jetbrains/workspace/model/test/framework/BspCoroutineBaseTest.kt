package org.jetbrains.workspace.model.test.framework

import com.intellij.testFramework.registerOrReplaceServiceInstance
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService

/**
 * Base test class for testing code which does some background operations using `BspCoroutineService`.
 * These operations will be performed in a single thread, with `delay(...)` statements skipped.
 * Usage example:
 * ```
 * @Test
 * fun `Some test`() {
 *   bspCoroutineTest {
 *     // when (some code with coroutines)
 *     waitForCoroutines()
 *     // then (assertions)
 *   }
 * }
 * ```
 *
 * In this example, coroutines started using `BspCoroutineService` will be finished by the time the assertions are executed
 */
abstract class BspCoroutineBaseTest : MockProjectBaseTest() {
  /** Runs a code block with BSP coroutines done in a single thread and `delay(...)` statements skipped */
  protected fun bspCoroutineTest(testBody: suspend TestScopeWrapper.() -> Unit) {
    runTest {
      project.registerOrReplaceServiceInstance(
        BspCoroutineService::class.java,
        BspCoroutineService(this),
        this@BspCoroutineBaseTest,
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
