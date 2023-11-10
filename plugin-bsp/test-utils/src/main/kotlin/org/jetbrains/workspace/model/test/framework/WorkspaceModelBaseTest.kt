package org.jetbrains.workspace.model.test.framework

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.rules.ProjectModelExtension
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.workspaceModel.ide.getInstance
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

private const val JAVA_ROOT_TYPE = "java-source"

@TestApplication
public open class WorkspaceModelBaseTest {
  protected lateinit var workspaceEntityStorageBuilder: MutableEntityStorage
  protected lateinit var jdk: Sdk
  private val languageLevel = LanguageLevel.JDK_17

  @JvmField
  @RegisterExtension
  protected val projectModel: ProjectModelExtension = ProjectModelExtension()

  @BeforeEach
  protected open fun beforeEach() {
    workspaceEntityStorageBuilder = workspaceModel.getBuilderSnapshot().builder
    jdk = IdeaTestUtil.getMockJdk(languageLevel.toJavaVersion())
  }

  protected val project: Project
    get() = projectModel.project

  protected val projectBasePath: Path
    get() = projectModel.projectRootDir

  protected val workspaceModel: WorkspaceModel
    get() = WorkspaceModel.getInstance(project)

  protected val virtualFileUrlManager: VirtualFileUrlManager
    get() = VirtualFileUrlManager.getInstance(project)

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
          ModuleDependencyItem.SdkDependency(jdk.name, jdk.sdkType.name),
          ModuleDependencyItem.ModuleSourceDependency,
        ),
        entitySource = object : EntitySource {},
      ) {
        this.type = ModuleTypeId.JAVA_MODULE
      },
    )

  public fun addJavaSourceRootEntities(
    files: List<VirtualFile>,
    packagePrefix: String,
    module: ModuleEntity,
    entityStorage: MutableEntityStorage,
  ): List<JavaSourceRootPropertiesEntity> =
    files.map {
      entityStorage.addEntity(
        ContentRootEntity(
          url = virtualFileUrlManager.fromUrl(it.url),
          excludedPatterns = emptyList(),
          entitySource = module.entitySource,
        ) { this.module = module },
      )
    }.map {
      entityStorage.addEntity(
        SourceRootEntity(
          url = it.url,
          rootType = JAVA_ROOT_TYPE,
          entitySource = it.entitySource,
        ) { this.contentRoot = it },
      )
    }.map {
      entityStorage.addEntity(
        JavaSourceRootPropertiesEntity(
          generated = false,
          packagePrefix = packagePrefix,
          entitySource = it.entitySource,
        ) { this.sourceRoot = it },
      )
    }
}

public abstract class WorkspaceModelWithParentModuleBaseTest : WorkspaceModelBaseTest() {
  protected lateinit var parentModuleEntity: ModuleEntity

  private val parentModuleName = "test-module-root"
  public abstract val parentModuleType: String

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
  override val parentModuleType: String = "JAVA_MODULE"
}

public abstract class WorkspaceModelWithParentPythonModuleBaseTest : WorkspaceModelWithParentModuleBaseTest() {
  override val parentModuleType: String = "PYTHON_MODULE"
}
