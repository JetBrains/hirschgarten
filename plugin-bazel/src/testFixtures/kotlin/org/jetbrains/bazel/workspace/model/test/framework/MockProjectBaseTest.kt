package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.utils.findVirtualFile
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.jvm.optionals.getOrNull

@TestApplication
@ExtendWith(BazelProjectFixtureTestExtension::class)
abstract class MockProjectBaseTest {

  @TestDisposable
  lateinit var disposable: Disposable

  val projectDir = tempPathFixture()
  val projectFixture: TestFixture<Project> = projectFixture(pathFixture = projectDir, openAfterCreation = false)

  val project by projectFixture

  protected fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}

class BazelProjectFixtureTestExtension : BeforeEachCallback {
  override fun beforeEach(context: ExtensionContext?) {
    val instance = context?.testInstance?.getOrNull() ?: return
    val fixture = (instance as? MockProjectBaseTest)?.projectFixture ?: return
    fixture.get().rootDir = instance.projectDir.get().findVirtualFile() ?: error("Cannot find root directory")
  }
}
