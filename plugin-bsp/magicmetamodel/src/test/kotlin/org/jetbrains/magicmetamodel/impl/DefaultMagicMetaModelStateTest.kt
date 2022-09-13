package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BuildTargetIdentifierState tests")
class BuildTargetIdentifierStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val buildTargetIdentifier = BuildTargetIdentifier("target")

    // when
    val state = buildTargetIdentifier.toState()

    // then
    state.fromState() shouldBe buildTargetIdentifier
  }
}

@DisplayName("BuildTargetCapabilitiesState tests")
class BuildTargetCapabilitiesStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val buildTargetCapabilities = BuildTargetCapabilities(true, false, true, true)

    // when
    val state = buildTargetCapabilities.toState()

    // then
    state.fromState() shouldBe buildTargetCapabilities
  }
}

@DisplayName("BuildTargetState tests")
class BuildTargetStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val buildTarget = BuildTarget(
      BuildTargetIdentifier("target"),
      listOf("tag1", "tag2"),
      listOf("language1"),
      listOf(BuildTargetIdentifier("dep1"), BuildTargetIdentifier("dep2")),
      BuildTargetCapabilities(true, false, true, true)
    )
    buildTarget.displayName = "target name"
    buildTarget.baseDirectory = "/base/dir"
    buildTarget.dataKind = "kind"
    buildTarget.data = "DATA"

    // when
    val state = buildTarget.toState()

    // then
    state.fromState() shouldBe buildTarget
  }
}

@DisplayName("SourceItemState tests")
class SourceItemStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val sourceItem = SourceItem(
      "uriii",
      SourceItemKind.FILE,
      false,
    )

    // when
    val state = sourceItem.toState()

    // then
    state.fromState() shouldBe sourceItem
  }
}

@DisplayName("SourcesItemState tests")
class SourcesItemStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val sourcesItem = SourcesItem(
      BuildTargetIdentifier("target"),
      listOf(
        SourceItem("urriii1", SourceItemKind.FILE, false),
        SourceItem("urriii2", SourceItemKind.DIRECTORY, true),
      )

    )
    sourcesItem.roots = listOf("root1", "root2")

    // when
    val state = sourcesItem.toState()

    // then
    state.fromState() shouldBe sourcesItem
  }
}

@DisplayName("ResourcesItemState tests")
class ResourcesItemStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val resourcesItem = ResourcesItem(
      BuildTargetIdentifier("target"),
      listOf("resoruce1", "resource2", "resource3")
    )

    // when
    val state = resourcesItem.toState()

    // then
    state.fromState() shouldBe resourcesItem
  }
}


@DisplayName("DependencySourcesItemState tests")
class DependencySourcesItemStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val dependencySourcesItem = DependencySourcesItem(
      BuildTargetIdentifier("target"),
      listOf("source1", "source2", "source3")
    )

    // when
    val state = dependencySourcesItem.toState()

    // then
    state.fromState() shouldBe dependencySourcesItem
  }
}


@DisplayName("JavacOptionsItemState tests")
class JavacOptionsItemStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val javacOptionsItem = JavacOptionsItem(
      BuildTargetIdentifier("target"),
      listOf("opt1", "opt2", "opt3"),
      listOf("classpath1", "classpath2"),
      "dir/"
    )

    // when
    val state = javacOptionsItem.toState()

    // then
    state.fromState() shouldBe javacOptionsItem
  }
}


@DisplayName("ModuleDetailsState tests")
class ModuleDetailsStateTest {

  @Test
  fun `should do toState and fromState`() {
    // given
    val moduleDetails = ModuleDetails(
      target = BuildTarget(
        BuildTargetIdentifier("target1"),
        listOf("tag1"),
        listOf("lang1"),
        listOf(BuildTargetIdentifier("dep1"), BuildTargetIdentifier("dep2")),
        BuildTargetCapabilities(true, false, false, true)
      ),
      allTargetsIds = listOf(
        BuildTargetIdentifier("target1"),
        BuildTargetIdentifier("target2"),
        BuildTargetIdentifier("target3")
      ),
      sources = listOf(
        SourcesItem(
          BuildTargetIdentifier("target1"),
          listOf(
            SourceItem("/source/file1", SourceItemKind.FILE, false),
            SourceItem("/source/file2", SourceItemKind.FILE, true)
          )
        )
      ),
      resources = listOf(
        ResourcesItem(
          BuildTargetIdentifier("target1"),
          listOf("resource1", "resource2")
        )
      ),
      dependenciesSources = listOf(
        DependencySourcesItem(
          BuildTargetIdentifier("target1"),
          listOf("/dep/source1", "/dep/source2")
        )
      ),
      javacOptions = JavacOptionsItem(
          BuildTargetIdentifier("target1"),
          listOf("opt1", "opt2", "opt3"),
          listOf("classpath1", "classpath2"),
          "class/dir"
        )
    )

    // when
    val state = moduleDetails.toState()

    // then
    state.fromState() shouldBe moduleDetails
  }
}
