package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.golang.sync.GoBuildTarget
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.protobuf.target.ProtobufBuildTarget
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.JavaSourceRootPatterns
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.ProjectViewGlobPattern
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaProviderData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JdepsJar
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmOutputs
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.ScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.junit.jupiter.api.Test
import java.nio.file.Path

private const val TEST_BUFFER_SIZE = 4 * 1024 * 1024

@TestApplication
class SnapshotKryoSerializationTest {

  private val project by projectFixture()

  private val kryo: SnapshotKryo by lazy {
    SnapshotKryo(SnapshotTypeUniverse.build(contributedEntries()))
  }

  private fun contributedEntries(): List<WorkspaceTypeEntry> {
    return WorkspaceTypeContributor.EP_NAME.extensionList
      .flatMap { contributor -> contributor.contribute(project) }
  }

  private inline fun <reified T : Any> serializeAndDeserialize(value: T): T =
    kryo.pool.use { kryo ->
      val output = Output(TEST_BUFFER_SIZE, -1)
      kryo.writeObject(output, value)
      kryo.readObject(Input(output.toBytes()), T::class.java)
    }

  private fun serializeAndDeserializePolymorphic(value: Any): Any =
    kryo.pool.use { kryo ->
      val output = Output(TEST_BUFFER_SIZE, -1)
      kryo.writeClassAndObject(output, value)
      kryo.readClassAndObject(Input(output.toBytes()))
    }

  private fun key(label: String, configuration: String? = null, aspectIds: List<String> = emptyList()): WorkspaceTargetKey =
    WorkspaceTargetKey(
      label = Label.parse(label),
      configuration = WorkspaceConfigurationId.of(configuration),
      aspectIds = WorkspaceAspectIds.of(aspectIds),
    )

  private fun sources(vararg paths: String): SourceFileCollection =
    SourceFileCollectionBuilder.build(relativeRoot = Path.of("/workspace"), paths = paths.map { Path.of(it) })

  private fun rawTarget(
    key: WorkspaceTargetKey,
    dependencies: List<DependencyLabel> = emptyList(),
    data: List<BuildTargetData> = emptyList(),
  ): TestBuildTarget =
    TestBuildTarget(
      key = key,
      dependencies = dependencies,
      kind = TargetKind(
        kind = "java_library",
        languageClasses = setOf(LanguageClass("java", setOf("java"))),
        ruleType = RuleType.LIBRARY,
      ),
      sources = sources("/workspace/src/Main.java", "/external/gen/Gen.java"),
      generatedSources = SourceFileCollection.EMPTY,
      resources = sources("/workspace/resources/app.properties"),
      baseDirectory = Path.of("/workspace/src"),
      data = data,
      generatorName = "my_macro",
      isWorkspace = true,
      isTestOnly = false,
    )

  private fun jvmBuildTarget(): JvmBuildTarget =
    JvmBuildTarget(
      javacOpts = listOf("-parameters"),
      binaryOutputs = sources("/workspace/bazel-bin/foo.jar"),
      rawBinaryOutputs = sources("/workspace/bazel-bin/raw/foo.jar"),
      environmentVariables = mapOf("KEY" to "value"),
      mainClass = "com.example.Main",
      jvmArgs = listOf("-Xmx1g"),
      programArgs = listOf("--flag"),
      resolvedResourceStripPrefix = Path.of("/workspace/resources"),
      outputInterfaceJars = sources("/workspace/bazel-bin/foo-hjar.jar"),
      outputSourceJars = sources("/workspace/bazel-bin/foo-src.jar"),
      generatedJars = listOf(JvmOutputs(binaryJars = sources("/workspace/bazel-bin/gen.jar"))),
      jdepsJars = listOf(JdepsJar(syntheticLabel = Label.parse("@//foo:bar"), jar = Path.of("/workspace/bazel-bin/foo.jdeps"))),
      intellijPluginJars = SourceFileCollection.EMPTY,
      containsInternalJars = true,
      hasExecutableInfo = true,
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
    )

  @Test
  fun `type universe builds from all contributors`() {
    kryo.manifest.entries.size shouldBe contributedEntries().size
  }

