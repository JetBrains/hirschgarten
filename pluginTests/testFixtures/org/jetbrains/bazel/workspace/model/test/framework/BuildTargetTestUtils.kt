package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.OutputRoot
import java.nio.file.Path
import kotlin.io.path.Path

val TEST_OUTPUT_ROOT: OutputRoot = OutputRoot.of(listOf("k8-fastbuild", "bin"))

val testOutputResolver: OutputLocationResolver = DefaultOutputLocationResolver.createHardlinkResolving(testBazelInfo())

fun testLocation(path: Path): OutputLocation = OutputLocation.Host(path.toString())

fun generatedTestLocation(relativePath: String): OutputLocation = OutputLocation.Output(TEST_OUTPUT_ROOT, relativePath)

fun resolveTestLocation(location: OutputLocation): Path? = testOutputResolver.resolve(location)

fun testLocations(paths: List<Path>): OutputLocationCollection = OutputLocationCollectionBuilder.ofLocations(paths.map(::testLocation))

fun createTestBuildTarget(
  id: Label = Label.parse("//target"),
  dependencies: List<DependencyLabel> = emptyList(),
  kind: TargetKind = TargetKind(
    kind = "java_library",
    ruleType = RuleType.LIBRARY,
    languageClasses = setOf(JavaLanguageClass.JAVA),
  ),
  sources: List<Path> = emptyList(),
  generatedSources: List<Path> = emptyList(),
  resources: List<Path> = emptyList(),
  baseDirectory: Path = Path("/base/dir"),
  data: List<BuildTargetData> = emptyList(),
  isTestOnly: Boolean = false,
): TestBuildTarget =
  TestBuildTarget(
    key = WorkspaceTargetKey(label = id),
    dependencies = dependencies,
    kind = kind,
    sources = SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = sources),
    generatedSources = SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = generatedSources),
    resources = SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = resources),
    baseDirectory = baseDirectory,
    data = data,
    isTestOnly = isTestOnly,
  )
