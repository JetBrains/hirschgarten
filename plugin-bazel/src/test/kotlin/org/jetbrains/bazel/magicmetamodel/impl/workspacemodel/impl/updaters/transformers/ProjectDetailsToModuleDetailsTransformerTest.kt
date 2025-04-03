package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("ProjectDetailsToModuleDetailsTransformer.moduleDetailsForTargetId(projectDetails) tests")
class ProjectDetailsToModuleDetailsTransformerTest {
  @Test
  fun `should return empty module details for singular module`() {
    // given
    val targetId = Label.parse("target")
    val target =
      BuildTarget(
        targetId,
        emptyList(),
        emptyList(),
        emptyList(),
        BuildTargetCapabilities(),
        emptyList(),
        emptyList(),
      )
    val projectDetails =
      ProjectDetails(
        targetIds = listOf(targetId),
        targets = setOf(target),
        javacOptions = emptyList(),
        libraries = null,
        scalacOptions = emptyList(),
      )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, LibraryGraph(emptyList()))
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails =
      ModuleDetails(
        target = target,
        javacOptions = null,
        scalacOptions = null,
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
    val targetId = Label.parse("target")
    val target =
      BuildTarget(
        targetId,
        emptyList(),
        emptyList(),
        emptyList(),
        BuildTargetCapabilities(),
        listOf(SourceItem(Path("/root/dir/example/package/File1.java"), false)),
        listOf(Path("/root/dir/resource/File.txt")),
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
        scalacOptions = emptyList(),
      )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, LibraryGraph(emptyList()))
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails =
      ModuleDetails(
        target = target,
        javacOptions = javacOptions,
        scalacOptions = null,
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
    val target1Id = Label.parse("target1")
    val target2Id = Label.parse("target2")
    val target1 =
      BuildTarget(
        target1Id,
        emptyList(),
        emptyList(),
        listOf(target2Id),
        BuildTargetCapabilities(),
        listOf(SourceItem(Path("/root/dir1/example/package/File1.java"), false)),
        listOf(Path("/root/dir1/resource/File.txt")),
      )
    val target1JavacOptionsItem =
      JavacOptionsItem(
        target1Id,
        listOf("opt1", "opt2", "opt3"),
      )

    val target2 =
      BuildTarget(
        target2Id,
        emptyList(),
        emptyList(),
        emptyList(),
        BuildTargetCapabilities(),
        listOf(
          SourceItem(Path("/root/dir2/example/package/File1.java"), false),
          SourceItem(Path("/root/dir2/example/package/File2.java"), false),
        ),
        listOf(Path("/root/dir2/resource/File.txt")),
      )
    val target3Id = Label.parse("target3")
    val target3 =
      BuildTarget(
        target3Id,
        emptyList(),
        emptyList(),
        listOf(target2Id),
        BuildTargetCapabilities(),
        emptyList(),
        emptyList(),
      )
    val target3JavacOptionsItem =
      JavacOptionsItem(
        target3Id,
        listOf("opt1"),
      )

    val target4Id = Label.parse("target4")
    val target4 =
      BuildTarget(
        target4Id,
        emptyList(),
        emptyList(),
        listOf(target1Id),
        BuildTargetCapabilities(),
        listOf(
          SourceItem(Path("/root/dir2/example/package/file.py"), false),
        ),
        emptyList(),
      )
    val projectDetails =
      ProjectDetails(
        targetIds = listOf(target1Id, target3Id, target2Id, target4Id),
        targets = setOf(target2, target1, target3, target4),
        javacOptions = listOf(target3JavacOptionsItem, target1JavacOptionsItem),
        libraries = emptyList(),
        scalacOptions = emptyList(),
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
        scalacOptions = null,
        libraryDependencies = emptyList(),
        moduleDependencies = listOf(target2Id),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails2 =
      ModuleDetails(
        target = target2,
        javacOptions = null,
        scalacOptions = null,
        libraryDependencies = emptyList(),
        moduleDependencies = emptyList(),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails3 =
      ModuleDetails(
        target = target3,
        javacOptions = target3JavacOptionsItem,
        scalacOptions = null,
        libraryDependencies = emptyList(),
        moduleDependencies = listOf(target2Id),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails4 =
      ModuleDetails(
        target = target4,
        javacOptions = null,
        scalacOptions = null,
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
