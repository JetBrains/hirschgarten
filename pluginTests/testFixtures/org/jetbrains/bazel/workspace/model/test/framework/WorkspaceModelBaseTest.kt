package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.rules.ProjectModelExtension
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.TestBazelProject
import org.jetbrains.bazel.test.framework.testproject.addJavaModule
import org.jetbrains.bazel.test.framework.testproject.installTestBazelProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

@BazelTestApplication
abstract class WorkspaceModelBaseTest {

  @TestDisposable
  lateinit var disposable: Disposable

  protected lateinit var workspaceEntityStorageBuilder: MutableEntityStorage

  @JvmField
  @RegisterExtension
  protected val projectModel: ProjectModelExtension = ProjectModelExtension()

  protected lateinit var testBazel: TestBazelProject
    private set

  @BeforeEach
  protected open fun beforeEach() {
    workspaceEntityStorageBuilder = (workspaceModel as WorkspaceModelImpl).getBuilderSnapshot().builder
    testBazel = project.installTestBazelProject(projectBasePath, projectModel.disposableRule.disposable)
  }

  protected val project: Project
    get() = projectModel.project

  protected val projectBasePath: Path
    get() = projectModel.projectRootDir

  protected val workspaceModel: WorkspaceModel
    get() = WorkspaceModel.getInstance(project)

  protected val virtualFileUrlManager: VirtualFileUrlManager
    get() = workspaceModel.getVirtualFileUrlManager()

  protected fun <E : WorkspaceEntity> loadedEntries(entityClass: Class<E>): List<E> =
    workspaceEntityStorageBuilder.entities(entityClass).toList()

  protected fun updateWorkspaceModel(updater: (MutableEntityStorage) -> Unit) {
    WriteAction.compute<Unit, Throwable> { workspaceModel.updateProjectModel { updater(it) } }
  }

  fun addEmptyJavaModuleEntity(name: String, entityStorage: MutableEntityStorage): ModuleEntity = entityStorage.addJavaModule(name)
}
