package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createSymbolicLinkPointingTo

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

  @Test
  fun `should detect multiple default Java prefixes simultaneously`() {
    val srcMainResources = projectRoot.resolve("src/main/resources").createDirectories()
    val srcMainResourcesFile = srcMainResources.resolve("app.properties").createFile()
    val javaPrefix = projectRoot.resolve("java")
    val javaFile = javaPrefix.resolve("com/example/data.xml").also { it.parent.createDirectories() }.createFile()
    val javatestsPrefix = projectRoot.resolve("javatests")
    val javatestsFile = javatestsPrefix.resolve("test/resources/test.properties").also { it.parent.createDirectories() }.createFile()

    val target = javaTarget(resources = listOf(srcMainResourcesFile, javaFile, javatestsFile))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(srcMainResources, javaPrefix, javatestsPrefix)
  }

  @Test
  fun `should detect multiple default Kotlin prefixes simultaneously`() {
    val srcMainResources = projectRoot.resolve("src/main/resources").createDirectories()
    val srcMainResourcesFile = srcMainResources.resolve("app.properties").createFile()
    val srcMainJava = projectRoot.resolve("src/main/java").createDirectories()
    val srcMainJavaFile = srcMainJava.resolve("com/example/data.xml").also { it.parent.createDirectories() }.createFile()
    val kotlinPrefix = projectRoot.resolve("kotlin").createDirectories()
    val kotlinFile = kotlinPrefix.resolve("config/settings.json").also { it.parent.createDirectories() }.createFile()

    val target = kotlinTarget(resources = listOf(srcMainResourcesFile, srcMainJavaFile, kotlinFile))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(srcMainResources, srcMainJava, kotlinPrefix)
  }

  @TestFactory
  fun `Java prefixes that should be detected`(): List<DynamicTest> = shouldDetectJavaPrefix(
    "src/main/resources",
    "src/test/resources",
    "src/functionalTest/resources",
    "src/integrationTest/resources",
    "src/something/resources",
    "src/main/java",
    "src/test/java",
    "javatests",
    "testsrc",
    "java",
    "under/nested/dir/src/main/resources",
    "under/nested/dir/src/test/resources",
    "under/nested/dir/src/main/java",
    "under/nested/dir/src/test/java",
  )

  @TestFactory
  fun `Java prefixes that should not be detected`(): List<DynamicTest> = shouldNotDetectJavaPrefix(
    "src/something/java",
    "src/main/some/some/resources",
    "src/test/some/some/resources",
    "src/some/main/java",
    "src/some/test/java",
    "src/main/some/java",
    "src/test/some/java",
  )

  @TestFactory
  fun `Kotlin prefixes that should be detected`(): List<DynamicTest> = shouldDetectKotlinPrefix(
    "src/main/java",
    "src/test/java",
    "src/main/resources",
    "src/test/resources",
    "kotlin",
    "under/nested/dir/src/main/java",
    "under/nested/dir/src/test/java",
    "under/nested/dir/src/main/resources",
    "under/nested/dir/src/test/resources",
    "under/nested/dir/kotlin",
    "src/kotlin",
    "src/custom/kotlin",
  )

  @TestFactory
  fun `Kotlin prefixes that should not be detected`(): List<DynamicTest> = shouldNotDetectKotlinPrefix(
    "src/something/java",
    "src/tests/java",
    "src/some/main/java",
    "src/main/some/resources",
    "src/test/some/resources",
    "src/custom/resources",
  )

  @TestFactory
  fun `Scala prefixes that should be detected`(): List<DynamicTest> = shouldDetectScalaPrefix(
    "resources",
    "java",
    "src/main/resources",
    "under/nested/dir/resources",
    "under/nested/dir/java",
  )

  @TestFactory
  fun `Scala prefixes that should not be detected`(): List<DynamicTest> = shouldNotDetectScalaPrefix(
    "src/something/scala",
    "src/main/python",
  )

  @Test
  fun `should fall back to a single-file resource root when the resource overlaps a source root`() {
    val javaRoot = projectRoot.resolve("src/main/java").createDirectories()
    val packageDir = javaRoot.resolve("com/example").createDirectories()
    val sourceFile = packageDir.resolve("App.java").createFile()
    val resourceFile = packageDir.resolve("config.xml").createFile()

    val target = javaTarget(
      sources = listOf(sourceFile),
      resources = listOf(resourceFile),
    )

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldHaveSingleElement resourceFile
  }

  @Test
  fun `should only fall back conflicting roots when some overlap with sources and others do not`() {
    val kotlinRoot = projectRoot.resolve("src/main/kotlin").createDirectories()
    val kotlinPackage = kotlinRoot.resolve("com/example").createDirectories()
    val sourceFile = kotlinPackage.resolve("Main.kt").createFile()
    val conflictingResource = kotlinPackage.resolve("template.html").createFile()
    val resourcesRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val safeResource = resourcesRoot.resolve("app.properties").createFile()

    val target = kotlinTarget(
      sources = listOf(sourceFile),
      resources = listOf(conflictingResource, safeResource),
    )

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(resourcesRoot, conflictingResource)
  }

  @Test
  fun `should merge resource roots normally when there are no sources`() {
    val kotlinRoot = projectRoot.resolve("src/main/kotlin").createDirectories()
    val packageDir = kotlinRoot.resolve("com/example").createDirectories()
    val resourceFile = packageDir.resolve("template.html").createFile()

    val target = kotlinTarget(resources = listOf(resourceFile))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldHaveSingleElement kotlinRoot
  }

  @Test
  fun `should fall back to single-file resource roots when explicit strip prefix overlaps a source root`() {
    val stripPrefix = projectRoot.resolve("sources").createDirectories()
    val packageDir = stripPrefix.resolve("com/example").createDirectories()
    val sourceFile = packageDir.resolve("Main.kt").createFile()
    val resourceFile = packageDir.resolve("data.json").createFile()

    val target = kotlinTarget(
      sources = listOf(sourceFile),
      resources = listOf(resourceFile),
      data = listOf(
        KotlinBuildTarget(
          languageVersion = null,
          apiVersion = null,
          kotlincOptions = emptyList(),
          associates = emptyList(),
          moduleName = null,
        ),
        JvmBuildTarget(javaVersion = "", resolvedResourceStripPrefix = stripPrefix),
      ),
    )

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldHaveSingleElement resourceFile
  }

  @Test
  fun `should accept the explicit strip prefix even when its tree contains a bazel symlink`() {
    val execrootTarget = projectRoot.resolve("execroot/_main").createDirectories()
    execrootTarget.resolve("generated.txt").createFile()
    val stripPrefix = projectRoot.resolve("mypackage").createDirectories()
    val resourceFile = stripPrefix.resolve("config.properties").createFile()
    stripPrefix.resolve("bazel-bin").createSymbolicLinkPointingTo(execrootTarget)

    val target = javaTarget(
      resources = listOf(resourceFile),
      data = listOf(JvmBuildTarget(javaVersion = "", resolvedResourceStripPrefix = stripPrefix)),
    )

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldHaveSingleElement stripPrefix
  }

  @Test
  fun `should accept the default strip prefix even when its tree contains a bazel symlink`() {
    val srcMainResources = projectRoot.resolve("src/main/resources").createDirectories()
    val resourceFile = srcMainResources.resolve("app.properties").createFile()
    val execrootTarget = projectRoot.resolve("execroot/_main").createDirectories()
    execrootTarget.resolve("extra.txt").createFile()
    srcMainResources.resolve("bazel-out").createSymbolicLinkPointingTo(execrootTarget)

    val target = javaTarget(resources = listOf(resourceFile))

    val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

    roots.map { it.resourcePath } shouldHaveSingleElement srcMainResources
  }

  private fun shouldDetectJavaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Java prefix '$prefix'") {
      val testRoot = projectRoot.resolve("java-detect-$i").createDirectories()
      val detected = testRoot.resolve(prefix).createDirectories()
      val resourceFile = detected.resolve("config.properties").createFile()
      val target = javaTarget(resources = listOf(resourceFile))

      val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

      roots.map { it.resourcePath } shouldHaveSingleElement detected
    }
  }

  private fun shouldNotDetectJavaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Java prefix '$prefix' (negative)") {
      val testRoot = projectRoot.resolve("java-no-detect-$i").createDirectories()
      val candidate = testRoot.resolve(prefix).createDirectories()
      val resourceFile = candidate.resolve("config.properties").createFile()
      val target = javaTarget(resources = listOf(resourceFile))

      val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

      roots.map { it.resourcePath } shouldNotContain candidate
    }
  }

  private fun shouldDetectKotlinPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Kotlin prefix '$prefix'") {
      val testRoot = projectRoot.resolve("kotlin-detect-$i").createDirectories()
      val detected = testRoot.resolve(prefix).createDirectories()
      val resourceFile = detected.resolve("config.properties").createFile()
      val target = kotlinTarget(resources = listOf(resourceFile))

      val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

      roots.map { it.resourcePath } shouldHaveSingleElement detected
    }
  }

  private fun shouldNotDetectKotlinPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Kotlin prefix '$prefix' (negative)") {
      val testRoot = projectRoot.resolve("kotlin-no-detect-$i").createDirectories()
      val candidate = testRoot.resolve(prefix).createDirectories()
      val resourceFile = candidate.resolve("config.properties").createFile()
      val target = kotlinTarget(resources = listOf(resourceFile))

      val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

      roots.map { it.resourcePath } shouldNotContain candidate
    }
  }

  private fun shouldDetectScalaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Scala prefix '$prefix'") {
      val testRoot = projectRoot.resolve("scala-detect-$i").createDirectories()
      val detected = testRoot.resolve(prefix).createDirectories()
      val resourceFile = detected.resolve("config.properties").createFile()
      val target = scalaTarget(resources = listOf(resourceFile))

      val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

      roots.map { it.resourcePath } shouldHaveSingleElement detected
    }
  }

  private fun shouldNotDetectScalaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Scala prefix '$prefix' (negative)") {
      val testRoot = projectRoot.resolve("scala-no-detect-$i").createDirectories()
      val candidate = testRoot.resolve(prefix).createDirectories()
      val resourceFile = candidate.resolve("config.properties").createFile()
      val target = scalaTarget(resources = listOf(resourceFile))

      val roots = ResourceRootBuilder.resolve(target, projectName, emptySet())

      roots.map { it.resourcePath } shouldNotContain candidate
    }
  }

  private fun javaTarget(
    label: String = "//target",
    ruleType: RuleType = RuleType.LIBRARY,
    sources: List<Path> = emptyList(),
    resources: List<Path> = emptyList(),
    data: List<BuildTargetData> = listOf(JvmBuildTarget(javaVersion = "")),
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "java_library",
      ruleType = ruleType,
      languageClasses = setOf(LanguageClass.JAVA),
    ),
    sources = sources,
    resources = resources,
    data = data,
  )

  private fun kotlinTarget(
    label: String = "//target",
    sources: List<Path> = emptyList(),
    resources: List<Path> = emptyList(),
    data: List<BuildTargetData> = listOf(
      KotlinBuildTarget(
        languageVersion = null,
        apiVersion = null,
        kotlincOptions = emptyList(),
        associates = emptyList(),
        moduleName = null,
      ),
    ),
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "kt_jvm_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(LanguageClass.KOTLIN),
    ),
    sources = sources,
    resources = resources,
    data = data,
  )

  private fun scalaTarget(
    label: String = "//target",
    sources: List<Path> = emptyList(),
    resources: List<Path> = emptyList(),
    data: List<BuildTargetData> = listOf(
      ScalaBuildTarget(
        scalaVersion = "2.13.0",
        scalacOptions = emptyList(),
        sdkJars = emptyList(),
      ),
    ),
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "scala_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(LanguageClass.SCALA),
    ),
    sources = sources,
    resources = resources,
    data = data,
  )
}
