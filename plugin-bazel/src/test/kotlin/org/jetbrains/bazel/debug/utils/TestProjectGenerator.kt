package org.jetbrains.bazel.debug.utils

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl

// TODO - replace with com.intellij.testFramework.junit5.fixture.FixturesKt#projectFixture once available (2024.2)

/**
 * This object is capable of generating a `Project` instance for tests.
 * Its implementation is based on `com.intellij.testFramework.fixtures.BasePlatformTestCase`,
 * but this object can also be used with JUnit5 (`BasePlatformTestCase` is only compatible with 3 and 4).
 * This method of getting project instances is mentioned in
 * [plugin SDK documentation](https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html).
 */
object TestProjectGenerator {
  fun createProject(): Project =
    getTestFixture().let {
      it.setUp()
      it.project
    }

  private fun getTestFixture(): CodeInsightTestFixture {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val builder = factory.createLightFixtureBuilder(null, "")
    val fixture = builder.fixture
    return IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture, getTempDir())
  }

  private fun getTempDir(): TempDirTestFixture =
    when(val policy = IdeaTestExecutionPolicy.current()) {
      null -> LightTempDirTestFixtureImpl(true)
      else -> policy.createTempDirTestFixture()
    }
}
