package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
      )
    val projectDetails =
      ProjectDetails(
        targetIds = listOf(targetId),
        targets = setOf(target),
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
        javacOptions = emptyList(),
        libraries = null,
        scalacOptions = emptyList(),
        nonModuleTargets = emptyList(),
      )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, LibraryGraph(emptyList()))
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails =
      ModuleDetails(
        target = target,
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
        javacOptions = null,
        scalacOptions = null,
        outputPathUris = emptyList(),
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
      )
    val targetSources =
      SourcesItem(
        targetId,
        listOf(SourceItem("file:///root/dir/example/package/", SourceItemKind.DIRECTORY, false)),
      )
    val targetResources =
      ResourcesItem(
        targetId,
        listOf("file:///root/dir/resource/File.txt"),
      )
    val targetDependencySources =
      DependencySourcesItem(
        targetId,
        listOf("file:///lib/test/1.0.0/test-sources.jar"),
      )
    val javacOptions =
      JavacOptionsItem(
        targetId,
        listOf("opt1", "opt2", "opt3"),
        listOf("classpath1", "classpath2", "classpath3"),
        "class/dir",
      )

    val projectDetails =
      ProjectDetails(
        targetIds = listOf(targetId),
        targets = setOf(target),
        sources = listOf(targetSources),
        resources = listOf(targetResources),
        dependenciesSources = listOf(targetDependencySources),
        javacOptions = listOf(javacOptions),
        libraries = emptyList(),
        scalacOptions = emptyList(),
        nonModuleTargets = emptyList(),
      )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, LibraryGraph(emptyList()))
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails =
      ModuleDetails(
        target = target,
        sources = listOf(targetSources),
        resources = listOf(targetResources),
        dependenciesSources = listOf(targetDependencySources),
        javacOptions = javacOptions,
        scalacOptions = null,
        outputPathUris = emptyList(),
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
      )
    val target1Sources =
      SourcesItem(
        target1Id,
        listOf(SourceItem("file:///root/dir1/example/package/", SourceItemKind.DIRECTORY, false)),
      )
    val target1Resources =
      ResourcesItem(
        target1Id,
        listOf("file:///root/dir1/resource/File.txt"),
      )
    val target1DependencySources =
      DependencySourcesItem(
        target1Id,
        listOf("file:///lib/test/1.0.0/test-sources.jar"),
      )
    val target1JavacOptionsItem =
      JavacOptionsItem(
        target1Id,
        listOf("opt1", "opt2", "opt3"),
        listOf("classpath1", "classpath2"),
        "class/dir1",
      )

    val target2 =
      BuildTarget(
        target2Id,
        emptyList(),
        emptyList(),
        emptyList(),
        BuildTargetCapabilities(),
      )
    val target2Sources1 =
      SourcesItem(
        target2Id,
        listOf(
          SourceItem("file:///root/dir2/example/package/File1.java", SourceItemKind.FILE, false),
        ),
      )
    val target2Sources2 =
      SourcesItem(
        target2Id,
        listOf(
          SourceItem("file:///root/dir2/example/package/File2.java", SourceItemKind.FILE, false),
        ),
      )
    val target2Resources =
      ResourcesItem(
        target2Id,
        listOf("file:///root/dir2/resource/File.txt"),
      )
    val target2DependencySources =
      DependencySourcesItem(
        target2Id,
        listOf("file:///lib/test/2.0.0/test-sources.jar"),
      )

    val target3Id = Label.parse("target3")
    val target3 =
      BuildTarget(
        target3Id,
        emptyList(),
        emptyList(),
        listOf(target2Id),
        BuildTargetCapabilities(),
      )
    val target3Sources =
      SourcesItem(
        target3Id,
        emptyList(),
      )
    val target3JavacOptionsItem =
      JavacOptionsItem(
        target3Id,
        listOf("opt1"),
        listOf("classpath1", "classpath2", "classpath3"),
        "class/dir3",
      )

    val target4Id = Label.parse("target4")
    val target4 =
      BuildTarget(
        target4Id,
        emptyList(),
        emptyList(),
        listOf(target1Id),
        BuildTargetCapabilities(),
      )
    val target4Sources =
      SourcesItem(
        target4Id,
        listOf(
          SourceItem("file:///root/dir2/example/package/file.py", SourceItemKind.FILE, false),
        ),
      )

    val projectDetails =
      ProjectDetails(
        targetIds = listOf(target1Id, target3Id, target2Id, target4Id),
        targets = setOf(target2, target1, target3, target4),
        sources = listOf(target3Sources, target2Sources1, target1Sources, target2Sources2, target4Sources),
        resources = listOf(target1Resources, target2Resources),
        dependenciesSources = listOf(target2DependencySources, target1DependencySources),
        javacOptions = listOf(target3JavacOptionsItem, target1JavacOptionsItem),
        libraries = emptyList(),
        scalacOptions = emptyList(),
        nonModuleTargets = emptyList(),
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
        sources = listOf(target1Sources),
        resources = listOf(target1Resources),
        dependenciesSources = listOf(target1DependencySources),
        javacOptions = target1JavacOptionsItem,
        scalacOptions = null,
        outputPathUris = emptyList(),
        libraryDependencies = emptyList(),
        moduleDependencies = listOf(target2Id),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails2 =
      ModuleDetails(
        target = target2,
        sources = listOf(target2Sources1, target2Sources2),
        resources = listOf(target2Resources),
        dependenciesSources = listOf(target2DependencySources),
        javacOptions = null,
        scalacOptions = null,
        outputPathUris = emptyList(),
        libraryDependencies = emptyList(),
        moduleDependencies = emptyList(),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails3 =
      ModuleDetails(
        target = target3,
        sources = listOf(target3Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
        javacOptions = target3JavacOptionsItem,
        scalacOptions = null,
        outputPathUris = emptyList(),
        libraryDependencies = emptyList(),
        moduleDependencies = listOf(target2Id),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )
    val expectedModuleDetails4 =
      ModuleDetails(
        target = target4,
        sources = listOf(target4Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
        javacOptions = null,
        scalacOptions = null,
        outputPathUris = emptyList(),
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
