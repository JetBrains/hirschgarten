package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class ResourceRootBuilderTest {

  private val projectName = "test-project"

  @TempDir
  private lateinit var tempDir: Path
  private lateinit var projectRoot: Path

  @BeforeEach
  fun setUp() {
    projectRoot = tempDir.resolve("project").createDirectories()
  }

  @Test
  fun `should mark resources as JAVA_RESOURCE_ROOT_TYPE by default`() {
    val resource = projectRoot.resolve("file.txt").createFile()
    val target = javaTarget(resources = listOf(resource))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.rootType } shouldContainExactlyInAnyOrder listOf(JAVA_RESOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark resources of a TEST rule as JAVA_TEST_RESOURCE_ROOT_TYPE`() {
    val resource = projectRoot.resolve("file.txt").createFile()
    val target = javaTarget(
      ruleType = RuleType.TEST,
      resources = listOf(resource),
    )

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.rootType } shouldContainExactlyInAnyOrder listOf(JAVA_TEST_RESOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark resources of a target in testTargets as JAVA_TEST_RESOURCE_ROOT_TYPE`() {
    val resource = projectRoot.resolve("file.txt").createFile()
    val targetId = Label.parse("//target")
    val target = javaTarget(resources = listOf(resource), label = targetId.toString())

    val roots = ResourceRootBuilder.resolve(target, projectName, setOf(targetId))

    roots.map { it.rootType } shouldContainExactlyInAnyOrder listOf(JAVA_TEST_RESOURCE_ROOT_TYPE)
  }

  @Test
  fun `should produce one root per resource when no strip prefix applies (java target)`() {
    val a = projectRoot.resolve("random/dir/a.txt").also { it.parent.createDirectories() }.createFile()
    val b = projectRoot.resolve("random/dir/sub/b.txt").also { it.parent.createDirectories() }.createFile()
    val target = javaTarget(resources = listOf(a, b))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(a, b)
  }

  @Test
  fun `should strip the Java src-main-resources convention down to that prefix`() {
    val prefix = projectRoot.resolve("pkg/src/main/resources").createDirectories()
    val res1 = prefix.resolve("com/example/a.txt").also { it.parent.createDirectories() }.createFile()
    val res2 = prefix.resolve("com/example/b.txt").createFile()
    val target = javaTarget(resources = listOf(res1, res2))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(prefix)
  }

  @Test
  fun `should strip the Kotlin conventional segments (src-main-resources, kotlin)`() {
    val kotlinPrefix = projectRoot.resolve("pkg/kotlin").createDirectories()
    val res1 = kotlinPrefix.resolve("com/example/a.txt").also { it.parent.createDirectories() }.createFile()
    val mavenPrefix = projectRoot.resolve("pkg/src/main/resources").createDirectories()
    val res2 = mavenPrefix.resolve("d/e.txt").also { it.parent.createDirectories() }.createFile()
    val target = kotlinTarget(resources = listOf(res1, res2))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(kotlinPrefix, mavenPrefix)
  }

  @Test
  fun `should strip Scala conventional segments (resources, java)`() {
    val resPrefix = projectRoot.resolve("scala/pkg/resources").createDirectories()
    val res1 = resPrefix.resolve("a.txt").createFile()
    val javaPrefix = projectRoot.resolve("scala/pkg/java").createDirectories()
    val res2 = javaPrefix.resolve("d/e.txt").also { it.parent.createDirectories() }.createFile()
    val target = scalaTarget(resources = listOf(res1, res2))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(resPrefix, javaPrefix)
  }

  @Test
  fun `should honor the JvmBuildTarget resolvedResourceStripPrefix when present`() {
    val prefix = projectRoot.resolve("explicit/prefix").createDirectories()
    val res1 = prefix.resolve("com/example/a.txt").also { it.parent.createDirectories() }.createFile()
    val res2 = prefix.resolve("com/example/b.txt").createFile()
    val target = createRawBuildTarget(
      id = Label.parse("//target"),
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      resources = listOf(res1, res2),
      data = listOf(
        JvmBuildTarget(
          javaVersion = "",
          resolvedResourceStripPrefix = prefix,
        ),
      ),
    )

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(prefix)
  }

  private fun javaTarget(
    label: String = "//target",
    ruleType: RuleType = RuleType.LIBRARY,
    resources: List<Path> = emptyList(),
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "java_library",
      ruleType = ruleType,
      languageClasses = setOf(LanguageClass.JAVA),
    ),
    resources = resources,
    data = listOf(JvmBuildTarget(javaVersion = "")),
  )

  private fun kotlinTarget(
    label: String = "//target",
    resources: List<Path> = emptyList(),
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "kt_jvm_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(LanguageClass.KOTLIN),
    ),
    resources = resources,
    data = listOf(
      KotlinBuildTarget(
        languageVersion = null,
        apiVersion = null,
        kotlincOptions = emptyList(),
        associates = emptyList(),
        moduleName = null,
      ),
    ),
  )

  private fun scalaTarget(
    label: String = "//target",
    resources: List<Path> = emptyList(),
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "scala_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(LanguageClass.SCALA),
    ),
    resources = resources,
    data = listOf(
      ScalaBuildTarget(
        scalaVersion = "2.13.0",
        scalacOptions = emptyList(),
        sdkJars = emptyList(),
      ),
    ),
  )
}
