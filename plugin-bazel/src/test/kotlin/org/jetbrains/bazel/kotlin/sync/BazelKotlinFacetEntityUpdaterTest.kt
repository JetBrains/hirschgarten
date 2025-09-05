package org.jetbrains.bazel.kotlin.sync

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.testFramework.runInEdtAndWait
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.KotlinAddendum
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.name

class BazelKotlinFacetEntityUpdaterTest : WorkspaceModelBaseTest() {
  @Test
  fun `should add KotlinFacet when given KotlinAddendum`() {
    runInEdtAndWait {
      // given
      val javaHome = Path("/fake/path/to/local_jdk")
      val javaVersion = "11"

      val associates = listOf("//target4", "target5")

      val kotlinBuildTarget =
        KotlinBuildTarget(
          languageVersion = "1.8",
          apiVersion = "1.8",
          kotlincOptions = listOf(),
          associates = associates.map { Label.parse(it) },
          jvmBuildTarget =
            JvmBuildTarget(
              javaHome = javaHome,
              javaVersion = javaVersion,
            ),
        )

      val module =
        GenericModuleInfo(
          name = "module1",
          type = ModuleTypeId("JAVA_MODULE"),
          dependencies =
            listOf(
              "module2",
              "module3",
            ),
          kind =
            TargetKind(
              kindString = "java_library",
              ruleType = RuleType.LIBRARY,
              languageClasses = setOf(LanguageClass.JAVA),
            ),
          associates = associates,
        )

      val baseDirContentRoot =
        ContentRoot(
          path = projectBasePath.toAbsolutePath(),
        )
      val javaModule =
        JavaModule(
          genericModuleInfo = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = listOf(),
          resourceRoots = listOf(),
          jvmJdkName = "${projectBasePath.name}-$javaVersion",
          kotlinAddendum =
            KotlinAddendum(
              languageVersion = kotlinBuildTarget.languageVersion,
              apiVersion = kotlinBuildTarget.apiVersion,
              kotlincOptions = kotlinBuildTarget.kotlincOptions,
            ),
        )

      // when
      updateWorkspaceModel {
        val returnedModuleEntity =
          addEmptyJavaModuleEntity(
            module.name,
            it,
          )
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
  ) = runBlocking {
    BazelKotlinFacetEntityUpdater().addEntity(builder, javaModule, parentEntity, projectBasePath)
  }
}
