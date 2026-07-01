package org.jetbrains.bazel.kotlin.sync

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.testFramework.runInEdtAndWait
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.BazelKotlinFacetEntityUpdater
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.importer.KotlinOptions
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.IKotlinFacetSettings
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

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
          associates = listOf("//target4", "//target5").map { WorkspaceTargetKey(label = Label.parse(it)) },
          moduleName = "kotlin-module",
        )
      val jvmBuildTarget = JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = javaVersion,
      )

      // when
      val retrievedFacetSettings = addToWorkspaceModelAndGetFacet(kotlinBuildTarget, jvmBuildTarget)

      // then
      retrievedFacetSettings.additionalVisibleModuleNames shouldBe kotlinBuildTarget.associates.map { it.label.toString() }
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
        )
      val jvmBuildTarget = JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = javaVersion,
      )

      // when
      val retrievedFacetSettings = addToWorkspaceModelAndGetFacet(kotlinBuildTarget, jvmBuildTarget)

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

  private fun addToWorkspaceModelAndGetFacet(
    kotlinBuildTarget: KotlinBuildTarget,
    jvmBuildTarget: JvmBuildTarget,
  ): IKotlinFacetSettings {
    @Suppress("UNUSED_VARIABLE")
    val unused = jvmBuildTarget // reserved for future SDK plumbing in this fixture
    val moduleName = "module1"

    updateWorkspaceModel {
      val returnedModuleEntity = addEmptyJavaModuleEntity(moduleName, it)
      addKotlinFacetEntity(kotlinBuildTarget, returnedModuleEntity, it)
    }

    val moduleManager = ModuleManager.getInstance(project)
    val retrievedModule = moduleManager.findModuleByName(moduleName)
    retrievedModule.shouldNotBeNull()
    val facetManager = FacetManager.getInstance(retrievedModule)
    val facet = facetManager.getFacetByType(KotlinFacetType.TYPE_ID)
    facet.shouldNotBeNull()
    return facet.configuration.settings
  }

  private fun addKotlinFacetEntity(
    kotlinBuildTarget: KotlinBuildTarget,
    parentEntity: ModuleEntity,
    builder: MutableEntityStorage,
  ) = runBlocking {
    val kotlinOptions = KotlinOptions(
      languageVersion = kotlinBuildTarget.languageVersion,
      apiVersion = kotlinBuildTarget.apiVersion,
      moduleName = kotlinBuildTarget.moduleName,
      kotlincOptions = kotlinBuildTarget.kotlincOptions,
    )
    BazelKotlinFacetEntityUpdater().addEntity(
      diff = builder,
      parentModuleEntity = parentEntity,
      kotlinOptions = kotlinOptions,
      isTestModule = false,
      associates = kotlinBuildTarget.associates.map { it.label.toString() }.toSet(),
    )
  }
}
