package org.jetbrains.bazel.sync.workspace.mapper.direct

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DirectTargetMapperTest {
  @TempDir
  lateinit var tempDir: Path

  /**
   * Test implementation of DirectTargetMapper for testing the interface contract.
   */
  private class TestDirectTargetMapper : DirectTargetMapper {
    var mapTargetsCalled = false
    var lastTargets: Map<Label, BspTargetInfo.TargetInfo>? = null
    var lastSyncScope: org.jetbrains.bazel.sync.scope.ProjectSyncScope? = null
    var lastContext: MappingContext? = null

    override suspend fun mapTargets(
      targets: Map<Label, BspTargetInfo.TargetInfo>,
      syncScope: org.jetbrains.bazel.sync.scope.ProjectSyncScope,
      context: MappingContext,
    ): DirectMappingResult {
      mapTargetsCalled = true
      lastTargets = targets
      lastSyncScope = syncScope
      lastContext = context

      // Simple mapping for testing
      val modules =
        targets.map { (label, targetInfo) ->
          WorkspaceModuleData(
            name = label.targetName,
            label = label,
            type =
              when {
                targetInfo.kind.contains("java") -> ModuleType.JAVA_MODULE
                targetInfo.kind.contains("kotlin") -> ModuleType.KOTLIN_MODULE
                else -> ModuleType.JAVA_MODULE
              },
            contentRoots =
              listOf(
                ContentRoot(
                  path = context.workspaceRoot.resolve(label.packagePath.toString()),
                  sourceRoots =
                    listOf(
                      SourceRoot(
                        path = context.workspaceRoot.resolve(label.packagePath.toString()),
                        isGenerated = false,
                      ),
                    ),
                ),
              ),
            dependencies =
              targetInfo.dependenciesList.map { dep ->
                ModuleDependency.Module(dep.id)
              },
            languageSettings =
              LanguageSettings(
                primaryLanguage = Language.JAVA,
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

  @Test
  fun `DirectTargetMapper interface should be implementable`() {
    val mapper = TestDirectTargetMapper()
    assertNotNull(mapper)
  }

  @Test
  fun `mapTargets should receive all parameters correctly`() =
    runBlocking {
      val mapper = TestDirectTargetMapper()
      val label = Label.parse("//test:target")
      val targetInfo =
        BspTargetInfo.TargetInfo
          .newBuilder()
          .setId("//test:target")
          .setKind("java_library")
          .build()

      val targets = mapOf(label to targetInfo)
      val syncScope = FirstPhaseSync
      val context =
        MappingContext(
          workspaceRoot = tempDir,
          bazelBin = tempDir.resolve("bazel-bin"),
          importDepth = 3,
          isSharedSourceSupportEnabled = true,
          importIjars = false,
          hasError = false,
        )

      val result = mapper.mapTargets(targets, syncScope, context)

      assertTrue(mapper.mapTargetsCalled)
      assertEquals(targets, mapper.lastTargets)
      assertEquals(syncScope, mapper.lastSyncScope)
      assertEquals(context, mapper.lastContext)

      assertThat(result.workspaceModules).hasSize(1)
      assertEquals("target", result.workspaceModules[0].name)
      assertEquals(label, result.workspaceModules[0].label)
    }

  @Test
  fun `MappingContext should support language-specific context`() {
    val context =
      MappingContext(
        workspaceRoot = tempDir,
        bazelBin = tempDir.resolve("bazel-bin"),
        importDepth = 3,
        isSharedSourceSupportEnabled = false,
        importIjars = true,
        hasError = false,
        languageContext =
          mapOf(
            "kotlin" to KotlinContext(apiVersion = "1.9"),
            "scala" to ScalaContext(version = "2.13"),
          ),
      )

    assertNotNull(context.languageContext["kotlin"])
    assertEquals("1.9", (context.languageContext["kotlin"] as KotlinContext).apiVersion)
  }

  @Test
  fun `DirectMappingResult should handle empty results`() {
    val result =
      DirectMappingResult(
        workspaceModules = emptyList(),
        libraries = emptyList(),
        sdks = SdkData(),
      )

    assertThat(result.workspaceModules).isEmpty()
    assertThat(result.libraries).isEmpty()
    assertThat(result.languageSpecificData).isEmpty()
    assertThat(result.nonModuleTargets).isEmpty()
  }

  @Test
  fun `WorkspaceModuleData should support multiple languages`() {
    val module =
      WorkspaceModuleData(
        name = "mixed-module",
        label = Label.parse("//test:mixed"),
        type = ModuleType.MIXED_MODULE,
        contentRoots = emptyList(),
        dependencies = emptyList(),
        languageSettings =
          LanguageSettings(
            primaryLanguage = Language.JAVA,
            additionalLanguages = setOf(Language.KOTLIN, Language.SCALA),
          ),
      )

    assertEquals(Language.JAVA, module.languageSettings.primaryLanguage)
    assertThat(module.languageSettings.additionalLanguages).containsExactly(Language.KOTLIN, Language.SCALA)
  }

  @Test
  fun `ModuleDependency should support different dependency types`() {
    val moduleDep = ModuleDependency.Module("other-module", DependencyScope.TEST)
    val libraryDep = ModuleDependency.Library("junit", DependencyScope.TEST)
    val sdkDep = ModuleDependency.Sdk("JDK-17", SdkType.JDK)

    when (moduleDep) {
      is ModuleDependency.Module -> {
        assertEquals("other-module", moduleDep.name)
        assertEquals(DependencyScope.TEST, moduleDep.scope)
      }
      else -> error("Expected Module dependency")
    }

    when (libraryDep) {
      is ModuleDependency.Library -> {
        assertEquals("junit", libraryDep.name)
        assertEquals(DependencyScope.TEST, libraryDep.scope)
      }
      else -> error("Expected Library dependency")
    }

    when (sdkDep) {
      is ModuleDependency.Sdk -> {
        assertEquals("JDK-17", sdkDep.name)
        assertEquals(SdkType.JDK, sdkDep.type)
      }
      else -> error("Expected SDK dependency")
    }
  }

  @Test
  fun `LibraryData should support Maven coordinates`() {
    val library =
      LibraryData(
        name = "guava",
        classJars = setOf(tempDir.resolve("guava.jar")),
        mavenCoordinates =
          MavenCoordinates(
            groupId = "com.google.guava",
            artifactId = "guava",
            version = "32.0.0-jre",
          ),
      )

    assertNotNull(library.mavenCoordinates)
    assertEquals("com.google.guava", library.mavenCoordinates?.groupId)
    assertEquals("guava", library.mavenCoordinates?.artifactId)
    assertEquals("32.0.0-jre", library.mavenCoordinates?.version)
  }

  @Test
  fun `SdkData should support multiple SDK types`() {
    val sdkData =
      SdkData(
        jdks =
          mapOf(
            "JDK-17" to
              JdkInfo(
                name = "JDK-17",
                homePath = tempDir.resolve("jdk17"),
                version = "17.0.8",
              ),
          ),
        scalaSdks =
          mapOf(
            "Scala-2.13" to
              ScalaSdkInfo(
                name = "Scala-2.13",
                version = "2.13.12",
                compilerClasspath = listOf(tempDir.resolve("scala-compiler.jar")),
                libraryClasspath = listOf(tempDir.resolve("scala-library.jar")),
              ),
          ),
      )

    assertThat(sdkData.jdks).hasSize(1)
    assertEquals("17.0.8", sdkData.jdks["JDK-17"]?.version)
    assertThat(sdkData.scalaSdks).hasSize(1)
    assertEquals("2.13.12", sdkData.scalaSdks["Scala-2.13"]?.version)
  }

  @Test
  fun `MappingDiagnostics should collect warnings and errors`() {
    val diagnostics = MappingDiagnostics()

    diagnostics.warnings.add("Missing source jar for library X")
    diagnostics.errors.add("Failed to resolve dependency Y")
    diagnostics.performanceMetrics["mappingTimeMs"] = 1234L
    diagnostics.statistics["totalModules"] = 42

    assertThat(diagnostics.warnings).hasSize(1)
    assertThat(diagnostics.errors).hasSize(1)
    assertEquals(1234L, diagnostics.performanceMetrics["mappingTimeMs"])
    assertEquals(42, diagnostics.statistics["totalModules"])
  }

  @Test
  fun `DirectTargetMapper should handle partial sync correctly`() =
    runBlocking {
      val mapper = TestDirectTargetMapper()
      val targetsToSync = listOf(Label.parse("//app:main"), Label.parse("//lib:util"))
      val syncScope = PartialProjectSync(targetsToSync)

      val context =
        MappingContext(
          workspaceRoot = tempDir,
          bazelBin = tempDir.resolve("bazel-bin"),
          importDepth = 1,
          isSharedSourceSupportEnabled = false,
          importIjars = false,
          hasError = false,
        )

      mapper.mapTargets(emptyMap(), syncScope, context)

      when (val scope = mapper.lastSyncScope) {
        is PartialProjectSync -> {
          assertThat(scope.targetsToSync).containsExactlyElementsIn(targetsToSync)
        }
        else -> error("Expected PartialProjectSync")
      }
    }

  // Helper data classes for testing language-specific context
  private data class KotlinContext(val apiVersion: String)

  private data class ScalaContext(val version: String)
}
