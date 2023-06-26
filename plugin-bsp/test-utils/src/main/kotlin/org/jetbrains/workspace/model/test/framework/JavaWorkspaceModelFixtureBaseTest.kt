package org.jetbrains.workspace.model.test.framework

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.TestIndexingModeSupporter
import com.intellij.testFramework.TestIndexingModeSupporter.IndexingMode
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaIndexingModeCodeInsightTestFixture.Companion.wrapFixture
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.workspaceModel.ide.getInstance
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

private const val JAVA_ROOT_TYPE = "java-source"

public abstract class JavaWorkspaceModelFixtureBaseTest : TestIndexingModeSupporter {
  private var indexingMode = IndexingMode.SMART
  private lateinit var workspaceModel: WorkspaceModel
  private lateinit var virtualFileUrlManager: VirtualFileUrlManager
  private lateinit var jdk: Sdk
  protected lateinit var fixture: JavaCodeInsightTestFixture

  private val languageLevel = LanguageLevel.JDK_17

  @BeforeEach
  protected open fun beforeEach(testInfo: TestInfo) {
    initializeFixture(testInfo.displayName)
    initializeOthers()
  }

  private fun initializeFixture(name: String) {
    val projectBuilder =
      IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name)
    fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.fixture)
    fixture = wrapFixture(fixture, indexingMode)
    val moduleFixtureBuilder = projectBuilder.addModule(JavaModuleFixtureBuilder::class.java)

    moduleFixtureBuilder.addSourceContentRoot(fixture.tempDirPath)
    fixture.setUp()
  }

  protected val project: Project
    get() = fixture.project

  private fun initializeOthers() {
    workspaceModel = WorkspaceModel.getInstance(project)
    virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
    LanguageLevelProjectExtension.getInstance(project).languageLevel = languageLevel
    jdk = IdeaTestUtil.getMockJdk(languageLevel.toJavaVersion())
    runInEdtAndWait {
      WriteAction.compute<Unit, Throwable> {
        ProjectJdkTable.getInstance().addJdk(jdk, fixture.testRootDisposable)
      }
    }
  }

  public fun updateWorkspaceModel(updater: (MutableEntityStorage) -> Unit) {
    WriteAction.compute<Unit, Throwable> { workspaceModel.updateProjectModel { updater(it) } }
  }

  @AfterEach
  protected open fun afterEach() {
    fixture.tearDown()
  }

  public fun addEmptyJavaModuleEntity(name: String, entityStorage: MutableEntityStorage): ModuleEntity =
    entityStorage.addEntity(
      ModuleEntity(
        name = name,
        dependencies = listOf(
          ModuleDependencyItem.SdkDependency(jdk.name, jdk.sdkType.name),
          ModuleDependencyItem.ModuleSourceDependency
        ),
        entitySource = object : EntitySource {}
      ) {
        this.type = ModuleTypeId.JAVA_MODULE
      }
    )

  public fun addJavaSourceRootEntities(files: List<VirtualFile>,
                                       packagePrefix: String,
                                       module: ModuleEntity,
                                       entityStorage: MutableEntityStorage): List<JavaSourceRootPropertiesEntity> =
    files.map {
      entityStorage.addEntity(
        ContentRootEntity(
          url = virtualFileUrlManager.fromUrl(it.url),
          excludedPatterns = emptyList(),
          entitySource = module.entitySource
        ) { this.module = module }
      )
    }.map {
      entityStorage.addEntity(
        SourceRootEntity(
          url = it.url,
          rootType = JAVA_ROOT_TYPE,
          entitySource = it.entitySource
        ) { this.contentRoot = it }
      )
    }.map {
      entityStorage.addEntity(
        JavaSourceRootPropertiesEntity(
          generated = false,
          packagePrefix = packagePrefix,
          entitySource = it.entitySource
        ) { this.sourceRoot = it }
      )
    }

  override fun getIndexingMode(): IndexingMode = indexingMode

  override fun setIndexingMode(mode: IndexingMode) {
    indexingMode = mode
  }
}
