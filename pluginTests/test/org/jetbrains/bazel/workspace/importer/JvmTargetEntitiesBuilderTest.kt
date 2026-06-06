package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.common.timeoutRunBlocking
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
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMap
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.writeText

internal class JvmTargetEntitiesBuilderTest : WorkspaceModelBaseTest() {

  @Test
  fun `writes a single java module with no sources or resources`() = timeoutRunBlocking {
    val target = createRawBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
    )

    runImport(targets = listOf(target))

    val modules = loadedEntries(ModuleEntity::class.java)
    check(modules.size == 1) { "expected 1 module, got ${modules.size}: $modules" }
    check(modules[0].name == target.id.formatAsModuleNameTest()) {
      "unexpected module name ${modules[0].name}"
    }
  }

  @Test
  fun `writes a java module with one source root`() = timeoutRunBlocking {

    val sourcePath = projectBasePath.resolve("src/Foo.java")
    sourcePath.toFile().parentFile.mkdirs()
    sourcePath.toFile().writeText("class Foo {}")
    val target = createRawBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
      sources = listOf(sourcePath),
      baseDirectory = projectBasePath,
    )

    runImport(targets = listOf(target))

    val modules = loadedEntries(ModuleEntity::class.java)
    check(modules.size == 1) { "expected 1 module, got ${modules.size}" }
    val sourceRoots = loadedEntries(SourceRootEntity::class.java)
    check(sourceRoots.isNotEmpty()) { "expected at least one source root" }
  }

  @Test
  fun `writes libraries and modules referencing them`() = timeoutRunBlocking {
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

    runImport(targets = listOf(target), libraries = listOf(libraryItem))

    check(loadedEntries(LibraryEntity::class.java).isNotEmpty()) { "expected a library entity" }
    check(loadedEntries(ModuleEntity::class.java).isNotEmpty()) { "expected a module entity" }
  }

  @Test
  fun `groups sources sharing a parent directory under one content root`() = timeoutRunBlocking {
    disableMergeSourceRoots()
    val srcDir = projectBasePath.resolve("src/main")
    Files.createDirectories(srcDir)
    val fooPath = srcDir.resolve("Foo.java")
    val barPath = srcDir.resolve("Bar.java")
    fooPath.writeText("class Foo {}")
    barPath.writeText("class Bar {}")
    val target = createRawBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
      sources = listOf(fooPath, barPath),
      baseDirectory = projectBasePath,
    )

    runImport(targets = listOf(target))

    val contentRoots = loadedEntries(ContentRootEntity::class.java)
    check(contentRoots.size == 1) {
      "expected 1 content root, got ${contentRoots.size}: ${contentRoots.map { it.url }}"
    }
    check(contentRoots[0].url == srcDir.toVirtualFileUrl(virtualFileUrlManager)) {
      "expected content root URL ${srcDir.toVirtualFileUrl(virtualFileUrlManager)}, got ${contentRoots[0].url}"
    }
    check(contentRoots[0].sourceRoots.size == 2) {
      "expected 2 source roots under the shared content root, got ${contentRoots[0].sourceRoots.size}"
    }
  }

  @Test
  fun `keeps separate content roots for sources in different parent directories`() = timeoutRunBlocking {
    disableMergeSourceRoots()
    val mainDir = projectBasePath.resolve("src/main")
    val testDir = projectBasePath.resolve("src/test")
    Files.createDirectories(mainDir)
    Files.createDirectories(testDir)
    val mainPath = mainDir.resolve("Foo.java")
    val testPath = testDir.resolve("Bar.java")
    mainPath.writeText("class Foo {}")
    testPath.writeText("class Bar {}")
    val target = createRawBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
      sources = listOf(mainPath, testPath),
      baseDirectory = projectBasePath,
    )

    runImport(targets = listOf(target))

    val contentRoots = loadedEntries(ContentRootEntity::class.java)
    val urls = contentRoots.map { it.url }.toSet()
    val expected = setOf(
      mainDir.toVirtualFileUrl(virtualFileUrlManager),
      testDir.toVirtualFileUrl(virtualFileUrlManager),
    )
    check(urls == expected) { "expected content root URLs $expected, got $urls" }
    for (cr in contentRoots) {
      check(cr.sourceRoots.size == 1) {
        "expected exactly one source root under ${cr.url}, got ${cr.sourceRoots.size}"
      }
    }
  }

  @Test
  fun `places content root at the source path when the source lives directly under projectBasePath`() = timeoutRunBlocking {
    disableMergeSourceRoots()
    val sourcePath = projectBasePath.resolve("Foo.java")
    sourcePath.writeText("class Foo {}")
    val target = createRawBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
      sources = listOf(sourcePath),
      baseDirectory = projectBasePath,
    )

    runImport(targets = listOf(target))

    val contentRoots = loadedEntries(ContentRootEntity::class.java)
    check(contentRoots.size == 1) { "expected 1 content root, got ${contentRoots.size}" }
    check(contentRoots[0].url == sourcePath.toVirtualFileUrl(virtualFileUrlManager)) {
      "expected content root at source path ${sourcePath.toVirtualFileUrl(virtualFileUrlManager)}, " +
        "got ${contentRoots[0].url}"
    }
  }

  private fun Label.formatAsModuleNameTest(): String = this.formatAsModuleName(RepoMappingDisabled)

  // BAZEL-3205 grouping logic in SourceRootBuilder.write() only fires when this flag is off.
  private fun disableMergeSourceRoots() {
    Registry.get("bazel.merge.source.roots").setValue(false, disposable)
  }

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
      fileToTargets = File2TargetMap.EMPTY,
      virtualFileUrlManager = virtualFileUrlManager,
      importIJars = false,
      entitySource = BazelProjectEntitySource,
      excludeCompiledSourceCodeInsideJars = true,
      currentCompiledSourceExcludeEntity = null,
    )
    LibraryBuilder.writeAll(
      libraryItems = libraries,
      repoMapping = RepoMappingDisabled,
      importIjars = false,
      virtualFileUrlManager = virtualFileUrlManager,
      entitySource = BazelDummyEntitySource,
      storage = workspaceEntityStorageBuilder,
    )
    JvmTargetEntitiesBuilder(ctx).writeAll(workspaceEntityStorageBuilder)
  }
}
