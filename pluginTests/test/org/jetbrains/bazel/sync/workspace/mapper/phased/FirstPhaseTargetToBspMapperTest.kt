package org.jetbrains.bazel.sync.workspace.mapper.phased

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ALLOW_MANUAL_TARGETS_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.mapper.PhasedBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.test.framework.target.asTestBuildTarget
import org.jetbrains.bazel.test.framework.testproject.TestBazelProject
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceLocations
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// Helper: creates a mock source file at the given relative path with the given package.
private fun TestBazelProject.writeMockSource(relativePath: String, fullPackage: String): String {
  writeFile(
    relativePath,
    """
      |package $fullPackage;
      |
      |class A { }
    """.trimMargin(),
  )
  return relativePath
}

private fun TestBazelProject.writeMockResource(relativePath: String): String {
  writeFile(relativePath)
  return relativePath
}

@BazelTestApplication
class FirstPhaseTargetToBspMapperTest {
  private val testBazel by testBazelProjectFixture()

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
            generatorName = "generator_name_example",
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

      // Create source files for all targets that should have sources:
      val target1Src1 = testBazel.writeMockSource("target1/src1.java", "com.example")
      val target1Src2 = testBazel.writeMockSource("target1/a/src2.java", "com.example.a")

      val target2Src1 = testBazel.writeMockSource("target2/src1.kt", "com.example")
      val target2Src2 = testBazel.writeMockSource("target2/src2.kt", "com.example")

      val target7Src1 = testBazel.writeMockSource("target7/src1.java", "com.example")
      val target7Src2 = testBazel.writeMockSource("target7/a/src2.java", "com.example.a")

      val target8Src1 = testBazel.writeMockSource("target8/src1.kt", "com.example")
      // Create files for filegroupSources – they will be used via dependency resolution.
      val fgSrc1 = testBazel.writeMockSource("filegroupSources/src1.java", "com.fg")
      val fgSrc2 = testBazel.writeMockSource("filegroupSources/src2.java", "com.fg")

      // Create resource files for targets that use resources:
      val target1Resource1 = testBazel.writeMockResource("target1/resource1.txt")
      val target1Resource2 = testBazel.writeMockResource("target1/a/resource2.txt")
      val target3Resource1 = testBazel.writeMockResource("target3/resource1.txt")
      val target3Resource2 = testBazel.writeMockResource("target3/resource2.txt")
      val target8Resource1 = testBazel.writeMockResource("target8/resource1.txt")
      val fgRes1 = testBazel.writeMockResource("filegroupResources/file1.txt")
      val fgRes2 = testBazel.writeMockResource("filegroupResources/file2.txt")

      // when
      val mapper = PhasedBazelProjectMapper(BazelPathsResolver(testBazel.bazelInfo), ProjectView.EMPTY)
      val resultTargets = mapper.mapTargets(targets.associateBy { Label.parse(it.rule.name) })

