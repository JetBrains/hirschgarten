package org.jetbrains.workspace.model.test.framework

import com.google.idea.testing.BazelTestApplication
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.testFramework.rules.ProjectModelExtension
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

private val JAVA_ROOT_TYPE = SourceRootTypeId("java-source")
private const val JAVA_SDK_NAME = "11"
private const val JAVA_SDK_TYPE = "JavaSDK"

@BazelTestApplication
public open class WorkspaceModelBaseTest {
  protected lateinit var workspaceEntityStorageBuilder: MutableEntityStorage

  @JvmField
  @RegisterExtension
  protected val projectModel: ProjectModelExtension = ProjectModelExtension()

  @BeforeEach
  protected open fun beforeEach() {
    workspaceEntityStorageBuilder = (workspaceModel as WorkspaceModelImpl).getBuilderSnapshot().builder
  }

  protected val project: Project
    get() = projectModel.project

  protected val projectBasePath: Path
    get() = projectModel.projectRootDir

  protected val workspaceModel: WorkspaceModel
    get() = WorkspaceModel.getInstance(project)

  protected val virtualFileUrlManager: VirtualFileUrlManager
    get() = workspaceModel.getVirtualFileUrlManager()

  protected fun <T : Any> runTestWriteAction(action: () -> T): T {
    lateinit var result: T
    WriteCommandAction.runWriteCommandAction(project) { result = action() }

    return result
  }

  protected fun <E : WorkspaceEntity> loadedEntries(entityClass: Class<E>): List<E> =
    workspaceEntityStorageBuilder.entities(entityClass).toList()

  protected fun updateWorkspaceModel(updater: (MutableEntityStorage) -> Unit) {
    WriteAction.compute<Unit, Throwable> { workspaceModel.updateProjectModel { updater(it) } }
  }

  public fun addEmptyJavaModuleEntity(name: String, entityStorage: MutableEntityStorage): ModuleEntity =
    entityStorage.addEntity(
      ModuleEntity(
        name = name,
        dependencies = listOf(
          SdkDependency(SdkId(JAVA_SDK_NAME, JAVA_SDK_TYPE)),
          ModuleSourceDependency,
        ),
        entitySource = object : EntitySource {},
      ) {
        this.type = ModuleTypeId(StdModuleTypes.JAVA.id)
      },
    )
}

public abstract class WorkspaceModelWithParentModuleBaseTest : WorkspaceModelBaseTest() {
  protected lateinit var parentModuleEntity: ModuleEntity

  private val parentModuleName = "test-module-root"
  public abstract val parentModuleType: ModuleTypeId

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()

    addParentModuleEntity()
  }

  private fun addParentModuleEntity() {
    parentModuleEntity = addParentModuleEntity(workspaceEntityStorageBuilder)
  }

  private fun addParentModuleEntity(builder: MutableEntityStorage): ModuleEntity =
    builder.addEntity(
      ModuleEntity(
        name = parentModuleName,
        dependencies = emptyList(),
        entitySource = object : EntitySource {},
      ) {
        this.type = parentModuleType
      },
    )
}

public abstract class WorkspaceModelWithParentJavaModuleBaseTest : WorkspaceModelWithParentModuleBaseTest() {
  override val parentModuleType = ModuleTypeId("JAVA_MODULE")
}

public abstract class WorkspaceModelWithParentPythonModuleBaseTest : WorkspaceModelWithParentModuleBaseTest() {
  override val parentModuleType = ModuleTypeId("PYTHON_MODULE")
}
