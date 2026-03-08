package org.jetbrains.bazel.test.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.projectAware.BazelProjectAwareTestHooks.resetBazelWorkspaceForTest
import org.jetbrains.bazel.projectAware.BazelProjectAwareTestHooks.resetLastBuiltByJpsForTest
import org.jetbrains.bazel.sync.environment.BazelProjectContextService

abstract class BazelBasePlatformTestCase : BasePlatformTestCase() {
  private var disposable = Disposer.newCheckedDisposable()

  override fun setUp() {
    disposable = Disposer.newCheckedDisposable()
    super.setUp()
    project.isBazelProject = true
    project.rootDir = LightPlatformTestCase.getSourceRoot()
  }

  override fun tearDown() {
    try {
      myFixture?.project?.let(::resetBazelTestProjectState)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    } finally {
      try {
        Disposer.dispose(disposable)
      }
      catch (e: Throwable) {
        addSuppressedException(e)
      }
      finally {
        super.tearDown()
      }
    }
  }

  fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}

internal fun resetBazelTestProjectState(project: Project) {
  resetBazelWorkspaceForTest(project)
  resetLastBuiltByJpsForTest(project)
  project.getServiceIfCreated(BazelProjectContextService::class.java)?.apply {
    isBazelProject = false
    projectRootDir = null
    workspaceName = null
    bazelBinPath = null
    bazelExecPath = null
  }
}
