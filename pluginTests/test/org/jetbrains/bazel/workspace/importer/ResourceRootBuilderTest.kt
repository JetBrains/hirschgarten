package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.ScalaBuildTarget
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.createTestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.BuildTargetData
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createSymbolicLinkPointingTo

class ResourceRootBuilderTest : MockProjectBaseTest() {

  private val projectName = "test-project"

  @TempDir
  private lateinit var tempDir: Path

  private val projectRoot: Path get() = projectDir.get()

  @Test
  fun `should mark resources as JAVA_RESOURCE_ROOT_TYPE by default`() {
    val resource = projectRoot.resolve("file.txt").createFile()
    val target = javaTarget(resources = listOf(resource))

    val roots = resolve(target)

    roots.map { it.rootType } shouldContainExactlyInAnyOrder listOf(JAVA_RESOURCE_ROOT_TYPE)
  }

  @Test
  fun `should not throw when a resource is missing on disk`() {
    val prefix = projectRoot.resolve("pkg/src/main/resources")
    val missingResource = prefix.resolve("com/example/a.txt")
    val target = javaTarget(resources = listOf(missingResource))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(prefix)
  }

  @Test
  fun `should mark resources of a TEST rule as JAVA_TEST_RESOURCE_ROOT_TYPE`() {
    val resource = projectRoot.resolve("file.txt").createFile()
    val target = javaTarget(
      ruleType = RuleType.TEST,
      resources = listOf(resource),
    )

    val roots = resolve(target)

    roots.map { it.rootType } shouldContainExactlyInAnyOrder listOf(JAVA_TEST_RESOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark resources of a testonly target as JAVA_TEST_RESOURCE_ROOT_TYPE`() {
    val resource = projectRoot.resolve("file.txt").createFile()
    val target = javaTarget(resources = listOf(resource), isTestOnly = true)

    val roots = resolve(target)

    roots.map { it.rootType } shouldContainExactlyInAnyOrder listOf(JAVA_TEST_RESOURCE_ROOT_TYPE)
  }

  @Test
  fun `should collapse by directory when no strip prefix applies (java target)`() {
    val dir = projectRoot.resolve("random/dir").createDirectories()
    val a = dir.resolve("a.txt").createFile()
    val sub = dir.resolve("sub").createDirectories()
    val b = sub.resolve("b.txt").createFile()
    val target = javaTarget(resources = listOf(a, b))

    val roots = resolve(target)

    // `dir` holds a nested leftover group so it cannot collapse, but the leaf can
    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(a, sub)
  }

  @Test
  fun `should collapse cross-package resources to one root per leaf directory`() {
    val fixtures = projectRoot.resolve("integrations/examples/resources").createDirectories()
    val leaves = (1..3).map { example ->
      fixtures.resolve("example_$example/test-resources/input").createDirectories()
    }
    val resources = leaves.flatMap { leaf -> (1..5).map { leaf.resolve("req_$it.json").createFile() } }

    val target = javaTarget(resources = resources)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder leaves
  }

  @Test
  fun `should climb above the immediate parent when a package encloses cross-package resources`() {
    val pkg = projectRoot.resolve("integrations/examples/resources").createDirectories()
    pkg.resolve("BUILD").createFile()
    val example = pkg.resolve("example").createDirectories()
    val input = example.resolve("test-resources/input").createDirectories()
    val output = example.resolve("test-resources/output").createDirectories()
    val a = input.resolve("a.json").createFile()
    val b = output.resolve("b.json").createFile()

    val target = javaTarget(resources = listOf(a, b))

    val roots = resolve(target)

    // climbs out of input/ and output/ and stops below the package, whose BUILD file makes it dirty
    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(example)
  }

  @Test
  fun `should collapse only to parents when no package encloses the resources`() {
    val fixtures = projectRoot.resolve("integrations/examples/resources").createDirectories()
    val input = fixtures.resolve("example/test-resources/input").createDirectories()
    val output = fixtures.resolve("example/test-resources/output").createDirectories()
    val a = input.resolve("a.json").createFile()
    val b = output.resolve("b.json").createFile()

    val target = javaTarget(resources = listOf(a, b))

    // no BUILD file anywhere, so there is no package to walk
    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(input, output)
  }

  @Test
  fun `should not climb for resources living outside the workspace root`() {
    val external = tempDir.resolve("external/repo/data").createDirectories()
    external.parent.resolve("BUILD").createFile()
    val a = external.resolve("a.json").createFile()
    val b = external.resolve("b.json").createFile()

    val target = javaTarget(resources = listOf(a, b))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(external)
  }

  @Test
  fun `should strip the Java src-main-resources convention down to that prefix`() {
    val prefix = projectRoot.resolve("pkg/src/main/resources").createDirectories()
    val res1 = prefix.resolve("com/example/a.txt").also { it.parent.createDirectories() }.createFile()
    val res2 = prefix.resolve("com/example/b.txt").createFile()
    val target = javaTarget(resources = listOf(res1, res2))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(prefix)
  }

  @Test
  fun `should strip the Kotlin conventional segments (src-main-resources, kotlin)`() {
    val kotlinPrefix = projectRoot.resolve("pkg/kotlin").createDirectories()
    val res1 = kotlinPrefix.resolve("com/example/a.txt").also { it.parent.createDirectories() }.createFile()
    val mavenPrefix = projectRoot.resolve("pkg/src/main/resources").createDirectories()
    val res2 = mavenPrefix.resolve("d/e.txt").also { it.parent.createDirectories() }.createFile()
    val target = kotlinTarget(resources = listOf(res1, res2))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(kotlinPrefix, mavenPrefix)
  }

  @Test
  fun `should strip Scala conventional segments (resources, java)`() {
    val resPrefix = projectRoot.resolve("scala/pkg/resources").createDirectories()
    val res1 = resPrefix.resolve("a.txt").createFile()
    val javaPrefix = projectRoot.resolve("scala/pkg/java").createDirectories()
    val res2 = javaPrefix.resolve("d/e.txt").also { it.parent.createDirectories() }.createFile()
    val target = scalaTarget(resources = listOf(res1, res2))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(resPrefix, javaPrefix)
  }

  @Test
  fun `should honor the JvmBuildTarget resolvedResourceStripPrefix when present`() {
    val prefix = projectRoot.resolve("explicit/prefix").createDirectories()
    val res1 = prefix.resolve("com/example/a.txt").also { it.parent.createDirectories() }.createFile()
    val res2 = prefix.resolve("com/example/b.txt").createFile()
    val target = createTestBuildTarget(
      id = Label.parse("//target"),
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(JavaLanguageClass.JAVA),
      ),
      resources = listOf(res1, res2),
      data = listOf(
        JvmBuildTarget(
          resolvedResourceStripPrefix = prefix,
        ),
      ),
    )

    val roots = resolve(target)

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

    val roots = resolve(target)

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

    val roots = resolve(target)

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

    val roots = resolve(target)

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

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(resourcesRoot, conflictingResource)
  }

  @Test
  fun `should merge resource roots normally when there are no sources`() {
    val kotlinRoot = projectRoot.resolve("src/main/kotlin").createDirectories()
    val packageDir = kotlinRoot.resolve("com/example").createDirectories()
    val resourceFile = packageDir.resolve("template.html").createFile()

    val target = kotlinTarget(resources = listOf(resourceFile))

    val roots = resolve(target)

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
        JvmBuildTarget(resolvedResourceStripPrefix = stripPrefix),
      ),
    )

    val roots = resolve(target)

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
      data = listOf(JvmBuildTarget(resolvedResourceStripPrefix = stripPrefix)),
    )

    val roots = resolve(target)

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

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldHaveSingleElement srcMainResources
  }

  @Test
  fun `should collapse leftover resources sharing a clean immediate parent directory`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("app.properties").createFile()
    val leftoversParent = projectRoot.resolve("extra/data").createDirectories()
    val leftovers = ('a'..'e').map { leftoversParent.resolve("$it.txt").createFile() }

    val target = javaTarget(resources = listOf(gateResource) + leftovers)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, leftoversParent)
  }

  @Test
  fun `should not collapse a parent that has resource files nested in subdirectories`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("x.txt").createFile()
    val area = projectRoot.resolve("area").createDirectories()
    val flat = area.resolve("a.txt").createFile()
    val nestedParent = area.resolve("sub").createDirectories()
    val nested = nestedParent.resolve("b.txt").createFile()

    val target = javaTarget(resources = listOf(gateResource, flat, nested))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, flat, nestedParent)
  }

  @Test
  fun `should collapse to the immediate parent even when the grandparent is dirty`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("x.txt").createFile()
    val extra = projectRoot.resolve("extra").createDirectories()
    val data = extra.resolve("data").createDirectories()
    val leftover = data.resolve("y.txt").createFile()
    extra.resolve("README.md").createFile()

    val target = javaTarget(resources = listOf(gateResource, leftover))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, data)
  }

  @Test
  fun `should collapse a leftover whose immediate parent does not overlap a merged prefix`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("x.txt").createFile()
    val extras = projectRoot.resolve("src/extras").createDirectories()
    val leftover = extras.resolve("y.txt").createFile()

    val target = kotlinTarget(resources = listOf(gateResource, leftover))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, extras)
  }

  @Test
  fun `should fall back to single-file root when the leftover's parent is dirty`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("x.txt").createFile()
    val pkg = projectRoot.resolve("pkg").createDirectories()
    val leftover = pkg.resolve("y.txt").createFile()
    pkg.resolve("Helper.kt").createFile()

    val target = javaTarget(resources = listOf(gateResource, leftover))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, leftover)
  }

