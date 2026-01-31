package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.test.compat.PluginTestsCompat
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

@BazelTestApplication
abstract class MockProjectBaseTest {

  @TestDisposable
  lateinit var disposable: Disposable

  val projectDir = tempPathFixture()
  val project: Project by projectFixture(pathFixture = projectDir, openAfterCreation = false)

  protected fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}


class BazelIdeaTextExtension : BeforeAllCallback, AfterAllCallback {
  val disposable = Disposer.newDisposable()

  override fun beforeAll(context: ExtensionContext?) {
    PluginTestsCompat.setupTestSuite(disposable)
  }

  override fun afterAll(context: ExtensionContext?) {
    Disposer.dispose(disposable)
  }

}
