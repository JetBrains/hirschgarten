package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.JavaModuleType.JAVA_MODULE_ENTITY_TYPE_ID_NAME
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createJavaModule
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class JavaModuleToDummyJavaModulesTransformerHACKTest : WorkspaceModelBaseTest() {
  @Test
  fun `should merge sources of module with sources in common root`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"

    val packageA1Path = projectRoot.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()
    val file2APath = packageA2Path.resolve("File2.java").createFile()
    val irrelevantFilePath = projectRoot.resolve("irrelevant.xml").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      dependencies = listOf(
        Dependency("module2"),
        Dependency("module3"),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = irrelevantFilePath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = irrelevantFilePath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should merge sources for module with nested source roots`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"

    val srcPath = projectRoot.resolve("src").createDirectories()
    val mainPath = srcPath.resolve("main").createDirectories()
    val javaPath = mainPath.resolve("java").createDirectories()
    val packageA1Path = javaPath.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA1Path.resolve("File1.java").createFile()
    val file2APath = packageA2Path.resolve("File2.java").createFile()

    val resourceFilePath = projectRoot.resolve("Resources.properties").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should return no dummy java module for module with sources in directories`() {
    // given
    val projectRoot1 = projectBasePath.resolve("module1").createDirectories()
    val projectRoot2 = projectBasePath.resolve("module2").createDirectories()

    val givenJavaModule1 =
      createJavaModule(
        name = "module1",
        type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
        kind = TargetKind(
          kind = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirContentRoot = ContentRoot(path = projectRoot1.toAbsolutePath()),
        sourceRoots = listOf(
          JavaSourceRoot(
            sourcePath = projectRoot1.toAbsolutePath(),
            generated = false,
            packagePrefix = "",
            rootType = JAVA_SOURCE_ROOT_TYPE,
          ),
        ),
      )

    val givenJavaModule2 = createJavaModule(
      name = "module2",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot2.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = projectRoot2.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
    )

    val modulesList = listOf(givenJavaModule1, givenJavaModule2)

    // when
    val javaModules = transformIntoDummyModules(modulesList)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should not create dummy modules for generated source roots`() {
    // given
    val projectRoot = projectBasePath.resolve("module").createDirectories()
    val filePath = projectRoot.resolve("File.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = filePath.toAbsolutePath(),
          generated = true,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
    )

    val modulesList = listOf(givenJavaModule)

    // when
    val javaModules = transformIntoDummyModules(modulesList)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should merge sources of module with test sources in common root`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"

    val packageA1Path = projectRoot.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()
    val file2APath = packageA2Path.resolve("File2.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind =
        TargetKind(
          kind = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should prefer test root if test and production sources are together`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"

    val srcPath = projectRoot.resolve("src").createDirectories()
    val mainPath = srcPath.resolve("main").createDirectories()
    val javaPath = mainPath.resolve("java").createDirectories()
    val packageA1Path = javaPath.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()
    val file2APath = packageA2Path.resolve("File2.java").createFile()

    val resourceFilePath = projectRoot.resolve("Resources.properties").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA1.packageA2",
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA1.packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = javaPath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should not go higher than the BUILD file`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"

    val packageA1Path = projectRoot.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()
    val file2APath = packageA2Path.resolve("File2.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = packageA2Path.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA2Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should stop going up when directories stop matching package segments`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"
    val packageA1Path = projectRoot.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "org.example.packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "org.example",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should not merge sources if there are shared sources`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"
    val packageA1Path = projectRoot.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    val fileToTarget =
      mapOf(
        file1APath to listOf(Label.parse("//:target1"), Label.parse("//:target2")),
      )

    // when
    val dummyModules = transformIntoDummyModules(givenJavaModule, fileToTarget)

    // then
    dummyModules.size shouldBe 1
  }

  @Test
  fun `should prefer source root that has more votes`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"
    val srcPath = projectRoot.resolve("src").createDirectories()
    val mainPath = srcPath.resolve("main").createDirectories()
    val javaPath = mainPath.resolve("java").createDirectories()
    val packageA1Path = javaPath.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()
    val file2APath = packageA2Path.resolve("File2.java").createFile()
    val file3APath = packageA2Path.resolve("File3.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind =
        TargetKind(
          kind = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file3APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA1.packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should fall back to parent directory if can't add package`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"
    val packageA1Path = projectRoot.resolve("packageA1").createDirectories()
    val packageA2Path = packageA1Path.resolve("packageA2").createDirectories()
    val file1APath = packageA2Path.resolve("File1.java").createFile()
    packageA1Path.resolve("File2.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kind = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA1.packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA2Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "packageA1.packageA2",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should prefer empty prefix when longer prefix extends it`() {
    // given
    val projectRoot = projectBasePath.resolve("project").createDirectories()
    val javaVersion = "11"
    val fooPackage = projectRoot.resolve("foo").createDirectories()
    val file1Path = fooPackage.resolve("File1.java").createFile()
    val file2Path = fooPackage.resolve("File2.kt").createFile()
    val file3Path = fooPackage.resolve("File3.kt").createFile()

    val givenJavaModule = createJavaModule(
      name = "project",
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(sourcePath = file1Path.toAbsolutePath(), generated = false, packagePrefix = "foo", rootType = JAVA_SOURCE_ROOT_TYPE),
        JavaSourceRoot(sourcePath = file2Path.toAbsolutePath(), generated = false, packagePrefix = "com.example.foo", rootType = JAVA_SOURCE_ROOT_TYPE),
        JavaSourceRoot(sourcePath = file3Path.toAbsolutePath(), generated = false, packagePrefix = "com.example.foo", rootType = JAVA_SOURCE_ROOT_TYPE),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = projectRoot.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should prefer shorter non-empty prefix when longer prefix extends it`() {
    // given
    val projectRoot = projectBasePath.resolve("module1").createDirectories()
    val javaVersion = "11"

    val srcPath = projectRoot.resolve("src").createDirectories()
    val file1 = srcPath.resolve("File1.kt").createFile()
    val file2 = srcPath.resolve("File2.kt").createFile()
    val file3 = srcPath.resolve("File3.java").createFile()

    val givenJavaModule = createJavaModule(
      name = "module1",
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(sourcePath = file1.toAbsolutePath(), generated = false, packagePrefix = "org.company.project", rootType = JAVA_SOURCE_ROOT_TYPE),
        JavaSourceRoot(sourcePath = file2.toAbsolutePath(), generated = false, packagePrefix = "org.company.project", rootType = JAVA_SOURCE_ROOT_TYPE),
        JavaSourceRoot(sourcePath = file3.toAbsolutePath(), generated = false, packagePrefix = "project", rootType = JAVA_SOURCE_ROOT_TYPE),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap()).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = srcPath.toAbsolutePath(),
          generated = false,
          packagePrefix = "project",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should stop at the exact level where siblings contain JVM files`() {
    // given
    val projectRoot = projectBasePath.resolve("module").createDirectories()
    val org = projectRoot.resolve("org").createDirectories()
    val example = org.resolve("example").createDirectories()
    val app = example.resolve("app").createDirectories()
    val file1 = app.resolve("File1.java").createFile()
    projectRoot.resolve("Sibling.java").createFile() // JVM sibling at projectRoot level

    val fileToTargets = mapOf(file1 to listOf(Label.parse("//:t1"), Label.parse("//:t2")))

    val givenJavaModule = createJavaModule(
      name = "module",
      baseDirContentRoot = ContentRoot(path = example.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1.toAbsolutePath(),
          generated = false,
          packagePrefix = "org.example.app",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
    )

    // when
    val result = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTargets).transform(givenJavaModule)

    // then
    val dummyModules = (result as JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd).dummyModules
    dummyModules.size shouldBe 1
    dummyModules[0].sourceRoots[0].sourcePath shouldBe org.toAbsolutePath()
    dummyModules[0].sourceRoots[0].packagePrefix shouldBe "org"
  }

  @Test
  fun `should go higher when siblings contain no JVM files`() {
    // given
    val projectRoot = projectBasePath.resolve("module").createDirectories()
    val com = projectRoot.resolve("com").createDirectories()
    val example = com.resolve("example").createDirectories()
    val file1 = example.resolve("File1.java").createFile()
    projectRoot.resolve("readme.txt").createFile() // non-JVM sibling

    val fileToTargets = mapOf(file1 to listOf(Label.parse("//:t1"), Label.parse("//:t2")))

    val givenJavaModule = createJavaModule(
      name = "module",
      baseDirContentRoot = ContentRoot(path = com.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1.toAbsolutePath(),
          generated = false,
          packagePrefix = "com.example",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
    )

    // when
    val result = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTargets).transform(givenJavaModule)

    // then
    val dummyModules = (result as JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd).dummyModules
    dummyModules.size shouldBe 1
    dummyModules[0].sourceRoots[0].sourcePath shouldBe projectRoot.toAbsolutePath()
    dummyModules[0].sourceRoots[0].packagePrefix shouldBe ""
  }

  @Test
  fun `should go higher past own source files in sibling directories`() {
    // given
    val projectRoot = projectBasePath.resolve("module").createDirectories()
    val com = projectRoot.resolve("com").createDirectories()
    val example = com.resolve("example").createDirectories()
    val file1 = example.resolve("File1.java").createFile()
    val file2 = com.resolve("File2.java").createFile()

    val fileToTargets = mapOf(
      file1 to listOf(Label.parse("//:t1"), Label.parse("//:t2")),
      file2 to listOf(Label.parse("//:t1"), Label.parse("//:t2")),
    )

    val givenJavaModule = createJavaModule(
      name = "module",
      baseDirContentRoot = ContentRoot(path = com.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1.toAbsolutePath(),
          generated = false,
          packagePrefix = "com.example",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2.toAbsolutePath(),
          generated = false,
          packagePrefix = "com",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
    )

    // when
    val result = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTargets).transform(givenJavaModule)

    // then
    val dummyModules = (result as JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd).dummyModules
    dummyModules.size shouldBe 1
    dummyModules[0].sourceRoots[0].sourcePath shouldBe projectRoot.toAbsolutePath()
    dummyModules[0].sourceRoots[0].packagePrefix shouldBe ""
  }

  private fun transformIntoDummyModules(module: JavaModule, fileToTarget: Map<Path, List<Label>> = emptyMap()): List<JavaModule> =
    transformIntoDummyModules(listOf(module), fileToTarget)

  private fun transformIntoDummyModules(modules: List<JavaModule>, fileToTarget: Map<Path, List<Label>> = emptyMap()): List<JavaModule> =
    modules
      .flatMap { module ->
        val result = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTarget).transform(module)
        (result as JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd).dummyModules
      }.distinctBy { it.getModuleName() }
}
