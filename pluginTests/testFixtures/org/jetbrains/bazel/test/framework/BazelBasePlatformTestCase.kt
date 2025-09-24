package org.jetbrains.bazel.test.framework

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir

abstract class BazelBasePlatformTestCase : BasePlatformTestCase() {
  private val disposable = Disposer.newDisposable()

  override fun setUp() {
    super.setUp()
    project.isBazelProject = true
    project.rootDir = LightPlatformTestCase.getSourceRoot()
  }

  override fun tearDown() {
    Disposer.dispose(disposable)
    super.tearDown()
  }

  fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}
