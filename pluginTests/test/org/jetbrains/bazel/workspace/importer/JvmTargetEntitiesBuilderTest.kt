package org.jetbrains.bazel.workspace.importer

import com.intellij.java.workspace.entities.javaSettings
import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsLibraryName
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.DefaultJvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.JvmDependency
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
      key = WorkspaceTargetKey(label = libLabel),
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
        DependencyLabel(targetKey = WorkspaceTargetKey(label = libLabel), kind = DependencyLabelKind.COMPILE),
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

  @Test
  fun `disambiguates module names for a label imported under multiple configurations`(): Unit = timeoutRunBlocking {
    val label = Label.parse("//foo")
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA))
    val normal = createRawBuildTarget(id = label, kind = kind)
      .copy(key = WorkspaceTargetKey(label = label, configuration = WorkspaceConfigurationId.of("00000f1")))
    val exec = createRawBuildTarget(id = label, kind = kind)
      .copy(key = WorkspaceTargetKey(label = label, configuration = WorkspaceConfigurationId.of("00000f2")))

    runImport(targets = listOf(normal, exec))

    val names = loadedEntries(ModuleEntity::class.java).map { it.name }.toSet()
    val base = label.formatAsModuleNameTest()
    names shouldContainExactlyInAnyOrder setOf("$base-00000f1", "$base-00000f2")
  }

  @Test
  fun `a label with a single configuration keeps its plain module name`() = timeoutRunBlocking {
    val label = Label.parse("//foo")
    val target = createRawBuildTarget(id = label)
      .copy(key = WorkspaceTargetKey(label = label, configuration = WorkspaceConfigurationId.of("00000f1")))

    runImport(targets = listOf(target))

    val names = loadedEntries(ModuleEntity::class.java).map { it.name }
    names shouldContainExactly listOf(label.formatAsModuleNameTest())
  }

  @Test
  fun `dependency resolves to the module of its exact configuration`(): Unit = timeoutRunBlocking {
    val foo = Label.parse("//foo")
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA))
    val fooNormal = createRawBuildTarget(id = foo, kind = kind)
      .copy(key = WorkspaceTargetKey(label = foo, configuration = WorkspaceConfigurationId.of("00000f1")))
    val fooExec = createRawBuildTarget(id = foo, kind = kind)
      .copy(key = WorkspaceTargetKey(label = foo, configuration = WorkspaceConfigurationId.of("00000f2")))
    val app = createRawBuildTarget(
      id = Label.parse("//app"),
      kind = kind,
      dependencies = listOf(
        DependencyLabel(
          targetKey = WorkspaceTargetKey(label = foo, configuration = WorkspaceConfigurationId.of("00000f2")),
          kind = DependencyLabelKind.COMPILE,
        ),
      ),
    )

    runImport(targets = listOf(fooNormal, fooExec, app))

    val appModule = loadedEntries(ModuleEntity::class.java).single { it.name == Label.parse("//app").formatAsModuleNameTest() }
    val moduleDeps = appModule.dependencies.filterIsInstance<ModuleDependency>().map { it.module.name }
    val fooBase = foo.formatAsModuleNameTest()
    moduleDeps shouldContain "$fooBase-00000f2"
    moduleDeps shouldNotContain "$fooBase-00000f1"
  }

  // BAZEL-3205 grouping logic in SourceRootBuilder.write() only fires when this flag is off.
  private fun disableMergeSourceRoots() {
    Registry.get("bazel.merge.source.roots").setValue(false, disposable)
  }

  @Test
  fun `merges aspect variants so a provider carried only by the aspect variant survives`(): Unit = timeoutRunBlocking {
    val label = Label.parse("//proto")
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA))
    val sourcePath = projectBasePath.resolve("Proto.java")
    sourcePath.writeText("class Proto {}")
    val bare = createRawBuildTarget(id = label, kind = kind, sources = listOf(sourcePath), baseDirectory = projectBasePath)
    val withProvider = createRawBuildTarget(
      id = label,
      kind = kind,
      data = listOf(JvmBuildTarget(javaHome = Path("/fake/jdk"), javaVersion = "11")),
    ).copy(key = WorkspaceTargetKey(label = label, aspectIds = WorkspaceAspectIds.of(listOf("//proto:proto_aspect"))))

    runImport(targets = listOf(bare, withProvider))

    val modules = loadedEntries(ModuleEntity::class.java)
    modules shouldHaveSize 1
    modules.single().javaSettings?.languageLevelId.shouldNotBeNull()
  }

  @Test
  fun `merges JvmBuildTarget data from two aspect variants of the same target`(): Unit = timeoutRunBlocking {
    val foo = Label.parse("//foo")
    val a = Label.parse("//a")
    val b = Label.parse("//b")
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA))
    val depA = createRawBuildTarget(id = a, kind = kind)
    val depB = createRawBuildTarget(id = b, kind = kind)
    val variantA = createRawBuildTarget(
      id = foo,
      kind = kind,
      data = listOf(
        JvmBuildTarget(
          javaVersion = "11",
          jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(WorkspaceTargetKey(label = a)))),
        ),
      ),
    ).copy(key = WorkspaceTargetKey(label = foo, aspectIds = WorkspaceAspectIds.of(listOf("//foo:aspect_a"))))
    val variantB = createRawBuildTarget(
      id = foo,
      kind = kind,
      data = listOf(
        JvmBuildTarget(
          javaVersion = "11",
          jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(WorkspaceTargetKey(label = b)))),
        ),
      ),
    ).copy(key = WorkspaceTargetKey(label = foo, aspectIds = WorkspaceAspectIds.of(listOf("//foo:aspect_b"))))

    runImport(targets = listOf(depA, depB, variantA, variantB))

    val fooModule = loadedEntries(ModuleEntity::class.java).single { it.name == foo.formatAsModuleNameTest() }
    val moduleDeps = fooModule.dependencies.filterIsInstance<ModuleDependency>().map { it.module.name }
    moduleDeps shouldContain a.formatAsModuleNameTest()
    moduleDeps shouldContain b.formatAsModuleNameTest()
  }

  private suspend fun runImport(
    targets: List<RawBuildTarget>,
    libraries: List<LibraryItem> = emptyList(),
  ) {
    val calc = DefaultJvmPackagePrefixCalculator(SourceRootOptimizationMode.Disabled)
    calc.calculate(targets)
    val jvmPackagePrefixes: JvmPackagePrefixCalculator = calc
    val ctx = ImportContext(
      targets = targets.map { WorkspaceTarget(it.key, it) },
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
      importIjars = false,
      virtualFileUrlManager = virtualFileUrlManager,
      entitySource = BazelDummyEntitySource,
      libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
      storage = workspaceEntityStorageBuilder,
    )
    JvmTargetEntitiesBuilder(ctx).writeAll(workspaceEntityStorageBuilder)
  }
}
