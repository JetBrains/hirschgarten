package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import kotlin.io.path.pathString

interface BazelSyncCodeInsightTestFixture : CodeInsightTestFixture {

  suspend fun performBazelSync()
}

class BazelSyncCodeInsightTestFixtureImpl(
  projectFixture: IdeaProjectTestFixture,
  tempDirTestFixture: TempDirTestFixture,
) : CodeInsightTestFixtureImpl(projectFixture, tempDirTestFixture), BazelSyncCodeInsightTestFixture {

  init {
    testDataPath = BazelPathManager.testProjectsRoot.pathString
  }

  override suspend fun performBazelSync() {
    ProjectSyncTask(project).sync(SecondPhaseSync, true)
  }

  override fun setUp() {
    super.setUp()
    project.bazelProjectProperties.rootDir = virtualFileOf(tempDirPath)
    VfsRootAccess.allowRootAccess(project, "/private/var/tmp/")
  }

  override fun tearDown() {
    try {
      project.bazelProjectProperties.rootDir = null
      WriteAction.runAndWait<Throwable> {
        ProjectJdkTable.getInstance().apply {
          allJdks.forEach(this::removeJdk)
        }
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}

