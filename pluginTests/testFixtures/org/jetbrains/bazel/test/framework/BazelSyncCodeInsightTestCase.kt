package org.jetbrains.bazel.test.framework

import com.intellij.testFramework.UsefulTestCase

/**
 * Base class for tests that require a sync.
 *
 * Primarily created for testing redcode issues.
 *
 * This is a *heavy* test because:
 * - Sync operations require real filesystem access.
 * - Reproducing redcode issues often involves multiple modules, which are only allowed in heavy tests.
 */
abstract class BazelSyncCodeInsightTestCase : UsefulTestCase() {

  protected lateinit var myFixture: BazelSyncCodeInsightTestFixture
    private set

  override fun setUp() {
    super.setUp()
    myFixture = BazelSyncCodeInsightTestFixture(name = javaClass.getName() + "." + name)
    myFixture.setUp()
  }

  override fun tearDown() {
    try {
      myFixture.tearDown()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
