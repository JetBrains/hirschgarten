package org.jetbrains.bazel.test.framework

import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.junit.jupiter.api.assertThrows
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.projectAware.BazelProjectAwareTestHooks.initializeBazelWorkspaceForTest
import org.jetbrains.bazel.projectAware.BazelProjectAwareTestHooks.isBazelWorkspaceInitializedForTest
import org.jetbrains.bazel.projectAware.BazelProjectAwareTestHooks.resetBazelWorkspaceForTest
import org.jetbrains.bazel.projectAware.BazelProjectAwareTestHooks.setLastBuiltByJpsForTest
import org.jetbrains.bazel.projectAware.BazelProjectAwareTestHooks.wasLastBuildByJpsForTest
import org.jetbrains.bazel.sync.environment.projectCtx

internal class BazelBasePlatformTestCaseTest : BazelBasePlatformTestCase() {
  fun testResetBazelTestProjectStateCleansSharedLightProject() {
    val tracker = ExternalSystemProjectTracker.getInstance(project) as AutoImportProjectTracker

    resetBazelWorkspaceForTest(project)
    project.isBazelProject = true
    project.rootDir = LightPlatformTestCase.getSourceRoot()
    initializeBazelWorkspaceForTest(project)

    assertTrue(isBazelWorkspaceInitializedForTest(project))
    assertEquals(1, tracker.getActivatedProjects().size)

    resetBazelTestProjectState(project)

    assertFalse(isBazelWorkspaceInitializedForTest(project))
    assertEmpty(tracker.getActivatedProjects())
    assertFalse(project.isBazelProject)
    assertNull(project.projectCtx.projectRootDir)

    project.isBazelProject = true
    project.rootDir = LightPlatformTestCase.getSourceRoot()
    initializeBazelWorkspaceForTest(project)

    assertTrue(isBazelWorkspaceInitializedForTest(project))
    assertEquals(1, tracker.getActivatedProjects().size)
  }

  fun testTearDownDoesNotMaskFixtureSetupFailure() {
    val testCase = FailingFixtureBazelBasePlatformTestCase().apply {
      name = "testDummy"
    }

    val error = assertThrows<IllegalStateException> {
      testCase.runBare()
    }

    assertEquals("fixture setup failed", error.message)
  }

  fun testResetBazelTestProjectStateClearsBuildTaskTracker() {
    setLastBuiltByJpsForTest(project, true)

    assertTrue(wasLastBuildByJpsForTest(project))

    resetBazelTestProjectState(project)

    assertFalse(wasLastBuildByJpsForTest(project))
  }
}

private class FailingFixtureBazelBasePlatformTestCase : BazelBasePlatformTestCase() {
  override fun createMyFixture(): CodeInsightTestFixture {
    throw IllegalStateException("fixture setup failed")
  }

  fun testDummy() = Unit
}
