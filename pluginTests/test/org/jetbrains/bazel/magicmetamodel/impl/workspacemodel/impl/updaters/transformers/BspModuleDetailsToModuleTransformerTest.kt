package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.toDefaultTargetsMap
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("BspModuleDetailsToModuleTransformer.transform(bspModuleDetails) tests")
class BspModuleDetailsToModuleTransformerTest : WorkspaceModelBaseTest() {
  @Test
  fun `should return no modules for no bsp module details items`() {
    // given
    val emptyBspModuleDetails = listOf<BspModuleDetails>()

    // when
    val modules =
      BspModuleDetailsToModuleTransformer(
        mapOf(),
        project,
      ).transform(
        emptyBspModuleDetails,
      )

    // then
    modules shouldBe emptyList()
  }

  @Test
  fun `should return java module with dependencies to other targets and libraries`() {
    // given
    val targetName = "@//target1"
    val targetId = Label.parse(targetName)

    val target =
      RawBuildTarget(
        targetId,
        emptyList(),
        listOf(
          DependencyLabel.parse("@maven//:test"),
          DependencyLabel.parse("@//target2"),
          DependencyLabel.parse("@//target3"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )

    val bspModuleDetails =
      BspModuleDetails(
        target = target,
        javacOptions = listOf(),
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            Label.parse("@//target2"),
            Label.parse("@//target3"),
          ),
      )

    val targetsMap = listOf("@//target1", "@//target2", "@//target3").toDefaultTargetsMap()

    // when
    val module =
      BspModuleDetailsToModuleTransformer(
        targetsMap,
        project,
      ).transform(
        bspModuleDetails,
      )

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "target1.target1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "target2.target2",
            "target3.target3",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    shouldBe(module, expectedModule)
  }

  @Test
  fun `should return module with associates when specified in BspModuleDetails`() {
    // given
    val targetName = "@//target1"
    val targetId = Label.parse(targetName)
    val target =
      RawBuildTarget(
        targetId,
        emptyList(),
        listOf(
          DependencyLabel.parse("@maven//:test"),
          DependencyLabel.parse("@//target2"),
          DependencyLabel.parse("@//target3"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val bspModuleDetails =
      BspModuleDetails(
        target = target,
        type = ModuleTypeId("JAVA_MODULE"),
        javacOptions = listOf(),
        associates =
          listOf(
            Label.parse("@//target4"),
            Label.parse("@//target5"),
          ),
        dependencies =
          listOf(
            Label.parse("@maven//:test"),
            Label.parse("@//target2"),
            Label.parse("@//target3"),
          ),
      )

    val targetsMap = listOf("@//target1", "@//target2", "@//target3", "@//target4", "@//target5").toDefaultTargetsMap()

    // when
    val module =
      BspModuleDetailsToModuleTransformer(
        targetsMap,
        project,
      ).transform(
        bspModuleDetails,
      )

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "target1.target1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "maven.test",
            "target2.target2",
            "target3.target3",
          ),
        associates =
          listOf(
            "target4.target4",
            "target5.target5",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    shouldBe(module, expectedModule)
  }

  @Test
  fun `should return multiple java modules with dependencies to other targets and libraries`() {
    // given
    val target1Name = "@//target1"
    val target1Id = Label.parse(target1Name)

    val target1 =
      RawBuildTarget(
        target1Id,
        emptyList(),
        listOf(
          DependencyLabel.parse("@maven//:test"),
          DependencyLabel.parse("//target2"),
          DependencyLabel.parse("//target3"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )

    val bspModuleDetails1 =
      BspModuleDetails(
        target = target1,
        javacOptions = listOf(),
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            Label.parse("//target2"),
            Label.parse("//target3"),
          ),
      )

    val target2Name = "@//target2"
    val target2Id = Label.parse(target2Name)

    val target2 =
      RawBuildTarget(
        target2Id,
        emptyList(),
        listOf(
          DependencyLabel.parse("@maven//:test"),
          DependencyLabel.parse("//target3"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )

    val bspModuleDetails2 =
      BspModuleDetails(
        target = target2,
        javacOptions = listOf(),
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            Label.parse("//target3"),
          ),
      )

    val targetsMap = listOf("@//target1", "@//target2", "@//target3").toDefaultTargetsMap()

    val bspModuleDetails = listOf(bspModuleDetails1, bspModuleDetails2)
    // when
    val modules =
      BspModuleDetailsToModuleTransformer(
        targetsMap,
        project,
      ).transform(
        bspModuleDetails,
      )

    // then
    val expectedModule1 =
      GenericModuleInfo(
        name = "target1.target1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "target2.target2",
            "target3.target3",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val expectedModule2 =
      GenericModuleInfo(
        name = "target2.target2",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "target3.target3",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    modules shouldContainExactlyInAnyOrder (
      listOf(
        expectedModule1,
        expectedModule2,
      ) to { actual, expected -> shouldBe(actual, expected) }
    )
  }

  private infix fun <T, C : Collection<T>, E> C.shouldContainExactlyInAnyOrder(
    expectedWithAssertion: Pair<Collection<E>, (T, E) -> Unit>,
  ) {
    val expectedValues = expectedWithAssertion.first
    val assertion = expectedWithAssertion.second

    this shouldHaveSize expectedValues.size

    this.forAll { actual -> expectedValues.forAny { assertion(actual, it) } }
  }

  private fun shouldBe(actual: GenericModuleInfo, expected: GenericModuleInfo) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.dependencies shouldBe expected.dependencies
  }
}
