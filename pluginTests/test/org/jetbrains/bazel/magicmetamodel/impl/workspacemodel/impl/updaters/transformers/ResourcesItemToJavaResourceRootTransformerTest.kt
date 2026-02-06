package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

@DisplayName("ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem) tests")
class ResourcesItemToJavaResourceRootTransformerTest {
  @TempDir
  private lateinit var tempDir: Path

  private lateinit var projectBasePath: Path

  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer()

  @BeforeEach
  fun beforeEach() {
    projectBasePath = tempDir.resolve("project").createDirectories()
  }

  @Test
  fun `should return no resources roots for no resources items`() {
    // given
    val buildTarget = createRawBuildTarget()

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldBe emptyList()
  }

  @Test
  fun `should return resource root with type test for resources item coming from a build target having test target kind`() {
    // given
    val resourceFilePath = projectBasePath.resolve("resourceFile.txt").createFile()

    val buildTarget = createRawBuildTarget(
      kind = TargetKind(
        kindString = "java_test",
        ruleType = RuleType.TEST,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      resources = listOf(resourceFilePath),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then

    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceFilePath,
      rootType = SourceRootTypeId("java-test-resource"),
    )
  }

  @Test
  fun `should return single resource root for resources item with one file in non standard directory`() {
    // given
    val resourceFilePath = projectBasePath.resolve("resourceFile.txt").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFilePath))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceFilePath,
      rootType = SourceRootTypeId("java-resource"),
    )

  }

  @Test
  fun `should return single resource root for single and non standard directory`() {
    // given
    val resourceDirPath = projectBasePath.resolve("resource").createDirectories()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceDirPath))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then

    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceDirPath,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should return resource root for each file when in the same non standard directory`() {
    // given
    val resourceFilePath1 = projectBasePath.resolve("resourceFile1.txt").createFile()
    val resourceFilePath2 = projectBasePath.resolve("resourceFile2.txt").createFile()
    val resourceFilePath3 = projectBasePath.resolve("resourceFile3.txt").createFile()

    val buildTarget = createRawBuildTarget(
      resources = listOf(resourceFilePath1, resourceFilePath2, resourceFilePath3),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath3,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }

  @Test
  fun `should return resource root for each resource when in non standard directory`() {
    // given
    val resourceFilePath1 = projectBasePath.resolve("resourceFile1.txt").createFile()
    val resourceFilePath2 = projectBasePath.resolve("resourceFile2.txt").createFile()
    val resourceDirPath3 = projectBasePath.resolve("resourcedir").createDirectories()

    val buildTarget =
      createRawBuildTarget(resources = listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceDirPath3,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }

  @Test
  fun `should return resource roots regardless they have resource items in project base path or not`() {
    // given
    val resourceFilePath1 = projectBasePath.resolve("resource1File1.txt").createFile()
    val resourceFilePath2 = tempDir.resolve("resource2File2.txt").createFile()
    val resourceDirPath3 = projectBasePath.resolve("resourcedir").createDirectories()

    val buildTarget =
      createRawBuildTarget(resources = listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceDirPath3,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }


  @Test
  fun `should use explicit prefix for Kotlin`() {
    val resourceStripPrefix = projectBasePath.resolve("src/main/resources/res")
    shouldDetectPrefix(
      prefix = resourceStripPrefix,
      subhierarchy = "",
      data = KotlinBuildTarget(
        languageVersion = "1.8",
        apiVersion = "1.8",
        kotlincOptions = emptyList(),
        associates = emptyList(),
        jvmBuildTarget = JvmBuildTarget(
          javaVersion = "17",
          resourceStripPrefix = resourceStripPrefix,
        ),
      ),
    )
  }

  @Test
  fun `should detect multiple default prefixes for Kotlin`() {
    // given
    val srcMainResourcesPrefix = projectBasePath.resolve("src/main/resources").createDirectories()
    val srcMainResourcesFile = srcMainResourcesPrefix.resolve("app.properties").createFile()

    val srcMainJavaPrefix = projectBasePath.resolve("src/main/java").createDirectories()
    val srcMainJavaSubdir = srcMainJavaPrefix.resolve("com/example").createDirectories()
    val srcMainJavaFile = srcMainJavaSubdir.resolve("data.xml").createFile()

    val kotlinPrefix = projectBasePath.resolve("kotlin").createDirectories()
    val kotlinSubdir = kotlinPrefix.resolve("config").createDirectories()
    val kotlinFile = kotlinSubdir.resolve("settings.json").createFile()

    val buildTarget = createRawBuildTarget(
      resources = listOf(srcMainResourcesFile, srcMainJavaFile, kotlinFile),
      data = KotlinBuildTarget(
        languageVersion = "1.8",
        apiVersion = "1.8",
        kotlincOptions = emptyList(),
        associates = emptyList(),
        jvmBuildTarget = JvmBuildTarget(
          javaVersion = "17",
          resourceStripPrefix = null,
        ),
      ),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(resourcePath = srcMainResourcesPrefix, rootType = SourceRootTypeId("java-resource")),
      ResourceRoot(resourcePath = srcMainJavaPrefix, rootType = SourceRootTypeId("java-resource")),
      ResourceRoot(resourcePath = kotlinPrefix, rootType = SourceRootTypeId("java-resource")),
    )
  }

  @TestFactory
  fun `should detect src main or test java prefix for Kotlin`() = shouldDetectKotlinPrefixTests(
    "src/main/java",
    "src/test/java",
    "under/nested/dir/src/main/java",
    "under/nested/dir/src/test/java",
  )


  @TestFactory
  fun `should not detect src _ java prefix for Kotlin`() = shouldNotDetectKotlinPrefixTests(
    "src/something/java",
    "src/tests/java",
    "src/some/main/java",
    "src/some/test/java",
    "src/main/some/java",
    "src/test/some/java",
  )

  @TestFactory
  fun `should detect src main or test resources prefix for Kotlin`() = shouldDetectKotlinPrefixTests(
    "src/main/resources",
    "under/nested/dir/src/main/resources",
    "src/test/java",
    "under/nested/dir/src/test/java",
  )

  @TestFactory
  fun `should not detect src _ resources prefix with for Kotlin`() = shouldNotDetectKotlinPrefixTests(
    "src/main/some/resources",
    "src/test/some/resources",
    "src/custom/resources",
    "under/nested/dir/src/main/some/resources",
    "under/nested/dir/src/test/some/resources",
    "under/nested/dir/src/custom/resources",
  )

  @TestFactory
  fun `should detect kotlin prefix for Kotlin`() = shouldDetectKotlinPrefixTests(
    "kotlin",
    "under/nested/dir/kotlin",
    "src/kotlin",
    "src/custom/kotlin",
    "under/nested/dir/src/kotlin",
    "under/nested/dir/src/custom/kotlin",
  )

  @Test
  fun `should use explicit prefix for Scala`() {
    val resourceStripPrefix = projectBasePath.resolve("src/main/resources/res")
    shouldDetectPrefix(
      prefix = resourceStripPrefix,
      subhierarchy = "",
      data = ScalaBuildTarget(
        scalaVersion = "2.13",
        sdkJars = emptyList(),
        scalacOptions = emptyList(),
        jvmBuildTarget = JvmBuildTarget(
          javaVersion = "17",
          resourceStripPrefix = resourceStripPrefix,
        ),
      ),
    )
  }

  @Test
  fun `should detect multiple prefixes for Scala`() {
    // given
    val srcMainResourcesPrefix = projectBasePath.resolve("src/main/resources").createDirectories()
    val srcMainResourcesSubdir = srcMainResourcesPrefix.resolve("config").createDirectories()
    val srcMainResourcesFile = srcMainResourcesSubdir.resolve("app.properties").createFile()

    val srcTestJavaPrefix = projectBasePath.resolve("src/test/java").createDirectories()
    val srcTestJavaSubdir = srcTestJavaPrefix.resolve("test/data").createDirectories()
    val srcTestJavaFile = srcTestJavaSubdir.resolve("test.xml").createFile()

    val externalPrefix = projectBasePath.resolve("external/mylib").createDirectories()
    val externalSubdir = externalPrefix.resolve("resources").createDirectories()
    val externalFile = externalSubdir.resolve("library.properties").createFile()

    val buildTarget = createRawBuildTarget(
      resources = listOf(srcMainResourcesFile, srcTestJavaFile, externalFile),
      data = ScalaBuildTarget(
        scalaVersion = "2.13",
        sdkJars = emptyList(),
        scalacOptions = emptyList(),
        jvmBuildTarget = JvmBuildTarget(
          javaVersion = "17",
          resourceStripPrefix = null,
        ),
      ),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(resourcePath = srcMainResourcesPrefix, rootType = SourceRootTypeId("java-resource")),
      ResourceRoot(resourcePath = srcTestJavaPrefix, rootType = SourceRootTypeId("java-resource")),
      ResourceRoot(resourcePath = externalPrefix, rootType = SourceRootTypeId("java-resource")),
    )
  }

  @TestFactory
  fun `should detect resources prefix for Scala`() = shouldDetectScalaPrefixTests(
    prefixes = listOf(
      "src/main/resources",
      "src/test/resources",
      "src/custom/resources",
      "src/resources",
      "resources",
      "under/nested/dir/src/main/resources",
      "under/nested/dir/src/test/resources",
      "under/nested/dir/src/custom/resources",
      "under/nested/dir/src/resources",
      "under/nested/dir/resources",
    ),
    subhierarchies = listOf(
      "some/resources",
      "resources",
      "",
    ),
  )

  @TestFactory
  fun `should detect java prefix for Scala`() = shouldDetectScalaPrefixTests(
    prefixes = listOf(
      "src/main/java",
      "src/test/java",
      "src/custom/java",
      "src/java",
      "java",
      "under/nested/dir/src/main/java",
      "under/nested/dir/src/test/java",
      "under/nested/dir/src/custom/java",
      "under/nested/dir/src/java",
      "under/nested/dir/java",
    ),
    subhierarchies = listOf(
      "some/java",
      "java",
      "",
    ),
  )

  @TestFactory
  fun `should detect external subdirectories prefix for Scala`() = shouldDetectScalaPrefixTests(
    "external/some",
    "external/other"
  )

  @TestFactory
  fun `should not detect direct external prefix for Scala`() = shouldNotDetectScalaPrefixTests(
    "external",
  )

  @Test
  fun `should use explicit prefix for Java`() {
    val resourceStripPrefix = projectBasePath.resolve("src/main/resources/res")
    shouldDetectPrefix(
      prefix = resourceStripPrefix,
      subhierarchy = "",
      data = JvmBuildTarget(
        javaVersion = "17",
        resourceStripPrefix = resourceStripPrefix,
      ),
    )
  }

  @Test
  fun `should detect multiple default prefixes for Java`() {
    // given
    val srcMainResourcesPrefix = projectBasePath.resolve("src/main/resources").createDirectories()
    val srcMainResourcesFile = srcMainResourcesPrefix.resolve("app.properties").createFile()

    val javaPrefix = projectBasePath.resolve("java")
    val javaSubdir = javaPrefix.resolve("com/example").createDirectories()
    val javaFile = javaSubdir.resolve("data.xml").createFile()

    val javatestsPrefix = projectBasePath.resolve("javatests")
    val javatestsSubdir = javatestsPrefix.resolve("test/resources").createDirectories()
    val javatestsFile = javatestsSubdir.resolve("test.properties").createFile()

    val buildTarget = createRawBuildTarget(
      resources = listOf(srcMainResourcesFile, javaFile, javatestsFile),
      data = JvmBuildTarget(javaVersion = "17", resourceStripPrefix = null),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(resourcePath = srcMainResourcesPrefix, rootType = SourceRootTypeId("java-resource")),
      ResourceRoot(resourcePath = javaPrefix, rootType = SourceRootTypeId("java-resource")),
      ResourceRoot(resourcePath = javatestsPrefix, rootType = SourceRootTypeId("java-resource")),
    )
  }

  @TestFactory
  fun `should detect src _ resources prefix for Java`() = shouldDetectJavaPrefixTests(
    "src/main/resources",
    "src/test/resources",
    "src/functionaTest/resources",
    "src/integrationTest/resources",
    "src/custom/resources",
    "java/src/something/resources",
    "javatests/src/something/resources",
    "testsrc/src/something/resources",
    "under/nested/dir/src/main/resources",
    "under/nested/dir/src/test/resources",
    "under/nested/dir/src/functionaTest/resources",
    "under/nested/dir/src/integrationTest/resources",
    "under/nested/dir/src/custom/resources",
  )

  @TestFactory
  fun `should not detect src _ resources prefix with more than one segment in between for Java`() = shouldNotDetectJavaPrefixTests(
    "src/main/some/some/resources",
    "src/test/some/some/resources",
    "java/src/some/some/resources",
    "javatests/src/some/some/resources",
    "testsrc/src/some/some/resources",
    "under/nested/dir/src/main/some/resources",
    "under/nested/dir/src/test/some/resources",
  )

  @TestFactory
  fun `should detect src main or test java prefix for Java`() = shouldDetectJavaPrefixTests(
    "src/main/java",
    "src/test/java",
    "under/nested/dir/src/main/java",
    "under/nested/dir/src/test/java",
  )

  @TestFactory
  fun `should not detect src _ java prefix for Java`() = shouldNotDetectJavaPrefixTests(
    "src/something/java",
    "src/tests/java",
    "src/some/main/java",
    "src/some/test/java",
    "src/main/some/java",
    "src/test/some/java",
  )

  @TestFactory
  fun `should detect first javatests prefix for Java`() = shouldDetectJavaPrefixTests(
    prefixes = listOf("javatests"),
    subhierarchies = listOf(
      "javatests",
      "testsrc",
      "something/javatests",
      "java",
      "someting/java",
      "something/src",
      "src",
      "src/com",
      "",
    ),
  )

  @TestFactory
  fun `should detect first testsrc prefix for Java`() = shouldDetectJavaPrefixTests(
    prefixes = listOf("testsrc"),
    subhierarchies = listOf(
      "javatests",
      "testsrc",
      "something/javatests",
      "java",
      "someting/java",
      "something/src",
      "src",
      "src/com",
      "",
    ),
  )

  @TestFactory
  fun `should detect first java prefix for Java`() = shouldDetectJavaPrefixTests(
    prefixes = listOf("java"),
    subhierarchies = listOf(
      "",
      "java",
      "src/com/example/java",
      "someting/java",
      "something/and/java",
      "com/example/java",
      "com/example/src",
    ),
  )

  @TestFactory
  fun `should detect single nested src with common packages of Java`() = shouldDetectJavaPrefixTests(
    prefixes = listOf("src/src"),
    subhierarchies = listOf(
      "com/example/java",
      "com/example/src",
      "net/example/java",
      "net/example/src",
      "org/example/java",
      "org/example/src",
    ),
  )

  @TestFactory
  fun `should not detect more than nested src with common packages of Java`() = shouldNotDetectJavaPrefixTests(
    prefixes = listOf("src/src/src"),
    subhierarchies = listOf(
      "com/example/java",
      "com/example/src",
      "net/example/java",
      "net/example/src",
      "org/example/java",
      "org/example/src",
    ),
  )

  @TestFactory
  fun `should not detect single nested src without common packages of Java`() = shouldNotDetectJavaPrefixTests(
    prefixes = listOf(
      "java/something/src",
      "java/src",
      "src/src",
    ),
    subhierarchies = listOf(
      "dev/example/java",
      "dev/example/src",
      "io/example/java",
      "io/example/src",
      "ai/example/java",
      "ai/example/src",
    ),
  )

  @TestFactory
  fun `should detect single nested java or javatests in src with common packages of Java`() = shouldDetectJavaPrefixTests(
    prefixes = listOf(
      "src/something/java",
      "src/java",
      "src/something/javatests",
      "src/javatests",
    ),
    subhierarchies = listOf(
      "com/example/javatests",
      "com/example/java",
      "com/example/src",
      "net/example/javatests",
      "net/example/java",
      "net/example/src",
      "org/example/javatests",
      "org/example/java",
      "org/example/src",
    ),
  )

  @TestFactory
  fun `should not detect more than nested java or javatests in src with common packages of Java`() = shouldNotDetectJavaPrefixTests(
    prefixes = listOf(
      "src/something/java/java",
      "src/java/java",
      "src/something/javatests/javatests",
      "src/javatests/javatests",
      "src/javatests/java",
    ),
    subhierarchies = listOf(
      "com/example/javatests",
      "com/example/java",
      "com/example/src",
      "net/example/javatests",
      "net/example/java",
      "net/example/src",
      "org/example/javatests",
      "org/example/java",
      "org/example/src",
    ),
  )

  @TestFactory
  fun `should not detect single nested java or javatests in src without common packages of Java`() = shouldNotDetectJavaPrefixTests(
    prefixes = listOf(
      "src/something/java",
      "src/java",
      "src/something/javatests",
      "src/javatests",
    ),
    subhierarchies = listOf(
      "dev/example/javatests",
      "dev/example/java",
      "dev/example/src",
      "io/example/javatests",
      "io/example/java",
      "io/example/src",
      "ai/example/javatests",
      "ai/example/java",
      "ai/example/src",
    ),
  )

  private fun shouldDetectJavaPrefixTests(vararg paths: String) = shouldDetectJavaPrefixTests(
    prefixes = paths.asList(),
    subhierarchies = listOf(""),
  )

  private fun shouldNotDetectJavaPrefixTests(vararg paths: String) = shouldNotDetectJavaPrefixTests(
    prefixes = paths.asList(),
    subhierarchies = listOf(""),
  )

  private fun shouldDetectJavaPrefixTests(prefixes: List<String>, subhierarchies: List<String>) = shouldDetectPrefixTests(
    prefixes = prefixes,
    subhierarchies = subhierarchies,
    data = JvmBuildTarget(javaVersion = "17", resourceStripPrefix = null),
  )


  private fun shouldNotDetectJavaPrefixTests(prefixes: List<String>, subhierarchies: List<String>) = shouldNotDetectPrefixTests(
    prefixes = prefixes,
    subhierarchies = subhierarchies,
    data = JvmBuildTarget(javaVersion = "17", resourceStripPrefix = null),
  )

  private fun shouldDetectKotlinPrefixTests(vararg paths: String) = shouldDetectPrefixTests(
    prefixes = paths.asList(),
    subhierarchies = listOf(""),
    data = KotlinBuildTarget(
      languageVersion = "1.8",
      apiVersion = "1.8",
      kotlincOptions = emptyList(),
      associates = emptyList(),
      jvmBuildTarget = JvmBuildTarget(
        javaVersion = "17",
        resourceStripPrefix = null,
      ),
    ),
  )

  private fun shouldNotDetectKotlinPrefixTests(vararg paths: String) = shouldNotDetectPrefixTests(
    prefixes = paths.asList(),
    subhierarchies = listOf(""),
    data = KotlinBuildTarget(
      languageVersion = "1.8",
      apiVersion = "1.8",
      kotlincOptions = emptyList(),
      associates = emptyList(),
      jvmBuildTarget = JvmBuildTarget(
        javaVersion = "17",
        resourceStripPrefix = null,
      ),
    ),
  )

  private fun shouldDetectScalaPrefixTests(vararg paths: String) = shouldDetectScalaPrefixTests(
    prefixes = paths.asList(),
    subhierarchies = listOf(""),
  )

  private fun shouldNotDetectScalaPrefixTests(vararg paths: String) = shouldNotDetectScalaPrefixTests(
    prefixes = paths.asList(),
    subhierarchies = listOf(""),
  )

  private fun shouldNotDetectScalaPrefixTests(prefixes: List<String>, subhierarchies: List<String>) = shouldNotDetectPrefixTests(
    prefixes = prefixes,
    subhierarchies = subhierarchies,
    data = ScalaBuildTarget(
      scalaVersion = "2.13",
      sdkJars = emptyList(),
      scalacOptions = emptyList(),
      jvmBuildTarget = JvmBuildTarget(
        javaVersion = "17",
        resourceStripPrefix = null,
      ),
    ),
  )

  private fun shouldDetectScalaPrefixTests(prefixes: List<String>, subhierarchies: List<String>) = shouldDetectPrefixTests(
    prefixes = prefixes,
    subhierarchies = subhierarchies,
    data = ScalaBuildTarget(
      scalaVersion = "2.13",
      sdkJars = emptyList(),
      scalacOptions = emptyList(),
      jvmBuildTarget = JvmBuildTarget(
        javaVersion = "17",
        resourceStripPrefix = null,
      ),
    ),
  )

  private fun shouldNotDetectPrefixTests(
    prefixes: List<String>,
    subhierarchies: List<String>,
    data: BuildTargetData?,
  ) = prefixes.flatMap { prefix ->
    subhierarchies.map { subhierarchy ->
      val displayName = when {
        subhierarchy.isEmpty() -> "prefix \'$prefix\'"
        else -> "prefix \'$prefix\' followed by \'$subhierarchy\'"
      }
      DynamicTest.dynamicTest(displayName) {
        shouldNotDetectPrefix(
          prefix = projectBasePath.resolve(prefix),
          subhierarchy = subhierarchy,
          data = data,
        )
      }
    }
  }

  private fun shouldDetectPrefixTests(
    prefixes: List<String>,
    subhierarchies: List<String>,
    data: BuildTargetData?,
  ) = prefixes.flatMap { prefix ->
    subhierarchies.map { subhierarchy ->
      val displayName = when {
        subhierarchy.isEmpty() -> "prefix \'$prefix\'"
        else -> "prefix \'$prefix\' followed by \'$subhierarchy\'"
      }
      DynamicTest.dynamicTest(displayName) {
        shouldDetectPrefix(
          prefix = projectBasePath.resolve(prefix),
          subhierarchy = subhierarchy,
          data = data,
        )
      }
    }
  }

  private fun shouldDetectPrefix(
    prefix: Path,
    subhierarchy: String,
    data: BuildTargetData?,
  ) {
    // given
    val subdirectories = prefix.resolve(subhierarchy).createDirectories()
    val resourceFile1 = subdirectories.resolve("config1.properties").createFile()
    val resourceFile2 = subdirectories.resolve("config2.properties").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFile1, resourceFile2), data = data)

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(resourcePath = prefix, rootType = SourceRootTypeId("java-resource"))
  }

  private fun shouldNotDetectPrefix(
    prefix: Path,
    subhierarchy: String,
    data: BuildTargetData?,
  ) {
    // given
    val resourcesDir = prefix
    val subdirectories = resourcesDir.resolve(subhierarchy).createDirectories()
    val resourceFile1 = subdirectories.resolve("config1.properties").createFile()
    val resourceFile2 = subdirectories.resolve("config2.properties").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFile1, resourceFile2), data = data)

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldNotContain ResourceRoot(resourcePath = prefix, rootType = SourceRootTypeId("java-resource"))
  }
}
