package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.DefaultJvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.SourceItem
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

internal class JvmTargetEntitiesBuilderTest : WorkspaceModelBaseTest() {

  @Test
  fun `writes a single java module with no sources or resources`() {
    runInEdtAndWait {
      val target = createRawBuildTarget(
        id = Label.parse("//foo"),
        kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
      )

      runTestWriteAction { runImport(targets = listOf(target)) }

      val modules = loadedEntries(ModuleEntity::class.java)
      check(modules.size == 1) { "expected 1 module, got ${modules.size}: $modules" }
      check(modules[0].name == target.id.formatAsModuleNameTest()) {
        "unexpected module name ${modules[0].name}"
      }
    }
  }

  @Test
  fun `writes a java module with one source root`() {
    runInEdtAndWait {
      val sourcePath = projectBasePath.resolve("src/Foo.java")
      sourcePath.toFile().parentFile.mkdirs()
      sourcePath.toFile().writeText("class Foo {}")
      val target = createRawBuildTarget(
        id = Label.parse("//foo"),
        kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
        sources = listOf(SourceItem(path = sourcePath, generated = false)),
        baseDirectory = projectBasePath,
      )

      runTestWriteAction { runImport(targets = listOf(target)) }

      val modules = loadedEntries(ModuleEntity::class.java)
      check(modules.size == 1) { "expected 1 module, got ${modules.size}" }
      val sourceRoots = loadedEntries(SourceRootEntity::class.java)
      check(sourceRoots.isNotEmpty()) { "expected at least one source root" }
    }
  }

  @Test
  fun `writes libraries and modules referencing them`() {
    runInEdtAndWait {
      val libLabel = Label.parse("//libfoo")
      val libraryItem = LibraryItem(
        id = libLabel,
        ijars = emptyList(),
        jars = listOf(Path("/dep/foo.jar")),
        sourceJars = emptyList(),
        mavenCoordinates = null,
        containsInternalJars = false,
      )
      val target = createRawBuildTarget(
        id = Label.parse("//app"),
        kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
        dependencies = listOf(
          DependencyLabel(label = libLabel, kind = DependencyLabelKind.COMPILE),
        ),
        data = listOf(
          JvmBuildTarget(
            javaHome = Path("/fake/jdk"),
            javaVersion = "11",
            libraries = listOf(libraryItem),
          ),
        ),
      )

      runTestWriteAction { runImport(targets = listOf(target), libraries = listOf(libraryItem)) }

      check(loadedEntries(LibraryEntity::class.java).isNotEmpty()) { "expected a library entity" }
      check(loadedEntries(ModuleEntity::class.java).isNotEmpty()) { "expected a module entity" }
    }
  }

  private fun Label.formatAsModuleNameTest(): String = this.formatAsModuleName(RepoMappingDisabled)

  private suspend fun runImport(
    targets: List<RawBuildTarget>,
    libraries: List<LibraryItem> = emptyList(),
  ) {
    val calc = DefaultJvmPackagePrefixCalculator(SourceRootOptimizationMode.Disabled)
    calc.calculate(targets)
    val jvmPackagePrefixes: JvmPackagePrefixCalculator = calc
    val ctx = ImportContext(
      targets = targets,
      libraries = libraries,
      repoMapping = RepoMappingDisabled,
      projectName = "test-project",
      projectBasePath = projectBasePath,
      defaultJdkName = null,
      testSourcesGlob = ProjectViewGlobSet(projectBasePath, emptyList()),
      testTargets = emptySet(),
      packagePrefixes = jvmPackagePrefixes,
      fileToTargets = emptyMap(),
      virtualFileUrlManager = virtualFileUrlManager,
    )
    LibraryBuilder.write(
      libraryItems = libraries,
      repoMapping = RepoMappingDisabled,
      importIjars = false,
      virtualFileUrlManager = virtualFileUrlManager,
      entitySource = BazelDummyEntitySource,
      storage = workspaceEntityStorageBuilder,
    )
    JvmTargetEntitiesBuilder(ctx).writeAll(workspaceEntityStorageBuilder)
  }

  private fun runTestWriteAction(action: suspend () -> Unit) {
    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
      runBlocking { action() }
    }
  }
}
