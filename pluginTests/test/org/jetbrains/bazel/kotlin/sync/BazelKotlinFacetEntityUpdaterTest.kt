package org.jetbrains.bazel.kotlin.sync

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.testFramework.runInEdtAndWait
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.BazelKotlinFacetEntityUpdater
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.KotlinAddendum
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.IKotlinFacetSettings
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

      val kotlinBuildTarget =
        KotlinBuildTarget(
          languageVersion = "1.8",
          apiVersion = "1.8",
          kotlincOptions = listOf("-opt-in=com.example.ExperimentalApi"),
          associates = listOf("//target4", "//target5").map { Label.parse(it) },
          moduleName = "kotlin-module",
          jvmBuildTarget =
            JvmBuildTarget(
              javaHome = javaHome,
              javaVersion = javaVersion,
            ),
        )

      // when
      val retrievedFacetSettings = addToWorkspaceModelAndGetFacet(kotlinBuildTarget)

      // then
      retrievedFacetSettings.additionalVisibleModuleNames shouldBe kotlinBuildTarget.associates.map { it.toString() }
      retrievedFacetSettings
        .compilerArguments
        .shouldBeInstanceOf<K2JVMCompilerArguments>()
        .apply {
          moduleName shouldBe "kotlin-module"
          apiVersion shouldBe "1.8"
          languageVersion shouldBe "1.8"
          autoAdvanceLanguageVersion shouldBe false
          autoAdvanceApiVersion shouldBe false
          optIn.shouldNotBeNull() shouldHaveSingleElement "com.example.ExperimentalApi"
        }
    }
  }

  // https://youtrack.jetbrains.com/issue/BAZEL-2350
  @Test
  fun `should parse Kotlin version from kotlincOptions if rules_jvm is used and not rules_kotlin`() {
    runInEdtAndWait {
      // given
      val javaHome = Path("/fake/path/to/local_jdk")
      val javaVersion = "11"

      val kotlinBuildTarget =
        KotlinBuildTarget(
          languageVersion = null,
          apiVersion = null,
          kotlincOptions = listOf("-language-version=1.8", "-api-version=1.9"),
          associates = emptyList(),
          moduleName = "kotlin-module",
          jvmBuildTarget =
            JvmBuildTarget(
              javaHome = javaHome,
              javaVersion = javaVersion,
            ),
        )

      // when
      val retrievedFacetSettings = addToWorkspaceModelAndGetFacet(kotlinBuildTarget)

      // then
      retrievedFacetSettings
        .compilerArguments
        .shouldBeInstanceOf<K2JVMCompilerArguments>()
        .apply {
          moduleName shouldBe "kotlin-module"
          apiVersion shouldBe "1.9"
          languageVersion shouldBe "1.8"
          autoAdvanceLanguageVersion shouldBe false
          autoAdvanceApiVersion shouldBe false
        }
    }
  }

  private fun addToWorkspaceModelAndGetFacet(kotlinBuildTarget: KotlinBuildTarget): IKotlinFacetSettings {
    val module =
      GenericModuleInfo(
        name = "module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            Dependency("module2"),
            Dependency("module3"),
          ),
        kind =
          TargetKind(
            kind = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
        associates = kotlinBuildTarget.associates.map { it.toString() },
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
        jvmJdkName = "${projectBasePath.name}-${kotlinBuildTarget.jvmBuildTarget!!.javaVersion}",
        kotlinAddendum =
          KotlinAddendum(
            languageVersion = kotlinBuildTarget.languageVersion,
            apiVersion = kotlinBuildTarget.apiVersion,
            moduleName = kotlinBuildTarget.moduleName,
            kotlincOptions = kotlinBuildTarget.kotlincOptions,
          ),
      )

    updateWorkspaceModel {
      val returnedModuleEntity =
        addEmptyJavaModuleEntity(
          module.name,
          it,
        )
      addKotlinFacetEntity(javaModule, returnedModuleEntity, it)
    }

    val moduleManager = ModuleManager.getInstance(project)
    val retrievedModule = moduleManager.findModuleByName(module.name)
    retrievedModule.shouldNotBeNull()
    val facetManager = FacetManager.getInstance(retrievedModule)
    val facet = facetManager.getFacetByType(KotlinFacetType.TYPE_ID)
    facet.shouldNotBeNull()
    val retrievedFacetSettings = facet.configuration.settings
    return retrievedFacetSettings
  }

  private fun addKotlinFacetEntity(
    javaModule: JavaModule,
    parentEntity: ModuleEntity,
    builder: MutableEntityStorage,
  ) = runBlocking {
    BazelKotlinFacetEntityUpdater().addEntity(builder, javaModule, parentEntity, projectBasePath)
  }
}
