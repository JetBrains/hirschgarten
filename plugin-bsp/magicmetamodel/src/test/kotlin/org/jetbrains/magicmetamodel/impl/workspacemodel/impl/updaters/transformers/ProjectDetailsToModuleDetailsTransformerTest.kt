package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("ProjectDetailsToModuleDetailsTransformer.moduleDetailsForTargetId(projectDetails) tests")
class ProjectDetailsToModuleDetailsTransformerTest {
  private val projectBasePathURI = "file:///root"
  private val projectBasePath = URI.create(projectBasePathURI).toPath()

  @Test
  fun `should return empty module details for singular module`() {
    // given
    val targetId = BuildTargetIdentifier("target")
    val target = BuildTarget(
      targetId,
      emptyList(),
      emptyList(),
      emptyList(),
      BuildTargetCapabilities(),
    )
    val projectDetails = ProjectDetails(
      targetsId = listOf(targetId),
      targets = setOf(target),
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = emptyList(),
      javacOptions = emptyList(),
      pythonOptions = emptyList(),
      outputPathUris = emptyList(),
      libraries = null,
    )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, projectBasePath)
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails = ModuleDetails(
      target = target,
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = emptyList(),
      javacOptions = null,
      pythonOptions = null,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = emptyList(),
    )

    actualModuleDetails shouldBe expectedModuleDetails
  }

  @Test
  fun `should return one module details for project details with one target`() {
    // given
    val targetId = BuildTargetIdentifier("target")
    val target = BuildTarget(
      targetId,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    )
    val targetSources = SourcesItem(
      targetId,
      listOf(SourceItem("file:///root/dir/example/package/", SourceItemKind.DIRECTORY, false)),
    )
    val targetResources = ResourcesItem(
      targetId,
      listOf("file:///root/dir/resource/File.txt"),
    )
    val targetDependencySources = DependencySourcesItem(
      targetId,
      listOf("file:///lib/test/1.0.0/test-sources.jar"),
    )
    val javacOptions = JavacOptionsItem(
      targetId,
      listOf("opt1", "opt2", "opt3"),
      listOf("classpath1", "classpath2", "classpath3"),
      "class/dir"
    )

    val pythonOptions = PythonOptionsItem(
      targetId,
      listOf("opt1", "opt2", "opt3")
    )

    val projectDetails = ProjectDetails(
      targetsId = listOf(targetId),
      targets = setOf(target),
      sources = listOf(targetSources),
      resources = listOf(targetResources),
      dependenciesSources = listOf(targetDependencySources),
      javacOptions = listOf(javacOptions),
      pythonOptions = listOf(pythonOptions),
      outputPathUris = emptyList(),
      libraries = emptyList(),
    )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, projectBasePath)
    val actualModuleDetails = transformer.moduleDetailsForTargetId(target.id)

    // then
    val expectedModuleDetails = ModuleDetails(
      target = target,
      sources = listOf(targetSources),
      resources = listOf(targetResources),
      dependenciesSources = listOf(targetDependencySources),
      javacOptions = javacOptions,
      pythonOptions = pythonOptions,
      outputPathUris = emptyList(),
      libraryDependencies = emptyList(),
      moduleDependencies = emptyList()
    )

    actualModuleDetails shouldBe expectedModuleDetails
  }

  @Test
  fun `should multiple module details for project details with multiple targets`() {
    // given
    val target1Id = BuildTargetIdentifier("target1")
    val target2Id = BuildTargetIdentifier("target2")
    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      emptyList(),
      listOf(target2Id, BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    )
    val target1Sources = SourcesItem(
      target1Id,
      listOf(SourceItem("file:///root/dir1/example/package/", SourceItemKind.DIRECTORY, false)),
    )
    val target1Resources = ResourcesItem(
      target1Id,
      listOf("file:///root/dir1/resource/File.txt"),
    )
    val target1DependencySources = DependencySourcesItem(
      target1Id,
      listOf("file:///lib/test/1.0.0/test-sources.jar"),
    )
    val target1JavacOptionsItem = JavacOptionsItem(
      target1Id,
      listOf("opt1", "opt2", "opt3"),
      listOf("classpath1", "classpath2"),
      "class/dir1"
    )

    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    )
    val target2Sources1 = SourcesItem(
      target2Id,
      listOf(
        SourceItem("file:///root/dir2/example/package/File1.java", SourceItemKind.FILE, false),
      ),
    )
    val target2Sources2 = SourcesItem(
      target2Id,
      listOf(
        SourceItem("file:///root/dir2/example/package/File2.java", SourceItemKind.FILE, false),
      ),
    )
    val target2Resources = ResourcesItem(
      target2Id,
      listOf("file:///root/dir2/resource/File.txt"),
    )
    val target2DependencySources = DependencySourcesItem(
      target2Id,
      listOf("file:///lib/test/2.0.0/test-sources.jar"),
    )

    val target3Id = BuildTargetIdentifier("target3")
    val target3 = BuildTarget(
      target3Id,
      emptyList(),
      emptyList(),
      listOf(target2Id),
      BuildTargetCapabilities(),
    )
    val target3Sources = SourcesItem(
      target3Id,
      emptyList(),
    )
    val target3JavacOptionsItem = JavacOptionsItem(
      target3Id,
      listOf("opt1"),
      listOf("classpath1", "classpath2", "classpath3"),
      "class/dir3"
    )

    val target4Id = BuildTargetIdentifier("target4")
    val target4 = BuildTarget(
      target4Id,
      emptyList(),
      emptyList(),
      listOf(target1Id),
      BuildTargetCapabilities(),
    )
    val target4Sources = SourcesItem(
      target4Id,
      listOf(
        SourceItem("file:///root/dir2/example/package/file.py", SourceItemKind.FILE, false),
      ),
    )
    val target4PythonOptionsItem = PythonOptionsItem(
      target4Id,
      listOf("opt1", "opt2")
    )

    val projectDetails = ProjectDetails(
      targetsId = listOf(target1Id, target3Id, target2Id, target4Id),
      targets = setOf(target2, target1, target3, target4),
      sources = listOf(target3Sources, target2Sources1, target1Sources, target2Sources2, target4Sources),
      resources = listOf(target1Resources, target2Resources),
      dependenciesSources = listOf(target2DependencySources, target1DependencySources),
      javacOptions = listOf(target3JavacOptionsItem, target1JavacOptionsItem),
      pythonOptions = listOf(target4PythonOptionsItem),
      outputPathUris = emptyList(),
      libraries = emptyList(),
    )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, projectBasePath)
    val actualModuleDetails1 = transformer.moduleDetailsForTargetId(target1.id)
    val actualModuleDetails2 = transformer.moduleDetailsForTargetId(target2.id)
    val actualModuleDetails3 = transformer.moduleDetailsForTargetId(target3.id)
    val actualModuleDetails4 = transformer.moduleDetailsForTargetId(target4.id)

    // then
    val expectedModuleDetails1 = ModuleDetails(
      target = target1,
      sources = listOf(target1Sources),
      resources = listOf(target1Resources),
      dependenciesSources = listOf(target1DependencySources),
      javacOptions = target1JavacOptionsItem,
      pythonOptions = null,
      outputPathUris = emptyList(),
      libraryDependencies = emptyList(),
      moduleDependencies = listOf(target2Id.uri)
    )
    val expectedModuleDetails2 = ModuleDetails(
      target = target2,
      sources = listOf(target2Sources1, target2Sources2),
      resources = listOf(target2Resources),
      dependenciesSources = listOf(target2DependencySources),
      javacOptions = null,
      pythonOptions = null,
      outputPathUris = emptyList(),
      libraryDependencies = emptyList(),
      moduleDependencies = emptyList(),
    )
    val expectedModuleDetails3 = ModuleDetails(
      target = target3,
      sources = listOf(target3Sources),
      resources = emptyList(),
      dependenciesSources = emptyList(),
      javacOptions = target3JavacOptionsItem,
      pythonOptions = null,
      outputPathUris = emptyList(),
      libraryDependencies = emptyList(),
      moduleDependencies = listOf(target2Id.uri),
    )
    val expectedModuleDetails4 = ModuleDetails(
      target = target4,
      sources = listOf(target4Sources),
      resources = emptyList(),
      dependenciesSources = emptyList(),
      javacOptions = null,
      pythonOptions = target4PythonOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = emptyList(),
      moduleDependencies = listOf(target1Id.uri),
    )

    actualModuleDetails1 shouldBe expectedModuleDetails1
    actualModuleDetails2 shouldBe expectedModuleDetails2
    actualModuleDetails3 shouldBe expectedModuleDetails3
    actualModuleDetails4 shouldBe expectedModuleDetails4
  }

  @Test
  fun `should assign all output path uris to a single root module`() {
    // given
    val targetId1 = BuildTargetIdentifier("target1")
    val target1 = BuildTarget(
      targetId1,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    )
    val targetSources1 = SourcesItem(
      targetId1,
      listOf(SourceItem("file:///root/dir/example/package1/", SourceItemKind.DIRECTORY, false)),
    )
    val targetResources1 = ResourcesItem(
      targetId1,
      listOf("file:///root/dir/resource/File1.txt"),
    )
    val targetDependencySources1 = DependencySourcesItem(
      targetId1,
      listOf("file:///lib/test/1.0.0/test-sources1.jar"),
    )
    val javacOptions1 = JavacOptionsItem(
      targetId1,
      listOf("opt1", "opt2", "opt3"),
      listOf("classpath1", "classpath2", "classpath3"),
      "class/dir"
    )
    val outputPaths1 = listOf("file:///output/file1.out", "file:///output/file2.out")

    val targetId2 = BuildTargetIdentifier("target2")
    val target2 = BuildTarget(
      targetId2,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    ).also { it.baseDirectory = "file:///root/dir/example" }
    val targetSources2 = SourcesItem(
      targetId2,
      listOf(SourceItem("file:///root/dir/example/package2/", SourceItemKind.DIRECTORY, false)),
    )
    val targetResources2 = ResourcesItem(
      targetId2,
      listOf("file:///root/dir/resource/File2.txt"),
    )
    val targetDependencySources2 = DependencySourcesItem(
      targetId2,
      listOf("file:///lib/test/1.0.0/test-sources2.jar"),
    )
    val javacOptions2 = JavacOptionsItem(
      targetId2,
      listOf("opt1", "opt2", "opt3"),
      listOf("classpath1", "classpath2", "classpath3"),
      "class/dir"
    )
    val outputPaths2 = listOf("file:///output/dir")

    val rootTargetId = BuildTargetIdentifier(".bsp-workspace-root")
    val rootTarget = BuildTarget(
      rootTargetId,
      emptyList(),
      emptyList(),
      emptyList(),
      BuildTargetCapabilities()
    ).also { it.baseDirectory = projectBasePathURI }


    val projectDetails = ProjectDetails(
      targetsId = listOf(targetId1, targetId2, rootTargetId),
      targets = setOf(target1, target2, rootTarget),
      sources = listOf(targetSources1, targetSources2),
      resources = listOf(targetResources1, targetResources2),
      dependenciesSources = listOf(targetDependencySources1, targetDependencySources2),
      javacOptions = listOf(javacOptions1, javacOptions2),
      pythonOptions = emptyList(),
      outputPathUris = listOf(outputPaths1, outputPaths2).flatten(),
      libraries = emptyList(),
    )

    // when
    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, projectBasePath)
    val actualModuleDetails1 = transformer.moduleDetailsForTargetId(targetId1)
    val actualModuleDetails2 = transformer.moduleDetailsForTargetId(targetId2)
    val actualRootDetails = transformer.moduleDetailsForTargetId(rootTargetId)

    // then
    val expectedModuleDetails1 = ModuleDetails(
      target = target1,
      sources = listOf(targetSources1),
      resources = listOf(targetResources1),
      dependenciesSources = listOf(targetDependencySources1),
      javacOptions = javacOptions1,
      pythonOptions = null,
      outputPathUris = emptyList(),
      libraryDependencies = emptyList(),
      moduleDependencies = emptyList(),
    )
    val expectedModuleDetails2 = ModuleDetails(
      target = target2,
      sources = listOf(targetSources2),
      resources = listOf(targetResources2),
      dependenciesSources = listOf(targetDependencySources2),
      javacOptions = javacOptions2,
      pythonOptions = null,
      outputPathUris = emptyList(),
      libraryDependencies = emptyList(),
      moduleDependencies = emptyList(),
    )
    val expectedRootModuleDetails = ModuleDetails(
      target = rootTarget,
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = emptyList(),
      javacOptions = null,
      pythonOptions = null,
      outputPathUris = listOf(outputPaths1, outputPaths2).flatten(),
      libraryDependencies = emptyList(),
      moduleDependencies = emptyList(),
    )

    actualModuleDetails1 shouldBe expectedModuleDetails1
    actualModuleDetails2 shouldBe expectedModuleDetails2
    actualRootDetails shouldBe expectedRootModuleDetails
  }
}