  @Test
  fun `partial workspace snapshot`() {
    val rootKey = key("@//foo:bar", configuration = "deadbeef42", aspectIds = listOf("my_aspect"))
    val depKey = key("@//foo:dep", configuration = "deadbeef42")
    val targets = listOf(
      rawTarget(
        key = rootKey,
        dependencies = listOf(DependencyLabel(targetKey = depKey, kind = DependencyLabelKind.COMPILE)),
        data = listOf(jvmBuildTarget()),
      ),
      rawTarget(key = depKey),
    )
    val graph = WorkspaceTargetGraphBuilder.build(rootTargets = setOf(rootKey), targets = targets)

    val configurationId = WorkspaceConfigurationId.of("deadbeef42")
    val targetId2Target = Int2ObjectBiMap<WorkspaceTargetKey>().apply {
      set(1, rootKey)
      set(2, depKey)
    }
    val labelId2Target = Int2ObjectBiMap<Label>().apply {
      set(1, rootKey.label)
      set(2, depKey.label)
    }
    val partial = PersistentWorkspaceSnapshot(
      workspaceName = "_main",
      configurations = mapOf(
        configurationId to WorkspaceConfiguration(
          id = configurationId,
          summary = WorkspaceConfigurationSummary(
            hash = "deadbeef4223",
            mnemonic = "darwin_arm64-fastbuild",
            platformName = "darwin",
            cpu = "arm64",
            isTool = false,
          ),
          fragments = listOf(),
        ),
      ),
      targetGraph = graph,
      syncConfigs = listOf(
        CommonWorkspaceSyncConfig(projectRootDir = Path.of("/workspace"), projectName = "test", importDepth = -1),
        JavaWorkspaceSyncConfig(
          testSourcesPatterns = listOf("**/test/**"),
          importIjars = true,
          excludeCompiledSourceCodeInsideJars = false,
          sourceRootOptimizationMode = SourceRootOptimizationMode.MavenLayout(
            patterns = JavaSourceRootPatterns(
              includes = listOf(ProjectViewGlobPattern(rootDir = Path.of("/workspace"), patterns = listOf("src/**"))),
              excludes = listOf(),
            ),
          ),
        ),
      ),
      repoMapping = BzlmodRepoMapping(
        canonicalRepoNameToLocalPath = mapOf("rules_jvm~" to Path.of("/cache/rules_jvm")),
        apparentRepoNameToCanonicalName = mapOf("rules_jvm" to "rules_jvm~"),
        canonicalRepoNameToPath = mapOf("rules_jvm~" to Path.of("/external/rules_jvm")),
        nonLocalCanonicalRepoNames = setOf(),
      ),
      metadata = WorkspaceSnapshotMetadata(version = 3),
      keyId2Target = targetId2Target,
      labelId2Label = labelId2Target,
    )

    val restored = serializeAndDeserialize(partial)

    restored.workspaceName shouldBe partial.workspaceName
    restored.configurations shouldBe partial.configurations
    restored.syncConfigs shouldBe partial.syncConfigs
    restored.repoMapping shouldBe partial.repoMapping
    restored.metadata shouldBe partial.metadata
    restored.keyId2Target shouldBe partial.keyId2Target
    restored.labelId2Label shouldBe partial.labelId2Label

    val restoredGraph = restored.targetGraph
    restoredGraph.allTargets.map { it.targetKey }.toSet() shouldBe setOf(rootKey, depKey)
    restoredGraph.findTargetByKey(rootKey)?.targetKey shouldBe rootKey
    restoredGraph.findAllTransitiveSuccessors(rootKey).map { it.targetKey }.toSet() shouldBe
      graph.findAllTransitiveSuccessors(rootKey).map { it.targetKey }.toSet()
    restoredGraph.findAllTargetsAtDepth(-1).map { it.targetKey }.toSet() shouldBe
      graph.findAllTargetsAtDepth(-1).map { it.targetKey }.toSet()
  }

  @Test
  fun `empty graph and empty source collection as singletons`() {
    val partial = PersistentWorkspaceSnapshot(
      workspaceName = null,
      configurations = emptyMap(),
      targetGraph = WorkspaceTargetGraph.EMPTY,
      syncConfigs = emptyList(),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(version = 1),
      keyId2Target = Int2ObjectBiMap(),
      labelId2Label = Int2ObjectBiMap(),
    )
    val restored = serializeAndDeserialize(partial)
    (restored.targetGraph === WorkspaceTargetGraph.EMPTY) shouldBe true
    restored.repoMapping shouldBe RepoMappingDisabled
    (serializeAndDeserializePolymorphic(SourceFileCollection.EMPTY) === SourceFileCollection.EMPTY) shouldBe true
  }

