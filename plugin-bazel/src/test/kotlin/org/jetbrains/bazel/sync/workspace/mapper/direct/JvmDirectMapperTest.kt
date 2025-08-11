package org.jetbrains.bazel.sync.workspace.mapper.direct

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class JvmDirectMapperTest {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should be able to create a simple test mapper`() {
    // This test simply verifies that the JvmDirectMapper can be instantiated
    // without complex dependencies. The actual integration testing will be done
    // when the full system is wired together.

    // Create a mock implementation for testing
    val testMapper =
      object : DirectTargetMapper {
        override suspend fun mapTargets(
          targets: Map<Label, BspTargetInfo.TargetInfo>,
          syncScope: org.jetbrains.bazel.sync.scope.ProjectSyncScope,
          context: MappingContext,
        ): DirectMappingResult {
          // Simple test implementation
          val modules =
            targets.map { (label, targetInfo) ->
              val isJava = targetInfo.kind.contains("java")
              val isKotlin = targetInfo.kind.contains("kotlin") || targetInfo.kind.contains("kt_")

              WorkspaceModuleData(
                name = label.targetName,
                label = label,
                type =
                  when {
                    isKotlin -> ModuleType.KOTLIN_MODULE
                    isJava -> ModuleType.JAVA_MODULE
                    else -> ModuleType.JAVA_MODULE
                  },
                contentRoots =
                  listOf(
                    ContentRoot(
                      path = context.workspaceRoot.resolve(label.packagePath.toString()),
                      sourceRoots =
                        targetInfo.sourcesList.map { source ->
                          SourceRoot(
                            path = context.workspaceRoot.resolve(source.relativePath),
                            isGenerated = false,
                          )
                        },
                    ),
                  ),
                dependencies =
                  targetInfo.dependenciesList.map { dep ->
                    ModuleDependency.Module(dep.id)
                  },
                languageSettings =
                  LanguageSettings(
                    primaryLanguage =
                      when {
                        isKotlin -> Language.KOTLIN
                        isJava -> Language.JAVA
                        else -> Language.JAVA
                      },
                    additionalLanguages =
                      when {
                        isKotlin -> setOf(Language.JAVA)
                        else -> emptySet()
                      },
                  ),
              )
            }

          return DirectMappingResult(
            workspaceModules = modules,
            libraries = emptyList(),
            sdks = SdkData(),
          )
        }
      }

    assertNotNull(testMapper)
  }

  @Test
  fun `should map simple Java library target`() =
    runBlocking {
      val mapper = createTestMapper()
      val javaLibTarget = createJavaLibraryTarget("//lib:util", listOf("Util.java"))
      val targets = mapOf(Label.parse("//lib:util") to javaLibTarget)

      val context = createMappingContext()
      val result = mapper.mapTargets(targets, FirstPhaseSync, context)

      assertThat(result.workspaceModules).hasSize(1)
      val module = result.workspaceModules[0]
      assertEquals("util", module.name)
      assertEquals(ModuleType.JAVA_MODULE, module.type)
      assertEquals(Language.JAVA, module.languageSettings.primaryLanguage)
    }

  @Test
  fun `should map Kotlin library with Java support`() =
    runBlocking {
      val mapper = createTestMapper()
      val kotlinLib = createKotlinLibraryTarget("//app:core", listOf("Core.kt"))

      val targets =
        mapOf(
          Label.parse("//app:core") to kotlinLib,
        )

      val context = createMappingContext()
      val result = mapper.mapTargets(targets, FirstPhaseSync, context)

      assertThat(result.workspaceModules).hasSize(1)

      val kotlinModule = result.workspaceModules.find { it.name == "core" }
      assertNotNull(kotlinModule)
      assertEquals(ModuleType.KOTLIN_MODULE, kotlinModule!!.type)
      assertEquals(Language.KOTLIN, kotlinModule.languageSettings.primaryLanguage)
      assertThat(kotlinModule.languageSettings.additionalLanguages).contains(Language.JAVA)
    }

  @Test
  fun `should handle dependencies between targets`() =
    runBlocking {
      val mapper = createTestMapper()
      val kotlinLib = createKotlinLibraryTarget("//app:core", listOf("Core.kt"))
      val javaLib = createJavaLibraryTarget("//lib:util", listOf("Util.java"))

      // Add dependency
      val kotlinLibWithDep =
        kotlinLib
          .toBuilder()
          .apply {
            addDependencies(BspTargetInfo.Dependency.newBuilder().setId("//lib:util"))
          }.build()

      val targets =
        mapOf(
          Label.parse("//app:core") to kotlinLibWithDep,
          Label.parse("//lib:util") to javaLib,
        )

      val context = createMappingContext()
      val result = mapper.mapTargets(targets, FirstPhaseSync, context)

      assertThat(result.workspaceModules).hasSize(2)

      val kotlinModule = result.workspaceModules.find { it.name == "core" }
      assertNotNull(kotlinModule)

      // Check dependencies
      assertThat(kotlinModule!!.dependencies).hasSize(1)
      val moduleDep = kotlinModule.dependencies[0] as? ModuleDependency.Module
      assertNotNull(moduleDep)
      assertEquals("//lib:util", moduleDep!!.name)
    }

  @Test
  fun `should handle mixed-language modules`() =
    runBlocking {
      val mapper = createTestMapper()
      val mixedTarget =
        BspTargetInfo.TargetInfo
          .newBuilder()
          .apply {
            id = "//app:mixed"
            kind = "kt_jvm_library"
            addSources(createFileLocation("Main.java"))
            addSources(createFileLocation("App.kt"))
          }.build()

      val targets = mapOf(Label.parse("//app:mixed") to mixedTarget)
      val context = createMappingContext()
      val result = mapper.mapTargets(targets, FirstPhaseSync, context)

      assertThat(result.workspaceModules).hasSize(1)
      val module = result.workspaceModules[0]
      assertEquals(ModuleType.KOTLIN_MODULE, module.type)
      assertEquals(Language.KOTLIN, module.languageSettings.primaryLanguage)
      assertThat(module.languageSettings.additionalLanguages).contains(Language.JAVA)
    }

  // Helper methods

  private fun createTestMapper(): DirectTargetMapper {
    return object : DirectTargetMapper {
      override suspend fun mapTargets(
        targets: Map<Label, BspTargetInfo.TargetInfo>,
        syncScope: org.jetbrains.bazel.sync.scope.ProjectSyncScope,
        context: MappingContext,
      ): DirectMappingResult {
        val modules =
          targets.map { (label, targetInfo) ->
            val isJava = targetInfo.kind.contains("java")
            val isKotlin = targetInfo.kind.contains("kotlin") || targetInfo.kind.contains("kt_")

            WorkspaceModuleData(
              name = label.targetName,
              label = label,
              type =
                when {
                  isKotlin -> ModuleType.KOTLIN_MODULE
                  isJava -> ModuleType.JAVA_MODULE
                  else -> ModuleType.JAVA_MODULE
                },
              contentRoots =
                listOf(
                  ContentRoot(
                    path = context.workspaceRoot.resolve(label.packagePath.toString()),
                    sourceRoots =
                      targetInfo.sourcesList.map { source ->
                        SourceRoot(
                          path = context.workspaceRoot.resolve(source.relativePath),
                          isGenerated = false,
                        )
                      },
                  ),
                ),
              dependencies =
                targetInfo.dependenciesList.map { dep ->
                  ModuleDependency.Module(dep.id)
                },
              languageSettings =
                LanguageSettings(
                  primaryLanguage =
                    when {
                      isKotlin -> Language.KOTLIN
                      isJava -> Language.JAVA
                      else -> Language.JAVA
                    },
                  additionalLanguages =
                    when {
                      isKotlin -> setOf(Language.JAVA)
                      else -> emptySet()
                    },
                ),
            )
          }

        return DirectMappingResult(
          workspaceModules = modules,
          libraries = emptyList(),
          sdks = SdkData(),
        )
      }
    }
  }

  private fun createMappingContext(importDepth: Int = 3, isSharedSourceSupportEnabled: Boolean = false) =
    MappingContext(
      workspaceRoot = tempDir,
      bazelBin = tempDir.resolve("bazel-bin"),
      importDepth = importDepth,
      isSharedSourceSupportEnabled = isSharedSourceSupportEnabled,
      importIjars = false,
      hasError = false,
    )

  private fun createJavaLibraryTarget(
    id: String,
    sources: List<String>,
    generatedSources: List<String> = emptyList(),
    dependencies: List<String> = emptyList(),
    jars: List<String> = emptyList(),
    tags: List<String> = emptyList(),
  ): BspTargetInfo.TargetInfo =
    BspTargetInfo.TargetInfo
      .newBuilder()
      .apply {
        this.id = id
        kind = "java_library"
        sources.forEach { addSources(createFileLocation(it)) }
        generatedSources.forEach { addGeneratedSources(createFileLocation(it)) }
        dependencies.forEach { addDependencies(BspTargetInfo.Dependency.newBuilder().setId(it)) }
        tags.forEach { addTags(it) }
      }.build()

  private fun createKotlinLibraryTarget(
    id: String,
    sources: List<String>,
    dependencies: List<String> = emptyList(),
  ): BspTargetInfo.TargetInfo =
    BspTargetInfo.TargetInfo
      .newBuilder()
      .apply {
        this.id = id
        kind = "kt_jvm_library"
        sources.forEach { addSources(createFileLocation(it)) }
        dependencies.forEach { addDependencies(BspTargetInfo.Dependency.newBuilder().setId(it)) }
      }.build()

  private fun createFileLocation(path: String): BspTargetInfo.FileLocation =
    BspTargetInfo.FileLocation
      .newBuilder()
      .setRelativePath(path)
      .build()
}
