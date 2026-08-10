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
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContextBuilder
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.DefaultJvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmDependency
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractJvmBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createTestBuildTarget
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.writeText

private val FOO_BAR: Label = Label.parse("//foo:bar")

internal class JvmTargetEntitiesBuilderTest : WorkspaceModelBaseTest() {

  @Test
  fun `writes a single java module with no sources or resources`() = timeoutRunBlocking {
    val target = createTestBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
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
    val target = createTestBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
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
    val target = createTestBuildTarget(
      id = Label.parse("//app"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
      dependencies = listOf(
        DependencyLabel(targetKey = WorkspaceTargetKey(label = libLabel), kind = DependencyLabelKind.COMPILE),
      ),
      data = listOf(JvmBuildTarget()),
    )

    val appKey = target.key.copy(aspectIds = WorkspaceAspectIds.EMPTY)
    runImport(
      targets = listOf(target),
      resolved = mapOf(appKey to JvmResolvedTarget(appKey, listOf(libraryItem), emptyList(), null, "")),
    )

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
    val target = createTestBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
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
    val target = createTestBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
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
    val target = createTestBuildTarget(
      id = Label.parse("//foo"),
      kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
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
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA))
    val normal = createTestBuildTarget(id = label, kind = kind)
      .copy(key = WorkspaceTargetKey(label = label, configuration = WorkspaceConfigurationId.of("00000f1")))
    val exec = createTestBuildTarget(id = label, kind = kind)
      .copy(key = WorkspaceTargetKey(label = label, configuration = WorkspaceConfigurationId.of("00000f2")))

    runImport(targets = listOf(normal, exec))

    val names = loadedEntries(ModuleEntity::class.java).map { it.name }.toSet()
    val base = label.formatAsModuleNameTest()
    names shouldContainExactlyInAnyOrder setOf("$base-00000f1", "$base-00000f2")
  }

  @Test
  fun `a label with a single configuration keeps its plain module name`() = timeoutRunBlocking {
    val label = Label.parse("//foo")
    val target = createTestBuildTarget(id = label)
      .copy(key = WorkspaceTargetKey(label = label, configuration = WorkspaceConfigurationId.of("00000f1")))

    runImport(targets = listOf(target))

    val names = loadedEntries(ModuleEntity::class.java).map { it.name }
    names shouldContainExactly listOf(label.formatAsModuleNameTest())
  }

