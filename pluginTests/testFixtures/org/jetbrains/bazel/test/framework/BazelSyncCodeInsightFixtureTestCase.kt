package org.jetbrains.bazel.test.framework

import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl

/**
 * Base class for tests that require a sync.
 *
 * Primarily created for testing redcode issues.
 *
 * This is a *heavy* test because:
 * - Sync operations require real filesystem access.
 * - Reproducing redcode issues often involves multiple modules, which are only allowed in heavy tests.
 */
abstract class BazelSyncCodeInsightFixtureTestCase : UsefulTestCase() {

  protected lateinit var myFixture: BazelSyncCodeInsightTestFixture
    private set

  override fun setUp() {
    super.setUp()
    val projectBuilder = IdeaTestFixtureFactory
      .getFixtureFactory()
      .createFixtureBuilder(javaClass.getName() + "." + name)
    myFixture = BazelSyncCodeInsightTestFixtureImpl(
      projectFixture = projectBuilder.fixture,
      tempDirTestFixture = TempDirTestFixtureImpl(),
    )
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
