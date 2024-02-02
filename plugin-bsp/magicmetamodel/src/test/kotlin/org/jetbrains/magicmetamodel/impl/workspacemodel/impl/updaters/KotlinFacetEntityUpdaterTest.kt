package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import com.intellij.facet.FacetManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.testFramework.runInEdtAndWait
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.KotlinBuildTarget
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.workspaceModel.KotlinSettingsEntity
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlinAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.name

@DisplayName("kotlinFacetEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
class KotlinFacetEntityUpdaterTest : WorkspaceModelBaseTest() {
  @Test
  fun `should add KotlinFacet when given KotlinAddendum`() {
    runInEdtAndWait {
      // given
      val javaHome = "/fake/path/to/local_jdk"
      val javaVersion = "11"

      val associates = listOf("//target4", "target5")

      val kotlinBuildTarget = KotlinBuildTarget(
        languageVersion = "1.8",
        apiVersion = "1.8",
        kotlincOptions = listOf(),
        associates = associates.map { BuildTargetIdentifier(it) },
        jvmBuildTarget = JvmBuildTarget().also {
          it.javaHome = javaHome
          it.javaVersion = javaVersion
        }
      )

      val module = GenericModuleInfo(
        name = "module1",
        type = "JAVA_MODULE",
        modulesDependencies = listOf(
          ModuleDependency("module2"),
          ModuleDependency("module3"),
        ),
        librariesDependencies = listOf(),
        associates = associates.map { ModuleDependency(it) },
      )

      val baseDirContentRoot = ContentRoot(
        path = projectBasePath.toAbsolutePath(),
        excludedPaths = listOf(),
      )
      val javaModule = JavaModule(
        genericModuleInfo = module,
        baseDirContentRoot = baseDirContentRoot,
        sourceRoots = listOf(),
        resourceRoots = listOf(),
        moduleLevelLibraries = listOf(),
        jvmJdkName = "${projectBasePath.name}-$javaVersion",
        kotlinAddendum = KotlinAddendum(
          languageVersion = kotlinBuildTarget.languageVersion,
          apiVersion = kotlinBuildTarget.apiVersion,
          kotlincOptions = kotlinBuildTarget.kotlincOptions,
        ),
      )

      // when
      updateWorkspaceModel {
        val returnedModuleEntity = addEmptyJavaModuleEntity(module.name, it)
        addKotlinFacetEntity(javaModule, returnedModuleEntity, it)
      }

      // then
      val moduleManager = ModuleManager.getInstance(project)
      val retrievedModule = moduleManager.findModuleByName(module.name)
      retrievedModule.shouldNotBeNull()
      val facetManager = FacetManager.getInstance(retrievedModule)
      val facet = facetManager.getFacetByType(KotlinFacetType.TYPE_ID)
      facet.shouldNotBeNull()
      val retrievedFacetSettings = facet.configuration.settings
      retrievedFacetSettings.additionalVisibleModuleNames shouldBe associates
    }
  }

  private fun addKotlinFacetEntity(
    javaModule: JavaModule,
    parentEntity: ModuleEntity,
    builder: MutableEntityStorage,
  ): KotlinSettingsEntity {
    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(
        builder,
        virtualFileUrlManager,
        projectBasePath,
        project
      )
    val kotlinFacetEntityUpdater = KotlinFacetEntityUpdater(workspaceModelEntityUpdaterConfig, projectBasePath)
    return kotlinFacetEntityUpdater.addEntity(javaModule, parentEntity)
  }
}
