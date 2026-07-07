package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.JvmDependency
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.junit.jupiter.api.Test

class DependencyBuilderTest {
  @Test
  fun `should resolve no deps for a target with no jvm dependencies`() {
    val target = jvmTarget(
      label = "//target",
      jvmDependencies = emptyList(),
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.dependencies shouldBe emptyList()
    resolved.strictDependenciesCheck shouldBe StrictDependencyCheckedType.OFF
    resolved.strictDependencies shouldBe emptyList()
  }

  @Test
  fun `should resolve a module dependency and promote COMPILE to EXPORTED_COMPILE_TIME for non-jvm_library kinds`() {
    val depLabel = Label.parse("//other")
    val target = jvmTarget(
      label = "//target",
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(depLabel.asKey()))),
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.dependencies shouldContainExactlyInAnyOrder listOf(
      DependencyLabel(depLabel.asKey(), kind = DependencyLabelKind.EXPORTED_COMPILE_TIME),
    )
  }

  @Test
  fun `a library dependency shadowing a source module becomes a exported module dependency`() {
    val libLabel = Label.parse("//lib")
    val producer = Label.parse("//producer")
    val target = jvmTarget(
      label = "//target",
      jvmDependencies = listOf(JvmDependency.LibraryDependency(DependencyLabel(libLabel.asKey()))),
    )

    val resolved = DependencyBuilder(
      listOf(target),
      libraryShadowsModule = mapOf(libLabel.asKey() to producer.asKey()),
    ).resolve(target)

    resolved.dependencies shouldContainExactlyInAnyOrder listOf(
      DependencyLabel(
        producer.asKey(),
        DependencyLabelKind.EXPORTED_COMPILE_TIME,
      ),
    )
  }

