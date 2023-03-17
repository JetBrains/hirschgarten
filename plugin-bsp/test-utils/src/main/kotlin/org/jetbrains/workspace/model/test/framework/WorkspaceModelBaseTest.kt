package org.jetbrains.workspace.model.test.framework

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addModuleEntity
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path
import kotlin.io.path.Path

public open class WorkspaceModelBaseTest {

  protected lateinit var project: Project
  protected lateinit var workspaceModel: WorkspaceModel
  protected lateinit var workspaceEntityStorageBuilder: MutableEntityStorage
  protected lateinit var virtualFileUrlManager: VirtualFileUrlManager
  protected val projectBasePath: Path = Path("")

  @BeforeEach
  protected open fun beforeEach() {
    project = emptyProjectTestMock()
    workspaceModel = WorkspaceModel.getInstance(project)
    workspaceEntityStorageBuilder = workspaceModel.getBuilderSnapshot().builder
    virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
  }

  private fun emptyProjectTestMock(): Project {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val fixtureBuilder = factory.createFixtureBuilder("test", true)
    val fixture = fixtureBuilder.fixture
    fixture.setUp()

    return fixture.project
  }

  protected fun <T : Any> runTestWriteAction(action: () -> T): T {
    lateinit var result: T
    WriteCommandAction.runWriteCommandAction(project) { result = action() }

    return result
  }

  protected fun <E : WorkspaceEntity> loadedEntries(entityClass: Class<E>): List<E> =
    workspaceEntityStorageBuilder.entities(entityClass).toList()
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
    parentModuleEntity = addParentModuleEntity(workspaceEntityStorageBuilder)
  }

  private fun addParentModuleEntity(builder: MutableEntityStorage): ModuleEntity =
    builder.addModuleEntity(
      name = parentModuleName,
      dependencies = emptyList(),
      source = object : EntitySource {},
      type = parentModuleType
    )
}