  @Test
  fun `should collapse a large fan-out of leftovers into per-immediate-parent roots`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("app.properties").createFile()
    val assets = projectRoot.resolve("assets").createDirectories()
    val img = assets.resolve("img").createDirectories()
    val data = assets.resolve("data").createDirectories()
    val imgFiles = (1..25).map { img.resolve("$it.png").createFile() }
    val dataFiles = (1..25).map { data.resolve("$it.json").createFile() }

    val target = javaTarget(resources = listOf(gateResource) + imgFiles + dataFiles)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, img, data)
  }

  @Test
  fun `should keep leftovers from independent subtrees as separate roots when their common ancestor is dirty`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("x.txt").createFile()
    val unrelated = projectRoot.resolve("unrelated").createDirectories()
    val areaA = unrelated.resolve("area-a").createDirectories()
    val areaB = unrelated.resolve("area-b").createDirectories()
    val a1 = areaA.resolve("1.txt").createFile()
    val a2 = areaA.resolve("2.txt").createFile()
    val b1 = areaB.resolve("1.txt").createFile()
    val b2 = areaB.resolve("2.txt").createFile()
    unrelated.resolve("README.md").createFile()

    val target = javaTarget(resources = listOf(gateResource, a1, a2, b1, b2))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, areaA, areaB)
  }

  @Test
  fun `should collapse inside a source content root and set relativeOutputPath so the FQN is preserved`() {
    val gateRoot = projectRoot.resolve("src/main/resources").createDirectories()
    val gateResource = gateRoot.resolve("app.properties").createFile()
    val kotlinRoot = projectRoot.resolve("src/main/kotlin").createDirectories()
    val messages = kotlinRoot.resolve("messages").createDirectories()
    val bundle1 = messages.resolve("KotlinBundle.properties").createFile()
    val bundle2 = messages.resolve("KotlinBundle1.properties").createFile()
    val sourceFile = kotlinRoot.resolve("com/example/Module.kt").also { it.parent.createDirectories() }.createFile()

    val target = kotlinTarget(
      sources = listOf(sourceFile),
      resources = listOf(gateResource, bundle1, bundle2),
    )

    val roots = resolve(target, sourceContentRoots = listOf(kotlinRoot))

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(gateRoot, messages)
    val messagesRoot = roots.single { it.resourcePath == messages }
    messagesRoot.relativeOutputPath shouldBe "messages"
    val gateRootResolved = roots.single { it.resourcePath == gateRoot }
    gateRootResolved.relativeOutputPath shouldBe ""
  }

  @Test
  fun `should aggressively collapse a clean filegroup target up to its base directory`() {
    val baseDir = projectRoot.resolve("pkg").createDirectories()
    val flat = baseDir.resolve("a.txt").createFile()
    val nested = baseDir.resolve("sub/b.txt").also { it.parent.createDirectories() }.createFile()
    val deeper = baseDir.resolve("sub/deeper/c.txt").also { it.parent.createDirectories() }.createFile()

    val target = filegroupTarget(resources = listOf(flat, nested, deeper), baseDirectory = baseDir)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(baseDir)
  }

  @Test
  fun `should stop the aggressive climb at the first dirty ancestor under the ceiling`() {
    val baseDir = projectRoot.resolve("pkg").createDirectories()
    val component = baseDir.resolve("component").createDirectories()
    component.resolve("Module.java").createFile()
    val testResources = component.resolve("test-resources").createDirectories()
    val input = testResources.resolve("input").createDirectories()
    val output = testResources.resolve("output").createDirectories()
    val inputRes = input.resolve("a.txt").createFile()
    val outputRes = output.resolve("b.txt").createFile()

    val target = filegroupTarget(resources = listOf(inputRes, outputRes), baseDirectory = baseDir)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(testResources)
  }

  @Test
  fun `should not climb a filegroup target above its base directory`() {
    val baseDir = projectRoot.resolve("pkg").createDirectories()
    val nested = baseDir.resolve("sub/x.txt").also { it.parent.createDirectories() }.createFile()

    val target = filegroupTarget(resources = listOf(nested), baseDirectory = baseDir)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(baseDir)
  }

  @Test
  fun `should produce per-subtree roots for a filegroup whose ceiling has a dirty sibling`() {
    val baseDir = projectRoot.resolve("pkg").createDirectories()
    val areaA = baseDir.resolve("area-a").createDirectories()
    val areaB = baseDir.resolve("area-b").createDirectories()
    val a = areaA.resolve("x.txt").createFile()
    val b = areaB.resolve("y.txt").createFile()
    baseDir.resolve("README.md").createFile()

    val target = filegroupTarget(resources = listOf(a, b), baseDirectory = baseDir)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(areaA, areaB)
  }

  @Test
  fun `should collapse a large filegroup into a single root`() {
    val baseDir = projectRoot.resolve("pkg").createDirectories()
    val data1 = baseDir.resolve("data1").createDirectories()
    val data2 = baseDir.resolve("data2").createDirectories()
    val files1 = (1..200).map { data1.resolve("$it.txt").createFile() }
    val files2 = (1..200).map { data2.resolve("$it.json").createFile() }

    val target = filegroupTarget(resources = files1 + files2, baseDirectory = baseDir)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(baseDir)
  }

  @Test
  fun `should collapse a shared cross-package fixture instead of one root per file`() {
    val pkg = projectRoot.resolve("integrations/examples/resources").createDirectories()
    pkg.resolve("BUILD").createFile()
    pkg.resolve("README.md").createFile()
    val examples = (1..50).map { c ->
      val example = pkg.resolve("example_$c").createDirectories()
      example.resolve("example_$c.cc").createFile()
      example.resolve("test-resources").createDirectories()
    }
    val fixtures = examples.flatMap { testResources ->
      listOf("input", "output").flatMap { dir ->
        val leaf = testResources.resolve(dir).createDirectories()
        (1..20).map { leaf.resolve("case_$it.json").createFile() }
      }
    }

    val target = javaTarget(resources = fixtures)

    val roots = resolve(target)

    fixtures.size shouldBe 2000
    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder examples
  }

  @Test
  fun `should stop the aggressive climb when the dirty file hides deep inside a sibling subtree`() {
    val baseDir = projectRoot.resolve("pkg").createDirectories()
    val res = baseDir.resolve("res").createDirectories()
    val nested = res.resolve("nested").createDirectories()
    val resource = nested.resolve("a.txt").createFile()
    val sibling = baseDir.resolve("sibling/deep/deeper").createDirectories()
    sibling.resolve("Code.java").createFile()

    val target = filegroupTarget(resources = listOf(resource), baseDirectory = baseDir)

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(res)
  }

  @Test
  fun `should keep per-target dirtiness apart when VFS`() {
    val dir = projectRoot.resolve("shared/data").createDirectories()
    val a = dir.resolve("a.txt").createFile()
    val b = dir.resolve("b.txt").createFile()

    val fullTarget = javaTarget(resources = listOf(a, b))
    val fullRoots = resolve(fullTarget)
    fullRoots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(dir)

    val partialTarget = javaTarget(label = "//other", resources = listOf(a))
    val partialRoots = resolve(partialTarget)
    partialRoots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(a)
  }

  @Test
  fun `should treat a non-bazel symlinked subdirectory as dirty`() {
    val dir = projectRoot.resolve("looped").createDirectories()
    val resource = dir.resolve("a.txt").createFile()
    dir.resolve("loop").createSymbolicLinkPointingTo(dir)

    val target = javaTarget(resources = listOf(resource))

    val roots = resolve(target)

    roots.map { it.resourcePath } shouldContainExactlyInAnyOrder listOf(resource)
  }

  private fun shouldDetectJavaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Java prefix '$prefix'") {
      val testRoot = projectRoot.resolve("java-detect-$i").createDirectories()
      val detected = testRoot.resolve(prefix).createDirectories()
      val resourceFile = detected.resolve("config.properties").createFile()
      val target = javaTarget(resources = listOf(resourceFile))

      val roots = resolve(target)

      roots.map { it.resourcePath } shouldHaveSingleElement detected
    }
  }

  private fun shouldNotDetectJavaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Java prefix '$prefix' (negative)") {
      val testRoot = projectRoot.resolve("java-no-detect-$i").createDirectories()
      val candidate = testRoot.resolve(prefix).createDirectories()
      val resourceFile = candidate.resolve("config.properties").createFile()
      candidate.resolve("NotAResource.md").createFile()
      val target = javaTarget(resources = listOf(resourceFile))

      val roots = resolve(target)

      roots.map { it.resourcePath } shouldNotContain candidate
    }
  }

  private fun shouldDetectKotlinPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Kotlin prefix '$prefix'") {
      val testRoot = projectRoot.resolve("kotlin-detect-$i").createDirectories()
      val detected = testRoot.resolve(prefix).createDirectories()
      val resourceFile = detected.resolve("config.properties").createFile()
      val target = kotlinTarget(resources = listOf(resourceFile))

      val roots = resolve(target)

      roots.map { it.resourcePath } shouldHaveSingleElement detected
    }
  }

  private fun shouldNotDetectKotlinPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Kotlin prefix '$prefix' (negative)") {
      val testRoot = projectRoot.resolve("kotlin-no-detect-$i").createDirectories()
      val candidate = testRoot.resolve(prefix).createDirectories()
      val resourceFile = candidate.resolve("config.properties").createFile()
      candidate.resolve("NotAResource.md").createFile()
      val target = kotlinTarget(resources = listOf(resourceFile))

      val roots = resolve(target)

      roots.map { it.resourcePath } shouldNotContain candidate
    }
  }

  private fun shouldDetectScalaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Scala prefix '$prefix'") {
      val testRoot = projectRoot.resolve("scala-detect-$i").createDirectories()
      val detected = testRoot.resolve(prefix).createDirectories()
      val resourceFile = detected.resolve("config.properties").createFile()
      val target = scalaTarget(resources = listOf(resourceFile))

      val roots = resolve(target)

      roots.map { it.resourcePath } shouldHaveSingleElement detected
    }
  }

  private fun shouldNotDetectScalaPrefix(vararg prefixes: String): List<DynamicTest> = prefixes.mapIndexed { i, prefix ->
    DynamicTest.dynamicTest("Scala prefix '$prefix' (negative)") {
      val testRoot = projectRoot.resolve("scala-no-detect-$i").createDirectories()
      val candidate = testRoot.resolve(prefix).createDirectories()
      val resourceFile = candidate.resolve("config.properties").createFile()
      candidate.resolve("NotAResource.md").createFile()
      val target = scalaTarget(resources = listOf(resourceFile))

      val roots = resolve(target)

      roots.map { it.resourcePath } shouldNotContain candidate
    }
  }

  private fun resolve(target: TestBuildTarget, sourceContentRoots: List<Path> = emptyList()) =
    ResourceRootBuilder.resolve(
      target = target,
      bazelProjectName = projectName,
      workspaceRoot = projectRoot,
      sourceContentRoots = sourceContentRoots,
    )

  private fun javaTarget(
    label: String = "//target",
    ruleType: RuleType = RuleType.LIBRARY,
    sources: List<Path> = emptyList(),
    resources: List<Path> = emptyList(),
    data: List<BuildTargetData> = listOf(JvmBuildTarget()),
    isTestOnly: Boolean = false,
  ): TestBuildTarget = createTestBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "java_library",
      ruleType = ruleType,
      languageClasses = setOf(JavaLanguageClass.JAVA),
    ),
    sources = sources,
    resources = resources,
    data = data,
    isTestOnly = isTestOnly,
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
  ): TestBuildTarget = createTestBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "kt_jvm_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(JavaLanguageClass.KOTLIN),
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
      ),
    ),
  ): TestBuildTarget = createTestBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "scala_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(JavaLanguageClass.SCALA),
    ),
    sources = sources,
    resources = resources,
    data = data,
  )

  private fun filegroupTarget(
    label: String = "//target",
    resources: List<Path> = emptyList(),
    baseDirectory: Path,
  ): TestBuildTarget = createTestBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "filegroup",
      ruleType = RuleType.LIBRARY,
      languageClasses = emptySet(),
    ),
    resources = resources,
    baseDirectory = baseDirectory,
    data = emptyList(),
  )
}
