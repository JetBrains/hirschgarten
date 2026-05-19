package org.jetbrains.bazel.test.framework

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject

abstract class BazelBasePlatformTestCase : BasePlatformTestCase() {
  private var disposable = Disposer.newCheckedDisposable()

  override fun setUp() {
    disposable = Disposer.newCheckedDisposable()
    super.setUp()
    initializeBazelProject(project, myFixture.tempDirPath)
  }

  override fun tearDown() {
    Disposer.dispose(disposable)
    super.tearDown()
  }

  fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}
