package org.jetbrains.magicmetamodel.impl.workspacemodel

import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.workspaceModel.ide.JpsFileEntitySource
import com.intellij.workspaceModel.ide.JpsProjectConfigLocation
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.WorkspaceEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path

// TODO refactor
open class WorkspaceModelBaseTest {

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

  protected infix fun <T, C : Collection<T>, E> C.shouldContainExactlyInAnyOrder(
    expectedWithAssertion: Pair<Collection<E>, (T, E) -> Unit>
  ) {
    val expectedValues = expectedWithAssertion.first
    val assertion = expectedWithAssertion.second

    this shouldHaveSize expectedValues.size

    this.forAll { actual -> expectedValues.forAny { assertion(actual, it) } }
  }

  protected fun <E : WorkspaceEntity> workspaceModelLoadedEntries(entityClass: Class<E>): List<E> =
    workspaceModel.entityStorage.current.entities(entityClass).toList()
}
