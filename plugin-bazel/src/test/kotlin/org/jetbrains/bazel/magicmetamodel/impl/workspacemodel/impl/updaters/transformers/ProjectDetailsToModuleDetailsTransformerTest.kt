package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("ProjectDetailsToModuleDetailsTransformer.moduleDetailsForTargetId(projectDetails) tests")
class ProjectDetailsToModuleDetailsTransformerTest {
  @Test
  fun `should return empty module details for singular module`() {
    // given
    val targetId = Label.parseCanonical("target")
    val target =
      RawBuildTarget(
        targetId,
        emptyList(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val projectDetails =
      ProjectDetails(
        targetIds = listOf(targetId),
        targets = setOf(target),
        javacOptions = emptyList(),
        libraries = null,
      )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, LibraryGraph(emptyList()))
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails =
      ModuleDetails(
        target = target,
        javacOptions = null,
        libraryDependencies = null,
        moduleDependencies = emptyList(),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    actualModuleDetails shouldBe expectedModuleDetails
  }

  @Test
  fun `should return one module details for project details with one target`() {
    // given
    val targetId = Label.parseCanonical("target")
    val target =
      RawBuildTarget(
        targetId,
        emptyList(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(SourceItem(Path("/root/dir/example/package/File1.java"), false)),
        listOf(Path("/root/dir/resource/File.txt")),
        baseDirectory = Path("base/dir"),
      )

    val javacOptions =
      JavacOptionsItem(
        targetId,
        listOf("opt1", "opt2", "opt3"),
      )

    val projectDetails =
      ProjectDetails(
        targetIds = listOf(targetId),
        targets = setOf(target),
        javacOptions = listOf(javacOptions),
        libraries = emptyList(),
      )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, LibraryGraph(emptyList()))
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails =
      ModuleDetails(
        target = target,
        javacOptions = javacOptions,
        libraryDependencies = emptyList(),
        moduleDependencies = emptyList(),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    actualModuleDetails shouldBe expectedModuleDetails
  }

  @Test
  fun `should multiple module details for project details with multiple targets`() {
    // given
    val target1Id = Label.parseCanonical("target1")
    val target2Id = Label.parseCanonical("target2")
    val target1 =
      RawBuildTarget(
        target1Id,
        emptyList(),
        listOf(target2Id),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(SourceItem(Path("/root/dir1/example/package/File1.java"), false)),
        listOf(Path("/root/dir1/resource/File.txt")),
        baseDirectory = Path("base/dir"),
      )
    val target1JavacOptionsItem =
      JavacOptionsItem(
        target1Id,
        listOf("opt1", "opt2", "opt3"),
      )

    val target2 =
      RawBuildTarget(
        target2Id,
        emptyList(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(
          SourceItem(Path("/root/dir2/example/package/File1.java"), false),
          SourceItem(Path("/root/dir2/example/package/File2.java"), false),
        ),
        listOf(Path("/root/dir2/resource/File.txt")),
        baseDirectory = Path("base/dir"),
      )
    val target3Id = Label.parseCanonical("target3")
    val target3 =
      RawBuildTarget(
        target3Id,
        emptyList(),
        listOf(target2Id),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val target3JavacOptionsItem =
      JavacOptionsItem(
        target3Id,
        listOf("opt1"),
      )

    val target4Id = Label.parseCanonical("target4")
    val target4 =
      RawBuildTarget(
        target4Id,
        emptyList(),
        listOf(target1Id),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(
          SourceItem(Path("/root/dir2/example/package/file.py"), false),
        ),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val projectDetails =
      ProjectDetails(
        targetIds = listOf(target1Id, target3Id, target2Id, target4Id),
        targets = setOf(target2, target1, target3, target4),
        javacOptions = listOf(target3JavacOptionsItem, target1JavacOptionsItem),
        libraries = emptyList(),
      )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, LibraryGraph(emptyList()))
    val actualModuleDetails1 = transformer.moduleDetailsForTargetId(target1.id)
    val actualModuleDetails2 = transformer.moduleDetailsForTargetId(target2.id)
    val actualModuleDetails3 = transformer.moduleDetailsForTargetId(target3.id)
    val actualModuleDetails4 = transformer.moduleDetailsForTargetId(target4.id)

    // then
    val expectedModuleDetails1 =
      ModuleDetails(
        target = target1,
        javacOptions = target1JavacOptionsItem,
        libraryDependencies = emptyList(),
        moduleDependencies = listOf(target2Id),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails2 =
      ModuleDetails(
        target = target2,
        javacOptions = null,
        libraryDependencies = emptyList(),
        moduleDependencies = emptyList(),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails3 =
      ModuleDetails(
        target = target3,
        javacOptions = target3JavacOptionsItem,
        libraryDependencies = emptyList(),
        moduleDependencies = listOf(target2Id),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails4 =
      ModuleDetails(
        target = target4,
        javacOptions = null,
        libraryDependencies = emptyList(),
        moduleDependencies = listOf(target1Id),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    actualModuleDetails1 shouldBe expectedModuleDetails1
    actualModuleDetails2 shouldBe expectedModuleDetails2
    actualModuleDetails3 shouldBe expectedModuleDetails3
    actualModuleDetails4 shouldBe expectedModuleDetails4
  }
}
