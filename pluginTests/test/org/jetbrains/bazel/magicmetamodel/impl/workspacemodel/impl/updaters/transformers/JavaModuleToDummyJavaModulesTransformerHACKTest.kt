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
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.name

class JavaModuleToDummyJavaModulesTransformerHACKTest : WorkspaceModelBaseTest() {
  @Test
  fun `should merge sources of module with sources in common root`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val irrelevantFilePath = createTempFile(projectRoot, "irrelevant", ".xml")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      dependencies = listOf(
        Dependency("module2"),
        Dependency("module3"),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
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
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

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
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA1Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kindString = "java_library",
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
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

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
    val projectRoot1 = createTempDirectory(projectBasePath, "module1")
    val projectRoot1Name = projectRoot1.name

    val projectRoot2 = createTempDirectory(projectBasePath, "module2")
    val projectRoot2Name = projectRoot2.name

    val givenJavaModule1 =
      createJavaModule(
        name = projectRoot1Name,
        type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
        kind = TargetKind(
          kindString = "java_library",
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
      name = projectRoot2Name,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
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
    val projectRoot = createTempDirectory(projectBasePath, "module")
    val projectRootName = projectRoot.name
    val filePath = createTempFile(projectRoot, "File", ".java")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
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
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind =
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

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
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")
    val packagePrefix = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
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
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

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
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = packageA2Path.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA2Path.toAbsolutePath(),
          generated = false,
          packagePrefix = packageA2Path.name,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should stop going up when directories stop matching package segments`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val packagePrefix = "org.example.${packageA2Path.name}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

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
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
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
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val file3APath = createTempFile(packageA2Path, "File2", ".java")

    val packagePrefixWithMoreVotes = "${packageA2Path.fileName}"
    val packagePrefixWithFewerVotes = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind =
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefixWithMoreVotes,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefixWithMoreVotes,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file3APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefixWithFewerVotes,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

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
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    createTempFile(packageA1Path, "File2", ".java")
    val packagePrefix = "${packageA1Path.name}.${packageA2Path.name}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA2Path.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  private fun transformIntoDummyModules(module: JavaModule, fileToTarget: Map<Path, List<Label>> = emptyMap()): List<JavaModule> =
    transformIntoDummyModules(listOf(module), fileToTarget)

  private fun transformIntoDummyModules(modules: List<JavaModule>, fileToTarget: Map<Path, List<Label>> = emptyMap()): List<JavaModule> =
    modules
      .flatMap { module ->
        val result = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTarget, project).transform(module)
        (result as JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd).dummyModules
      }.distinctBy { it.getModuleName() }
}