  @Test
  fun `should resolve a module dependency without exporting it for jvm_library kind (monorepo special case)`() {
    val depLabel = Label.parse("//other")
    val target = jvmTarget(
      label = "//target",
      kind = "jvm_library",
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(depLabel.asKey()))),
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.dependencies shouldContainExactlyInAnyOrder listOf(
      DependencyLabel(depLabel.asKey(), kind = DependencyLabelKind.COMPILE),
    )
  }

  @Test
  fun `should resolve a module dependency without exporting it for _jvm_library_jps kind (monorepo special case)`() {
    val depLabel = Label.parse("//other")
    val target = jvmTarget(
      label = "//target",
      kind = "_jvm_library_jps",
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(depLabel.asKey()))),
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.dependencies shouldContainExactlyInAnyOrder listOf(
      DependencyLabel(depLabel.asKey(), kind = DependencyLabelKind.COMPILE),
    )
  }

  @Test
  fun `should always export library dependencies regardless of target kind`() {
    val libLabel = Label.parse("//lib")
    val target = jvmTarget(
      label = "//target",
      kind = "jvm_library",
      jvmDependencies = listOf(JvmDependency.LibraryDependency(DependencyLabel(libLabel.asKey()))),
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.dependencies shouldContainExactlyInAnyOrder listOf(
      DependencyLabel(libLabel.asKey(), kind = DependencyLabelKind.EXPORTED_COMPILE_TIME),
    )
  }

  @Test
  fun `should preserve RUNTIME and EXPORTED_COMPILE_TIME kinds on incoming dependencies`() {
    val runtimeLabel = Label.parse("//runtime")
    val exportedLabel = Label.parse("//exported")
    val target = jvmTarget(
      label = "//target",
      jvmDependencies = listOf(
        JvmDependency.ModuleDependency(DependencyLabel(runtimeLabel.asKey(), kind = DependencyLabelKind.RUNTIME)),
        JvmDependency.ModuleDependency(DependencyLabel(exportedLabel.asKey(), kind = DependencyLabelKind.EXPORTED_COMPILE_TIME)),
      ),
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.dependencies shouldContainExactlyInAnyOrder listOf(
      DependencyLabel(runtimeLabel.asKey(), kind = DependencyLabelKind.RUNTIME),
      DependencyLabel(exportedLabel.asKey(), kind = DependencyLabelKind.EXPORTED_COMPILE_TIME),
    )
  }

  @Test
  fun `should fall back to target_dependencies when there is no JvmBuildTarget`() {
    val depLabel = Label.parse("//other")
    val target = createRawBuildTarget(
      id = Label.parse("//target"),
      dependencies = listOf(DependencyLabel(depLabel.asKey())),
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.dependencies shouldContainExactlyInAnyOrder listOf(
      DependencyLabel(depLabel.asKey(), kind = DependencyLabelKind.COMPILE),
    )
  }

  @Test
  fun `strictDependenciesCheck should be OFF when target is not in workspace`() {
    val target = RawBuildTarget(
      key = WorkspaceTargetKey(label = Label.parse("@external//some:target")),
      dependencies = emptyList(),
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      sources = SourceFileCollection.EMPTY,
      generatedSources = SourceFileCollection.EMPTY,
      resources = SourceFileCollection.EMPTY,
      baseDirectory = kotlin.io.path.Path("base/dir"),
      data = listOf(
        JvmBuildTarget(
          javaVersion = "",
          checkStrictDependencies = StrictDependencyCheckedType.WARNING,
        ),
      ),
      isWorkspace = false,
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.strictDependenciesCheck shouldBe StrictDependencyCheckedType.OFF
  }

  @Test
  fun `strictDependenciesCheck should reflect JvmBuildTarget setting when target is in workspace`() {
    val target = jvmTarget(
      label = "//target",
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
    )

    val resolved = DependencyBuilder(listOf(target)).resolve(target)

    resolved.strictDependenciesCheck shouldBe StrictDependencyCheckedType.WARNING
  }

  @Test
  fun `strictDependencies should be the transitive closure of EXPORTED_COMPILE_TIME dependencies`() {
    // a -> b -> c, where all are exported and a has strict deps on
    val a = Label.parse("//a")
    val b = Label.parse("//b")
    val c = Label.parse("//c")
    val targetA = jvmTarget(
      label = a.toString(),
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
      jvmDependencies = listOf(
        JvmDependency.ModuleDependency(
          DependencyLabel(
            b.asKey(),
            kind = DependencyLabelKind.EXPORTED_COMPILE_TIME,
          ),
        ),
      ),
    )
    val targetB = jvmTarget(
      label = b.toString(),
      jvmDependencies = listOf(
        JvmDependency.ModuleDependency(
          DependencyLabel(
            c.asKey(),
            kind = DependencyLabelKind.EXPORTED_COMPILE_TIME,
          ),
        ),
      ),
    )
    val targetC = jvmTarget(label = c.toString())

    val resolved = DependencyBuilder(listOf(targetA, targetB, targetC)).resolve(targetA)

    resolved.strictDependenciesCheck shouldBe StrictDependencyCheckedType.WARNING
    resolved.strictDependencies shouldContainExactlyInAnyOrder listOf(b, c)
  }

  @Test
  fun `library-shadow module dependencies are not propagated through the exported-deps closure`() {
    val x = Label.parse("//x")
    val a = Label.parse("//a")
    val b = Label.parse("//b")
    val libB = Label.parse("//libB")
    val targetX = jvmTarget(
      label = x.toString(),
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
      jvmDependencies = listOf(
        JvmDependency.ModuleDependency(DependencyLabel(a.asKey(), kind = DependencyLabelKind.EXPORTED_COMPILE_TIME)),
      ),
    )
    // //a reaches //b only through a jdeps library that shadows the //b source module
    val targetA = jvmTarget(
      label = a.toString(),
      jvmDependencies = listOf(JvmDependency.LibraryDependency(DependencyLabel(libB.asKey()))),
    )
    val targetB = jvmTarget(label = b.toString())

    val resolved = DependencyBuilder(
      listOf(targetX, targetA, targetB),
      libraryShadowsModule = mapOf(libB.asKey() to b.asKey()),
    ).resolve(targetX)

    resolved.strictDependencies shouldContainExactlyInAnyOrder listOf(a)
  }

  @Test
  fun `strictDependencies should follow each direct dependency through its exported-deps closure`() {
    // a -> b (compile, NOT exported); b -> c (exported).
    // strict deps for a is "what a transitively pulls through b's API": [b] plus everything b exports = [b, c].
    val a = Label.parse("//a")
    val b = Label.parse("//b")
    val c = Label.parse("//c")
    val targetA = jvmTarget(
      label = a.toString(),
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(b.asKey()))),
    )
    val targetB = jvmTarget(
      label = b.toString(),
      jvmDependencies = listOf(
        JvmDependency.ModuleDependency(
          DependencyLabel(
            c.asKey(),
            kind = DependencyLabelKind.EXPORTED_COMPILE_TIME,
          ),
        ),
      ),
    )
    val targetC = jvmTarget(label = c.toString())

    val resolved = DependencyBuilder(listOf(targetA, targetB, targetC)).resolve(targetA)

    resolved.strictDependencies shouldContainExactlyInAnyOrder listOf(b, c)
  }

  @Test
  fun `strictDependencies should not follow non-exported dependencies transitively`() {
    // a -> b (compile, NOT exported); b -> c (compile, NOT exported).
    // strict deps for a is [b] only, c is not in b's exported closure.
    val a = Label.parse("//a")
    val b = Label.parse("//b")
    val c = Label.parse("//c")
    val targetA = jvmTarget(
      label = a.toString(),
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(b.asKey()))),
    )
    val targetB = jvmTarget(
      label = b.toString(),
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(c.asKey()))),
    )
    val targetC = jvmTarget(label = c.toString())

    val resolved = DependencyBuilder(listOf(targetA, targetB, targetC)).resolve(targetA)

    resolved.strictDependencies shouldContainExactlyInAnyOrder listOf(b)
  }

  private fun Label.asKey(): WorkspaceTargetKey = WorkspaceTargetKey(label = this)

  @Test
  fun `strictDependencies are resolved per configuration when one label has several configurations`() {
    val a = Label.parse("//a")
    val b = Label.parse("//b")
    val c = Label.parse("//c")
    val normalConfig = WorkspaceConfigurationId.of("00000f1")
    val execConfig = WorkspaceConfigurationId.of("00000f2")
    val aNormal = jvmTarget(
      label = a.toString(),
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(b.asKey()))),
    ).copy(key = WorkspaceTargetKey(label = a, configuration = normalConfig))
    val aExec = jvmTarget(
      label = a.toString(),
      checkStrictDependencies = StrictDependencyCheckedType.WARNING,
      jvmDependencies = listOf(JvmDependency.ModuleDependency(DependencyLabel(c.asKey()))),
    ).copy(key = WorkspaceTargetKey(label = a, configuration = execConfig))
    val targetB = jvmTarget(label = b.toString())
    val targetC = jvmTarget(label = c.toString())

    val builder = DependencyBuilder(listOf(aNormal, aExec, targetB, targetC))

    builder.resolve(aNormal).strictDependencies shouldContainExactlyInAnyOrder listOf(b)
    builder.resolve(aExec).strictDependencies shouldContainExactlyInAnyOrder listOf(c)
  }

  private fun jvmTarget(
    label: String,
    kind: String = "java_library",
    ruleType: RuleType = RuleType.LIBRARY,
    jvmDependencies: List<JvmDependency> = emptyList(),
    checkStrictDependencies: StrictDependencyCheckedType = StrictDependencyCheckedType.OFF,
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = kind,
      ruleType = ruleType,
      languageClasses = setOf(LanguageClass.JAVA),
    ),
    data = listOf(
      JvmBuildTarget(
        javaVersion = "",
        jvmDependencies = jvmDependencies,
        checkStrictDependencies = checkStrictDependencies,
      ),
    ),
  )
}