  @Test
  fun `dependency resolves to the module of its exact configuration`(): Unit = timeoutRunBlocking {
    val foo = Label.parse("//foo")
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA))
    val fooNormal = createTestBuildTarget(id = foo, kind = kind)
      .copy(key = WorkspaceTargetKey(label = foo, configuration = WorkspaceConfigurationId.of("00000f1")))
    val fooExec = createTestBuildTarget(id = foo, kind = kind)
      .copy(key = WorkspaceTargetKey(label = foo, configuration = WorkspaceConfigurationId.of("00000f2")))
    val app = createTestBuildTarget(
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
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA))
    val sourcePath = projectBasePath.resolve("Proto.java")
    sourcePath.writeText("class Proto {}")
    val bare = createTestBuildTarget(id = label, kind = kind, sources = listOf(sourcePath), baseDirectory = projectBasePath)
    val withProvider = createTestBuildTarget(
      id = label,
      kind = kind,
      data = listOf(JvmBuildTarget()),
    ).copy(key = WorkspaceTargetKey(label = label, aspectIds = WorkspaceAspectIds.of(listOf("//proto:proto_aspect"))))

    val protoKey = WorkspaceTargetKey(label = label)
    runImport(
      targets = listOf(bare, withProvider),
      resolved = mapOf(protoKey to JvmResolvedTarget(protoKey, emptyList(), emptyList(), null, "11")),
    )

    val modules = loadedEntries(ModuleEntity::class.java)
    modules shouldHaveSize 1
    modules.single().javaSettings?.languageLevelId.shouldNotBeNull()
  }

  @Test
  fun `merges JvmBuildTarget data from two aspect variants of the same target`(): Unit = timeoutRunBlocking {
    val foo = Label.parse("//foo")
    val a = Label.parse("//a")
    val b = Label.parse("//b")
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA))
    val depA = createTestBuildTarget(id = a, kind = kind)
    val depB = createTestBuildTarget(id = b, kind = kind)
    val variantA = createTestBuildTarget(
      id = foo,
      kind = kind,
      dependencies = listOf(DependencyLabel(WorkspaceTargetKey(label = a))),
      data = listOf(JvmBuildTarget()),
    ).copy(key = WorkspaceTargetKey(label = foo, aspectIds = WorkspaceAspectIds.of(listOf("//foo:aspect_a"))))
    val variantB = createTestBuildTarget(
      id = foo,
      kind = kind,
      dependencies = listOf(DependencyLabel(WorkspaceTargetKey(label = b))),
      data = listOf(JvmBuildTarget()),
    ).copy(key = WorkspaceTargetKey(label = foo, aspectIds = WorkspaceAspectIds.of(listOf("//foo:aspect_b"))))

    runImport(targets = listOf(depA, depB, variantA, variantB))

    val fooModule = loadedEntries(ModuleEntity::class.java).single { it.name == foo.formatAsModuleNameTest() }
    val moduleDeps = fooModule.dependencies.filterIsInstance<ModuleDependency>().map { it.module.name }
    moduleDeps shouldContain a.formatAsModuleNameTest()
    moduleDeps shouldContain b.formatAsModuleNameTest()
  }

  @Test
  fun `a library dependency shadowing a source module becomes a exported module dependency`(): Unit = timeoutRunBlocking {
    val kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA))
    val producer = Label.parse("//producer")
    val producerJar = Path("/out/producer.jar")
    // producer is an in-scope source module whose output jar is reached by //app only through a jdeps library
    val producerTarget = createTestBuildTarget(
      id = producer,
      kind = kind,
      sources = listOf(Path("/base/dir/Producer.java")),
      data = listOf(JvmBuildTarget(outputInterfaceJars = SourceFileCollectionBuilder.build(listOf(producerJar)))),
    )
    val shadowLibraryKey = WorkspaceTargetKey(label = Label.parse("//producer-jdeps-lib"))
    val shadowLibrary = LibraryItem(
      key = shadowLibraryKey,
      ijars = emptyList(),
      jars = listOf(producerJar),
      sourceJars = emptyList(),
      mavenCoordinates = null,
      containsInternalJars = false,
    )
    val app = createTestBuildTarget(
      id = Label.parse("//app"),
      kind = kind,
      data = listOf(JvmBuildTarget()),
    )

    val producerKey = producerTarget.key.copy(aspectIds = WorkspaceAspectIds.EMPTY)
    val appKey = app.key.copy(aspectIds = WorkspaceAspectIds.EMPTY)
    runImport(
      targets = listOf(producerTarget, app),
      resolved = mapOf(
        producerKey to JvmResolvedTarget(producerKey, emptyList(), emptyList(), null, ""),
        appKey to JvmResolvedTarget(
          key = appKey,
          libraries = listOf(shadowLibrary),
          jvmDependencies = listOf(JvmDependency.LibraryDependency(DependencyLabel(shadowLibraryKey))),
          javaHome = null,
          javaVersion = "",
        ),
      ),
    )

    val appModule = loadedEntries(ModuleEntity::class.java).single { it.name == Label.parse("//app").formatAsModuleNameTest() }
    val dep = appModule.dependencies.filterIsInstance<ModuleDependency>().single { it.module.name == producer.formatAsModuleNameTest() }
    dep.exported shouldBe true
  }

  @Test
  fun `a rules_kotlin module name derived from the label does not rename the module`(): Unit = timeoutRunBlocking {
    runImport(targets = listOf(kotlinTarget(FOO_BAR, moduleName = "foo-bar")))

    moduleNames() shouldContainExactly listOf(FOO_BAR.formatAsModuleNameTest())
  }

  @Test
  fun `a rules_kotlin module name derived from a root package label does not rename the module`(): Unit = timeoutRunBlocking {
    val root = Label.parse("//:bar")
    runImport(targets = listOf(kotlinTarget(root, moduleName = "-bar")))

    moduleNames() shouldContainExactly listOf(root.formatAsModuleNameTest())
  }

  @Test
  fun `an explicitly set rules_kotlin module name renames the module`(): Unit = timeoutRunBlocking {
    runImport(targets = listOf(kotlinTarget(FOO_BAR, moduleName = "explicit.module.name")))

    moduleNames() shouldContainExactly listOf("explicit.module.name")
  }

  @Test
  fun `a derived rules_kotlin module name does not add the test suffix to a test target`(): Unit = timeoutRunBlocking {
    runImport(targets = listOf(kotlinTarget(FOO_BAR, moduleName = "foo-bar", isTestOnly = true)))

    moduleNames() shouldContainExactly listOf(FOO_BAR.formatAsModuleNameTest())
  }

  @Test
  fun `an explicitly set rules_kotlin module name adds the test suffix to a test target`(): Unit = timeoutRunBlocking {
    runImport(targets = listOf(kotlinTarget(FOO_BAR, moduleName = "explicit.module.name", isTestOnly = true)))

    moduleNames() shouldContainExactly listOf("explicit.module.name-test")
  }

  @Test
  fun `the ide-module-name tag wins over a rules_kotlin module name`(): Unit = timeoutRunBlocking {
    val target = kotlinTarget(FOO_BAR, moduleName = "explicit.module.name")
      .copy(tags = listOf("ide-module-name=tagged.module.name"))

    runImport(targets = listOf(target))

    moduleNames() shouldContainExactly listOf("tagged.module.name")
  }

  @Test
  fun `a rules_kotlin module name derived from an associate label does not rename the module`(): Unit = timeoutRunBlocking {
    val associate = Label.parse("//:friend")
    val target = kotlinTarget(Label.parse("//:consumer"), moduleName = "-friend", associates = listOf(associate))

    runImport(targets = listOf(target, kotlinTarget(associate, moduleName = "-friend")))

    moduleNames() shouldContainExactlyInAnyOrder listOf(
      Label.parse("//:consumer").formatAsModuleNameTest(),
      associate.formatAsModuleNameTest(),
    )
  }

  private fun moduleNames(): List<String> = loadedEntries(ModuleEntity::class.java).map { it.name }

  private fun kotlinTarget(
    label: Label,
    moduleName: String,
    isTestOnly: Boolean = false,
    associates: List<Label> = emptyList(),
  ): TestBuildTarget =
    createTestBuildTarget(
      id = label,
      kind = TargetKind(kind = "kt_jvm_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.KOTLIN)),
      data = listOf(
        JvmBuildTarget(),
        KotlinBuildTarget(
          languageVersion = null,
          apiVersion = null,
          kotlincOptions = emptyList(),
          associates = associates.map { WorkspaceTargetKey(label = it) },
          moduleName = moduleName,
        ),
      ),
      isTestOnly = isTestOnly,
    )

  private suspend fun runImport(
    targets: List<BuildTarget>,
    resolved: Map<WorkspaceTargetKey, JvmResolvedTarget> = defaultResolved(targets),
  ) {
    val calc = DefaultJvmPackagePrefixCalculator(SourceRootOptimizationMode.Disabled)
    calc.calculate(targets)
    val jvmPackagePrefixes: JvmPackagePrefixCalculator = calc
    val plan = JvmImportPlan(rawTargets = targets, jvmResolved = resolved)
    val naming = GlobalNamingContextBuilder.create(RepoMappingDisabled)
      .apply { plan.declareNames(this) }
      .build()
    val ctx = ImportContext(
      plan = plan,
      naming = naming,
      jvmResolved = resolved,
      projectName = "test-project",
      projectBasePath = projectBasePath,
      defaultJdkName = null,
      testSourcesGlob = ProjectViewGlobSet(projectBasePath, emptyList()),
      packagePrefixes = jvmPackagePrefixes,
      fileToTargets = FileToTargetMap.EMPTY,
      virtualFileUrlManager = virtualFileUrlManager,
      importIJars = false,
      entitySource = BazelProjectEntitySource,
      excludeCompiledSourceCodeInsideJars = true,
      currentCompiledSourceExcludeEntity = null,
    )
    // JvmTargetEntitiesBuilder writes ctx.libraries (sourced from the resolver) in its phase 0
    JvmTargetEntitiesBuilder(ctx).writeAll(workspaceEntityStorageBuilder)
  }

  private fun defaultResolved(targets: List<BuildTarget>): Map<WorkspaceTargetKey, JvmResolvedTarget> {
    val moduleKeys = targets.filter { extractJvmBuildTarget(it) != null }.map { it.key.copy(aspectIds = WorkspaceAspectIds.EMPTY) }.toSet()
    return targets.filter { extractJvmBuildTarget(it) != null }
      .groupBy { it.key.copy(aspectIds = WorkspaceAspectIds.EMPTY) }
      .mapValues { (key, group) ->
        val jvmDeps = group.flatMap { t ->
          t.dependencies.map { dep ->
            if (dep.targetKey.copy(aspectIds = WorkspaceAspectIds.EMPTY) in moduleKeys) JvmDependency.ModuleDependency(dep)
            else JvmDependency.LibraryDependency(dep)
          }
        }.distinct()
        JvmResolvedTarget(key = key, libraries = emptyList(), jvmDependencies = jvmDeps, javaHome = null, javaVersion = "")
      }
  }
}
