package org.jetbrains.bazel.sync.workspace.mapper.phased

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.orLatestSupported
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.RawPhasedTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

private fun createMockWorkspaceContext(allowManualTargetsSync: Boolean): WorkspaceContext =
  WorkspaceContext(
    targets = listOf(ExcludableValue.included(Label.parse("//..."))),
    directories = listOf(ExcludableValue.included(Path("."))),
    buildFlags = emptyList(),
    syncFlags = emptyList(),
    debugFlags = emptyList(),
    bazelBinary = Path("bazel"),
    allowManualTargetsSync = allowManualTargetsSync,
    dotBazelBspDirPath = Path(".bazelbsp"),
    importDepth = -1,
    enabledRules = emptyList(),
    ideJavaHomeOverride = Path("java_home"),
    shardSync = false,
    targetShardSize = 1000,
    shardingApproach = null,
    importRunConfigurations = emptyList(),
    gazelleTarget = null,
    indexAllFilesInDirectories = false,
    pythonCodeGeneratorRuleNames = emptyList(),
    importIjars = false,
    deriveInstrumentationFilterFromTargets = true,
    indexAdditionalFilesInDirectories = emptyList(),
  )

private fun createMockProject(lightweightModules: List<Build.Target>): PhasedBazelMappedProject =
  PhasedBazelMappedProject(
    targets =
      lightweightModules
        .map { RawPhasedTarget(it) }
        .associateBy { Label.parse(it.target.rule.name) },
    hasError = false,
  )

// Helper: creates a mock source file at the given relative path with the given package.
private fun Path.createMockSourceFile(relativePath: String, fullPackage: String): Path {
  val file = resolve(relativePath).createParentDirectories().createFile()
  file.writeText(
    """
      |package $fullPackage;
      |
      |class A { }
    """.trimMargin(),
  )
  return file
}

class FirstPhaseTargetToBspMapperTest {
  private lateinit var workspaceRoot: Path
  private lateinit var bazelInfo: BazelInfo
  private lateinit var bazelPathsResolver: BazelPathsResolver

  @BeforeEach
  fun beforeEach() {
    workspaceRoot = createTempDirectory("workspaceRoot").also { it.toFile().deleteOnExit() }
    bazelInfo =
      BazelInfo(
        execRoot = Paths.get(""),
        outputBase = Paths.get(""),
        workspaceRoot = workspaceRoot,
        bazelBin = Path("bazel-bin"),
        release = BazelRelease.fromReleaseString("release 6.0.0").orLatestSupported(),
        false,
        true,
        emptyList(),
      )
    bazelPathsResolver = BazelPathsResolver(bazelInfo)
  }