      // then: update expected build targets as per the new merged behavior
      resultTargets.map { it.asTestBuildTarget() } shouldContainExactlyInAnyOrder
        listOf(
          // target1: unchanged
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target1")),
            dependencies = listOf(DependencyLabel.parse("//dep/target1"), DependencyLabel.parse("//dep/target2")),
            kind =
              TargetKind(
                kind = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(JavaLanguageClass.JAVA),
              ),
            sources = workspaceLocations(target1Src1, target1Src2),
            resources = workspaceLocations(target1Resource1, target1Resource2),
            //data = listOf(
            //  JvmPackagePrefixData(mapOf(
            //    target1Src1 to "com.example",
            //    target1Src2 to "com.example.a",
            //  ))
            //),
            generatorName = "generator_name_example",
          ),
          // target2
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target2")),
            dependencies = listOf(DependencyLabel.parse("//dep/target1"), DependencyLabel.parse("//dep/target2")),
            kind =
              TargetKind(
                kind = "java_binary",
                ruleType = RuleType.BINARY,
                languageClasses = setOf(JavaLanguageClass.JAVA),
              ),
            sources = workspaceLocations(target2Src1, target2Src2),
            resources = OutputLocationCollection.EMPTY,
            //data = listOf(
            //  JvmPackagePrefixData(mapOf(
            //    target2Src1 to "com.example",
            //    target2Src2 to "com.example",
            //  ))
            //),
          ),
          // // target3
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target3")),
            dependencies = listOf(DependencyLabel.parse("//dep/target1"), DependencyLabel.parse("//dep/target2")),
            kind =
              TargetKind(
                kind = "java_test",
                ruleType = RuleType.TEST,
                languageClasses = setOf(JavaLanguageClass.JAVA),
              ),
            sources = OutputLocationCollection.EMPTY,
            resources = workspaceLocations(target3Resource1, target3Resource2),
          ),
          // // target4
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target4")),
            dependencies = listOf(DependencyLabel.parse("//dep/target1"), DependencyLabel.parse("//dep/target2")),
            kind =
              TargetKind(
                kind = "kt_jvm_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN),
              ),
            sources = OutputLocationCollection.EMPTY,
            resources = OutputLocationCollection.EMPTY,
          ),
          // // target5
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target5")),
            dependencies = listOf(DependencyLabel.parse("//dep/target1"), DependencyLabel.parse("//dep/target2")),
            kind =
              TargetKind(
                kind = "kt_jvm_binary",
                ruleType = RuleType.BINARY,
                languageClasses = setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN),
              ),
            sources = OutputLocationCollection.EMPTY,
            resources = OutputLocationCollection.EMPTY,
          ),
          // // target6
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target6")),
            dependencies = listOf(DependencyLabel.parse("//dep/target1"), DependencyLabel.parse("//dep/target2")),
            kind =
              TargetKind(
                kind = "kt_jvm_test",
                ruleType = RuleType.TEST,
                languageClasses = setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN),
              ),
            sources = OutputLocationCollection.EMPTY,
            resources = OutputLocationCollection.EMPTY,
          ),
          // // target7: now with its created source files
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target7")),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kind = "custom_rule_with_supported_rules_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(JavaLanguageClass.JAVA),
              ),
            sources = workspaceLocations(target7Src1, target7Src2),
            resources = OutputLocationCollection.EMPTY,
            //data = listOf(
            //  JvmPackagePrefixData(mapOf(
            //    target7Src1 to "com.example",
            //    target7Src2 to "com.example.a",
            //  ))
            //),
          ),
          // // target8: merging its own source and the sources from filegroupSources dependency
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//target8")),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kind = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(JavaLanguageClass.JAVA),
              ),
            // note: the direct mapping for "//target8:src1.kt" becomes workspaceRoot/target8/src1.kt
            // then the dependency from filegroupSources (its own direct source items)
            sources = workspaceLocations(target8Src1, fgSrc1, fgSrc2),
            resources = workspaceLocations(
              target8Resource1,
              // resources merged from filegroupResources dependency
              fgRes1,
              fgRes2,
            ),
            //data = listOf(
            //  JvmPackagePrefixData(mapOf(
            //    target8Src1 to "com.example"
            //  ))
            //),
          ),
          TestBuildTarget(
            key = WorkspaceTargetKey(label = Label.parse("//filegroupSources")),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kind = "filegroup",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(JavaLanguageClass.JAVA),
              ),
            sources = workspaceLocations(fgSrc1, fgSrc2),
            resources = OutputLocationCollection.EMPTY,
            //data = listOf(
            //  JvmPackagePrefixData(mapOf(
            //    fgSrc1 to "com.fg",
            //    fgSrc2 to "com.fg",
            //  ))
            //),
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

      // when
      val mapper =
        PhasedBazelProjectMapper(
          BazelPathsResolver(testBazel.bazelInfo),
          ProjectView(mapOf(ALLOW_MANUAL_TARGETS_SYNC_KEY to true), emptyList()),
        )
      val resultTargets = mapper.mapTargets(targets.associateBy { Label.parse(it.rule.name) })

      // then
      val strings =
        resultTargets
          .map { it.id.toString() }
      strings shouldContainExactlyInAnyOrder
        listOf(
          "@//target1",
          "@//manual_target",
        )
      resultTargets
        .singleOrNull { it.id.toString() == "@//manual_target" }
        .shouldNotBeNull()
        .tags shouldContainExactlyInAnyOrder listOf(BuildTargetTag.MANUAL)

      resultTargets
        .singleOrNull { it.id.toString() == "@//target1" }
        .shouldNotBeNull()
        .tags
        .shouldBeEmpty()
    }
  }
}
