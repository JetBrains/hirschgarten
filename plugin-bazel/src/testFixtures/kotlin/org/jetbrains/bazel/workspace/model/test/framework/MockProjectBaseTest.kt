package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.rules.ProjectModelExtension
import org.jetbrains.bazel.config.rootDir
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

@TestApplication
abstract class MockProjectBaseTest {

  @TestDisposable
  lateinit var disposable: Disposable

  @JvmField
  @RegisterExtension
  protected val projectModel: ProjectModelExtension = ProjectModelExtension()

  protected val project: Project
    get() = projectModel.project

  private val virtualFileManager: VirtualFileManager
    get() = VirtualFileManager.getInstance()

  @BeforeEach
  protected open fun beforeEach() {
    project.rootDir = projectModel.projectRootDir.toVirtualFile()
  }

  protected fun Path.toVirtualFile(): VirtualFile = virtualFileManager.findFileByNioPath(this)!!

  protected fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}