  @Nested
  @DisplayName(".toWorkspaceBuildTargetsResult(project)")
  inner class ToWorkspaceBuildTargetsResult {
    @Test
    fun `should map targets to bsp build targets and filter out manual, no ide and unsupported targets`() {
      // given: create a set of targets with various kinds and dependencies.
      val targets =
        listOf(
          createMockTarget(
            name = "//target1",
            kind = "java_library",
            deps = listOf("//dep/target1", "//dep/target2"),
            srcs = listOf("//target1:src1.java", "//target1:a/src2.java"),
            resources = listOf("//target1:resource1.txt", "//target1:a/resource2.txt"),
          ),
          createMockTarget(
            name = "//target2",
            kind = "java_binary",
            deps = listOf("//dep/target1", "//dep/target2"),
            // note: target2 has Kotlin sources so we expect merged languages
            srcs = listOf("//target2:src1.kt", "//target2:src2.kt"),
          ),
          createMockTarget(
            name = "//target3",
            kind = "java_test",
            deps = listOf("//dep/target1", "//dep/target2"),
            resources = listOf("//target3:resource1.txt", "//target3:resource2.txt"),
          ),
          createMockTarget(
            name = "//target4",
            kind = "kt_jvm_library",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target5",
            kind = "kt_jvm_binary",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target6",
            kind = "kt_jvm_test",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target7",
            kind = "custom_rule_with_supported_rules_library",
            // target7 should have its own sources – we now create files for it
            srcs = listOf("//target7:src1.java", "//target7:a/src2.java"),
          ),
          // // filegroup targets: note we set kind exactly to "filegroup" so they are filtered out from top-level.
          createMockTarget(
            name = "//filegroupSources",
            kind = "filegroup",
            srcs = listOf("//filegroupSources:src1.java", "//filegroupSources:src2.java"),
          ),
          createMockTarget(
            name = "//filegroupResources",
            kind = "filegroup",
            resources = listOf("//filegroupResources:file1.txt", "//filegroupResources:file2.txt"),
          ),
          createMockTarget(
            name = "//target8",
            kind = "java_library",
            // target8 references a filegroup target (which will be merged)
            srcs = listOf("//target8:src1.kt", "//filegroupSources"),
            resources = listOf("//target8:resource1.txt", "//filegroupResources"),
          ),
          // targets to be filtered out
          createMockTarget(
            name = "//manual_target",
            kind = "java_library",
            tags = listOf("manual"),
          ),
          createMockTarget(
            name = "//no_ide_target",
            kind = "java_library",
            tags = listOf("no-ide"),
          ),
          createMockTarget(
            name = "//unsupported_target",
            kind = "unsupported_target",
          ),
        )
      val project = createMockProject(targets)

      // Create source files for all targets that should have sources:
      val target1Src1 = workspaceRoot.createMockSourceFile("target1/src1.java", "com.example")
      val target1Src2 = workspaceRoot.createMockSourceFile("target1/a/src2.java", "com.example.a")

      val target2Src1 = workspaceRoot.createMockSourceFile("target2/src1.kt", "com.example")
      val target2Src2 = workspaceRoot.createMockSourceFile("target2/src2.kt", "com.example")

      val target7Src1 = workspaceRoot.createMockSourceFile("target7/src1.java", "com.example")
      val target7Src2 = workspaceRoot.createMockSourceFile("target7/a/src2.java", "com.example.a")

      val target8Src1 = workspaceRoot.createMockSourceFile("target8/src1.kt", "com.example")
      // Create files for filegroupSources – they will be used via dependency resolution.
      val fgSrc1 = workspaceRoot.createMockSourceFile("filegroupSources/src1.java", "com.fg")
      val fgSrc2 = workspaceRoot.createMockSourceFile("filegroupSources/src2.java", "com.fg")

      // Create resource files for targets that use resources:
      val target1Resource1 = workspaceRoot.resolve("target1/resource1.txt").createParentDirectories().createFile()
      val target1Resource2 = workspaceRoot.resolve("target1/a/resource2.txt").createParentDirectories().createFile()
      val target3Resource1 = workspaceRoot.resolve("target3/resource1.txt").createParentDirectories().createFile()
      val target3Resource2 = workspaceRoot.resolve("target3/resource2.txt").createParentDirectories().createFile()
      val target8Resource1 = workspaceRoot.resolve("target8/resource1.txt").createParentDirectories().createFile()
      val fgRes1 = workspaceRoot.resolve("filegroupResources/file1.txt").createParentDirectories().createFile()
      val fgRes2 = workspaceRoot.resolve("filegroupResources/file2.txt").createParentDirectories().createFile()

      // when
      val workspaceContext = createMockWorkspaceContext(false)
      val mapper = PhasedBazelProjectMapper(BazelPathsResolverMock.create(workspaceRoot), workspaceContext)
      val context = PhasedBazelProjectMapperContext(RepoMappingDisabled)
      val result: BazelResolvedWorkspace = mapper.resolveWorkspace(context, project)

      // then: update expected build targets as per the new merged behavior
      result.targets.getTargets().toList() shouldContainExactlyInAnyOrder
        listOf(
          // target1: unchanged
          RawBuildTarget(
            id = Label.parse("//target1"),
            tags = listOf(),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            kind =
              TargetKind(
                kindString = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
            sources =
              listOf(
                SourceItem(target1Src1, false, "com.example"),
                SourceItem(target1Src2, false, "com.example.a"),
              ),
            resources =
              listOf(
                target1Resource1,
                target1Resource2,
              ),
            baseDirectory = workspaceRoot.resolve(Path("target1")),
          ),
          // target2: now merges its declared language with those inferred from its .kt sources
          RawBuildTarget(
            id = Label.parse("//target2"),
            tags = listOf(),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            kind =
              TargetKind(
                kindString = "java_binary",
                ruleType = RuleType.BINARY,
                languageClasses = setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
              ),
            sources =
              listOf(
                SourceItem(target2Src1, false, "com.example"),
                SourceItem(target2Src2, false, "com.example"),
              ),
            resources = emptyList(),
            baseDirectory = workspaceRoot.resolve(Path("target2")),
          ),
          // // target3
          RawBuildTarget(
            id = Label.parse("//target3"),
            tags = listOf(),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            kind =
              TargetKind(
                kindString = "java_test",
                ruleType = RuleType.TEST,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
            sources = emptyList(),
            resources =
              listOf(
                target3Resource1,
                target3Resource2,
              ),
            baseDirectory = workspaceRoot.resolve(Path("target3")),
          ),
          // // target4
          RawBuildTarget(
            id = Label.parse("//target4"),
            tags = listOf(),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            kind =
              TargetKind(
                kindString = "kt_jvm_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.KOTLIN),
              ),
            sources = emptyList(),
            resources = emptyList(),
            baseDirectory = workspaceRoot.resolve(Path("target4")),
          ),
          // // target5
          RawBuildTarget(
            id = Label.parse("//target5"),
            tags = listOf(),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            kind =
              TargetKind(
                kindString = "kt_jvm_binary",
                ruleType = RuleType.BINARY,
                languageClasses = setOf(LanguageClass.KOTLIN),
              ),
            sources = emptyList(),
            resources = emptyList(),
            baseDirectory = workspaceRoot.resolve(Path("target5")),
          ),
          // // target6
          RawBuildTarget(
            id = Label.parse("//target6"),
            tags = listOf(),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            kind =
              TargetKind(
                kindString = "kt_jvm_test",
                ruleType = RuleType.TEST,
                languageClasses = setOf(LanguageClass.KOTLIN),
              ),
            sources = emptyList(),
            resources = emptyList(),
            baseDirectory = workspaceRoot.resolve(Path("target6")),
          ),
          // // target7: now with its created source files
          RawBuildTarget(
            id = Label.parse("//target7"),
            tags = listOf(),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kindString = "custom_rule_with_supported_rules_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
            sources =
              listOf(
                SourceItem(target7Src1, false, "com.example"),
                SourceItem(target7Src2, false, "com.example.a"),
              ),
            resources = emptyList(),
            baseDirectory = workspaceRoot.resolve(Path("target7")),
          ),
          // // target8: merging its own source and the sources from filegroupSources dependency
          RawBuildTarget(
            id = Label.parse("//target8"),
            tags = listOf(),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kindString = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
              ),
            sources =
              listOf(
                // note: the direct mapping for "//target8:src1.kt" becomes workspaceRoot/target8/src1.kt
                SourceItem(target8Src1, false, "com.example"),
                // then the dependency from filegroupSources (its own direct source items)
                SourceItem(fgSrc1, false, "com.fg"),
                SourceItem(fgSrc2, false, "com.fg"),
              ),
            resources =
              listOf(
                target8Resource1,
                // resources merged from filegroupResources dependency
                workspaceRoot.resolve("filegroupResources/file1.txt"),
                workspaceRoot.resolve("filegroupResources/file2.txt"),
              ),
            baseDirectory = workspaceRoot.resolve(Path("target8")),
          ),
          RawBuildTarget(
            id = Label.parse("//filegroupSources"),
            tags = listOf(),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kindString = "filegroup",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
            sources =
              listOf(
                SourceItem(fgSrc1, false, "com.fg"),
                SourceItem(fgSrc2, false, "com.fg"),
              ),
            resources = emptyList(),
            baseDirectory = workspaceRoot.resolve(Path("filegroupSources")),
          ),
        )
    }

    @Test
    fun `should keep manual targets if manual targets sync is allowed`() {
      // given
      val targets =
        listOf(
          createMockTarget(
            name = "//target1",
            kind = "java_library",
          ),
          createMockTarget(
            name = "//manual_target",
            kind = "java_library",
            tags = listOf("manual"),
          ),
        )
      val project = createMockProject(targets)

      // when
      val mapper = PhasedBazelProjectMapper(BazelPathsResolverMock.create(), createMockWorkspaceContext(true))
      val context = PhasedBazelProjectMapperContext(RepoMappingDisabled)
      val result: BazelResolvedWorkspace = mapper.resolveWorkspace(context, project)

      // then
      val strings =
        result.targets
          .getTargets()
          .map { it.id.toString() }
          .toList()
      strings shouldContainExactlyInAnyOrder
        listOf(
          "@//target1",
          "@//manual_target",
        )
    }
  }
}
