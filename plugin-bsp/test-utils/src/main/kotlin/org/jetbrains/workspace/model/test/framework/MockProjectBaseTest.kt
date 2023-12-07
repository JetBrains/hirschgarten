package org.jetbrains.workspace.model.test.framework

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.rules.ProjectModelExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

@TestApplication
public open class MockProjectBaseTest {
  @JvmField
  @RegisterExtension
  protected val projectModel: ProjectModelExtension = ProjectModelExtension()

  protected val project: Project
    get() = projectModel.project

  private val virtualFileManager: VirtualFileManager
    get() = VirtualFileManager.getInstance()

  @BeforeEach
  protected open fun beforeEach() {}

  protected fun Path.toVirtualFile(): VirtualFile = virtualFileManager.findFileByNioPath(this)!!

  protected fun <T> runWriteAction(task: () -> T): T {
    var result: T? = null
    WriteCommandAction.runWriteCommandAction(project) {
      result = task()
    }

    return result!!
  }
}