  @Test
  fun `mvstore envelope types`() {
    val partialTarget = PartialWorkspaceTarget(
      kind = TargetKind(
        kind = "kt_jvm_library",
        languageClasses = setOf(LanguageClass("kotlin", setOf("kt"))),
        ruleType = RuleType.LIBRARY,
      ),
      baseDirectory = Path.of("/workspace/foo"),
      generatorName = null,
      isWorkspace = true,
      isTestOnly = true,
      tags = listOf("no-ide"),
    )
    val restoredPartial = serializeAndDeserialize(partialTarget)
    restoredPartial.kind shouldBe partialTarget.kind
    restoredPartial.baseDirectory shouldBe partialTarget.baseDirectory
    restoredPartial.generatorName shouldBe partialTarget.generatorName
    restoredPartial.isWorkspace shouldBe partialTarget.isWorkspace
    restoredPartial.isTestOnly shouldBe partialTarget.isTestOnly
    restoredPartial.tags shouldBe partialTarget.tags

    val deps = WorkspaceTargetDeps(
      dependencies = listOf(
        DependencyLabel(targetKey = key("@//foo:dep"), kind = DependencyLabelKind.RUNTIME),
        DependencyLabel(targetKey = key("@//foo:single")),
      ),
    )
    serializeAndDeserialize(deps).dependencies shouldBe deps.dependencies

    val heavy = HeavyWorkspaceTarget(
      sources = sources("/workspace/a/A.kt", "/workspace/a/b/B.kt"),
      generatedSources = SourceFileCollection.EMPTY,
      resources = sources("/external/res.txt"),
    )
    val restoredHeavy = serializeAndDeserialize(heavy)
    restoredHeavy.sources shouldBe heavy.sources
    restoredHeavy.resources shouldBe heavy.resources
  }

  @Test
  fun `every build target data implementation`() {
    val samples: List<BuildTargetData> = listOf(
      jvmBuildTarget(),
      KotlinBuildTarget(
        languageVersion = "2.1",
        apiVersion = "2.1",
        kotlincOptions = listOf("-Xjvm-default=all"),
        associates = listOf(key("@//foo:assoc")),
        moduleName = "foo",
        stdlibHardLinkedJars = sources("/workspace/bazel-bin/kotlin-stdlib.jar"),
        stdlibInferredSourceJars = SourceFileCollection.EMPTY,
        exportedCompilerPluginTargetsList = listOf(key("@//plugin:compiler")),
      ),
      ScalaBuildTarget(
        scalaVersion = "3.4.1",
        sdkJars = sources("/workspace/bazel-bin/scala-sdk.jar"),
        scalacOptions = listOf("-deprecation"),
        scalatestClasspathTargets = listOf(Label.parse("@//scala:test")),
      ),
      JavaProviderData(fullCompileJars = sources("/workspace/bazel-bin/full.jar"), hasApiGeneratingPlugins = true),
      JavaToolchainData(
        sourceVersion = "17",
        targetVersion = "17",
        javaHome = Path.of("/jdk"),
        bootClasspathJavaHome = null,
        isExecConfig = false,
      ),
      PythonBuildTarget(
        version = "3.12",
        interpreter = Path.of("/usr/bin/python3"),
        imports = listOf("src"),
        mainFile = Path.of("/workspace/main.py"),
        mainModule = "main",
        runnerScript = null,
      ),
      GoBuildTarget(
        sdkHomePath = Path.of("/go/sdk"),
        importPath = "main",
        sources = sources("/workspace/main.go"),
        embed = listOf()
      ),
      ProtobufBuildTarget(sources = mapOf("foo/bar.proto" to "/workspace/foo/bar.proto")),
      GoBuildTarget(importPath = "example.com/foo", sources = SourceFileCollection.EMPTY, embed = listOf())
    )

    for (sample in samples) {
      serializeAndDeserializePolymorphic(sample) shouldBe sample
    }
  }
}
