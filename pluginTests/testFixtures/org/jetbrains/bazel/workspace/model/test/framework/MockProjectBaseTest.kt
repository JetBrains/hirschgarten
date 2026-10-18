package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication

@BazelTestApplication
abstract class MockProjectBaseTest {

  @TestDisposable
  lateinit var disposable: Disposable

  val projectDir = tempPathFixture()
  
  protected val projectFixture: TestFixture<Project> = projectFixture(pathFixture = projectDir, openAfterCreation = false)
  val project: Project by projectFixture

  protected fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}
