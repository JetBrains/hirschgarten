package org.jetbrains.workspace.model.test.framework

import com.google.idea.testing.BazelTestApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.rules.ProjectModelExtension
import org.jetbrains.plugins.bsp.config.rootDir
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

@BazelTestApplication
public open class MockProjectBaseTest : Disposable {
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

  protected fun <T> runWriteAction(task: () -> T): T {
    var result: T? = null
    WriteCommandAction.runWriteCommandAction(project) {
      result = task()
    }

    return result!!
  }

  protected fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, projectModel.disposableRule.disposable)
  }

  override fun dispose() {
    // Required for the test framework to clean up the project
    // Otherwise the "hidden" leak hunter test will fail
    Disposer.dispose(project)
  }
}
