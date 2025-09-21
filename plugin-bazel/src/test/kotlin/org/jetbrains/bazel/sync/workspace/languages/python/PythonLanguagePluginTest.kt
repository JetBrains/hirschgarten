package org.jetbrains.bazel.sync.workspace.languages.python

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.PythonTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class PythonLanguagePluginTest {
  private fun resolver(tmp: Path? = null): BazelPathsResolver = BazelPathsResolverMock.create(tmp ?: Path.of(""))

  private fun fileLocation(path: String, isExternal: Boolean = false): FileLocation =
    FileLocation.newBuilder().setRelativePath(path).setIsExternal(isExternal).build()

  private fun pythonInfo(
    version: String? = null,
    interpreter: FileLocation? = null,
    imports: List<String> = emptyList(),
    generated: List<FileLocation> = emptyList(),
    isCodeGenerator: Boolean = false,
  ): PythonTargetInfo =
    PythonTargetInfo.newBuilder()
      .apply { if (version != null) this.version = version }
      .apply { if (interpreter != null) this.interpreter = interpreter }
      .addAllImports(imports)
      .addAllGeneratedSources(generated)
      .setIsCodeGenerator(isCodeGenerator)
      .build()

  private fun target(id: String, kind: String, py: PythonTargetInfo, sources: List<FileLocation> = emptyList(), deps: List<String> = emptyList()): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .setPythonTargetInfo(py)
      .addAllSources(sources)
      .addAllDependencies(deps.map { depId -> BspTargetInfo.Dependency.newBuilder().setId(depId).build() })
      .build()

  private fun workspace(): WorkspaceContext =
    WorkspaceContext(
      targets = emptyList(),
      directories = emptyList(),
      buildFlags = emptyList(),
      syncFlags = emptyList(),
      debugFlags = emptyList(),
      bazelBinary = null,
      allowManualTargetsSync = false,
      dotBazelBspDirPath = Path.of(""),
      importDepth = 1,
      enabledRules = emptyList(),
      ideJavaHomeOverride = null,
      shardSync = false,
      targetShardSize = 0,
      shardingApproach = null,
      importRunConfigurations = emptyList(),
      gazelleTarget = null,
      indexAllFilesInDirectories = false,
      pythonCodeGeneratorRuleNames = emptyList(),
      importIjars = false,
      deriveInstrumentationFilterFromTargets = false,
      indexAdditionalFilesInDirectories = emptyList(),
    )

  @Test
  fun `prepareSync sets defaults and data uses them when missing`() {
    val pathsResolver = resolver()
    val plugin = PythonLanguagePlugin(pathsResolver)

    val defaultInterp = fileLocation("/usr/bin/python3")
    val defaultInfo = pythonInfo(version = "3.11.1", interpreter = defaultInterp)
    val defaultTarget = target("//tooling:py", "py_binary", defaultInfo)

    val gen = listOf(fileLocation("bazel-bin/gen/codegen.py"))
    val tInfo = pythonInfo(version = null, interpreter = null, generated = gen)
    val t = target("//app:lib", "py_library", tInfo)

    // Prepare with both targets so plugin captures defaults
    plugin.prepareSync(sequenceOf(defaultTarget, t), workspace())

    // Build context+graph
    val graph = DependencyGraph(setOf(Label.parse(t.id)), mapOf(Label.parse(defaultTarget.id) to defaultTarget, Label.parse(t.id) to t))
    val context = LanguagePluginContext(t, graph, org.jetbrains.bazel.commons.RepoMappingDisabled, pathsResolver)

    val data = kotlinx.coroutines.runBlocking { plugin.createBuildTargetData(context, t) }
    assertNotNull(data)
    data!!
    // Defaults applied
    assertEquals("3.11.1", data.version)
    assertTrue((data.interpreter ?: error("no interpreter")).toString().contains("/usr/bin/python3"))

    // Additional generated sources should be exposed by calculateAdditionalSources
    val additional = plugin.calculateAdditionalSources(t).toList()
    assertTrue(additional.any { it.fileName.toString() == "codegen.py" })
  }

  @Test
  fun `sourceDependencies include site-packages dir of external sources from transitive deps`() {
    val pathsResolver = resolver(Path.of("/ws"))
    val plugin = PythonLanguagePlugin(pathsResolver)

    // Dependency target with an external source under site-packages
    val externalSrc = fileLocation("external/pypi__requests/site-packages/requests/__init__.py", isExternal = true)
    val depInfo = pythonInfo(version = "3.10")
    val dep = target("//third_party/py:requests", "py_library", depInfo, sources = listOf(externalSrc))

    // Root python target depends on dep
    val rootInfo = pythonInfo(version = null, interpreter = null)
    val root = target("//svc:app", "py_binary", rootInfo, deps = listOf(dep.id))

    plugin.prepareSync(sequenceOf(dep, root), workspace())

    val graph = DependencyGraph(setOf(Label.parse(root.id)), mapOf(Label.parse(root.id) to root, Label.parse(dep.id) to dep))
    val context = LanguagePluginContext(root, graph, org.jetbrains.bazel.commons.RepoMappingDisabled, pathsResolver)

    val data = kotlinx.coroutines.runBlocking { plugin.createBuildTargetData(context, root) }
    assertNotNull(data)
    data!!

    // Expect a dependency pointing to site-packages directory
    val deps = data.sourceDependencies.map { it.toString().replace('\\', '/') }
    assertTrue(deps.any { it.endsWith("/site-packages") })
  }
}
