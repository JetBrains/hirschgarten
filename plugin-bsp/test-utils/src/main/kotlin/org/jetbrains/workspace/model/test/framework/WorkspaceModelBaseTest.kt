package org.jetbrains.workspace.model.test.framework

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.workspaceModel.ide.JpsFileEntitySource
import com.intellij.workspaceModel.ide.JpsProjectConfigLocation
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.WorkspaceEntity
import com.intellij.workspaceModel.storage.WorkspaceEntityStorageBuilder
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addModuleEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path

public open class WorkspaceModelBaseTest {

  protected lateinit var project: Project
  protected lateinit var workspaceModel: WorkspaceModel
  protected lateinit var virtualFileUrlManager: VirtualFileUrlManager

  protected lateinit var projectBaseDirPath: Path
  protected lateinit var projectConfigSource: JpsFileEntitySource

  @BeforeEach
  protected open fun beforeEach() {
    project = emptyProjectTestMock()
    workspaceModel = WorkspaceModel.getInstance(project)
    virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)

    projectBaseDirPath = project.stateStore.projectBasePath
    projectConfigSource = calculateProjectConfigSource(projectBaseDirPath, virtualFileUrlManager)
  }

  private fun emptyProjectTestMock(): Project {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val fixtureBuilder = factory.createFixtureBuilder("test", true)
    val fixture = fixtureBuilder.fixture
    fixture.setUp()

    return fixture.project
  }

  private fun calculateProjectConfigSource(
    projectBaseDirPath: Path,
    virtualFileUrlManager: VirtualFileUrlManager
  ): JpsFileEntitySource {
    val virtualProjectBaseDirPath = projectBaseDirPath.toVirtualFileUrl(virtualFileUrlManager)
    val virtualProjectIdeaDirPath = virtualProjectBaseDirPath.append(".idea/")
    val virtualProjectModulesDirPath = virtualProjectIdeaDirPath.append("modules/")

    val projectLocation = JpsProjectConfigLocation.DirectoryBased(virtualProjectBaseDirPath, virtualProjectIdeaDirPath)

    return JpsFileEntitySource.FileInDirectory(virtualProjectModulesDirPath, projectLocation)
  }

  protected fun <T : Any> runTestWriteAction(action: () -> T): T {
    lateinit var result: T
    WriteCommandAction.runWriteCommandAction(project) { result = action() }

    return result
  }

  protected fun <E : WorkspaceEntity> loadedEntries(entityClass: Class<E>): List<E> =
    workspaceModel.entityStorage.current.entities(entityClass).toList()
}

public abstract class WorkspaceModelWithParentJavaModuleBaseTest : WorkspaceModelBaseTest() {

  protected lateinit var parentModuleEntity: ModuleEntity

  private val parentModuleName = "test-module-root"
  private val parentModuleType = "JAVA_MODULE"

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()

    addParentModuleEntity()
  }

  private fun addParentModuleEntity() {
    WriteCommandAction.runWriteCommandAction(project) {
      workspaceModel.updateProjectModel {
        parentModuleEntity = addParentModuleEntity(it)
      }
    }
  }

  private fun addParentModuleEntity(builder: WorkspaceEntityStorageBuilder): ModuleEntity =
    builder.addModuleEntity(
      name = parentModuleName,
      dependencies = emptyList(),
      source = projectConfigSource,
      type = parentModuleType
    )
}
