package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMapBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.importer.SourceRootBuilder.ResolvedSourceRoot
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

internal class DummyModuleSplitterTest : WorkspaceModelBaseTest() {
  @Test
  fun `should merge sources of module with sources in common root`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val packageA1 = moduleRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA2.resolve("File1.java").createFile()
    val file2 = packageA2.resolve("File2.java").createFile()
    val irrelevant = moduleRoot.resolve("irrelevant.xml").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "packageA2"),
        sourceRoot(file2, packagePrefix = "packageA2"),
        sourceRoot(irrelevant, packagePrefix = ""),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(
        sourceRoot(packageA1, packagePrefix = ""),
        sourceRoot(irrelevant, packagePrefix = ""),
      )
  }

  @Test
  fun `should merge sources for module with nested source roots`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val javaRoot = moduleRoot.resolve("src/main/java").createDirectories()
    val packageA1 = javaRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA1.resolve("File1.java").createFile()
    val file2 = packageA2.resolve("File2.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = ""),
        sourceRoot(file2, packagePrefix = ""),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(packageA1, packagePrefix = ""))
  }

  @Test
  fun `should return no dummies when sources already point at directories`() {
    val moduleRoot1 = projectBasePath.resolve("module1").createDirectories()
    val moduleRoot2 = projectBasePath.resolve("module2").createDirectories()

    val splitter = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY)
    val dummies = listOf(moduleRoot1, moduleRoot2).flatMap { root ->
      val result = splitter.split(
        baseDirectory = root.toAbsolutePath(),
        sourceRoots = listOf(sourceRoot(root, packagePrefix = "")),
      )
      (result as? DummyModuleSplitter.DummyModulesToAdd)?.dummies.orEmpty()
    }

    dummies shouldBe emptyList()
  }

  @Test
  fun `should not create dummy modules for generated source roots`() {
    val moduleRoot = projectBasePath.resolve("module").createDirectories()
    val file = moduleRoot.resolve("File.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(sourceRoot(file, packagePrefix = "", generated = true)),
    )

    (result as DummyModuleSplitter.DummyModulesToAdd).dummies shouldBe emptyList()
  }

  @Test
  fun `should merge test sources in common root`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val packageA1 = moduleRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA2.resolve("File1.java").createFile()
    val file2 = packageA2.resolve("File2.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "packageA2", rootType = JAVA_TEST_SOURCE_ROOT_TYPE),
        sourceRoot(file2, packagePrefix = "packageA2", rootType = JAVA_TEST_SOURCE_ROOT_TYPE),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(packageA1, packagePrefix = "", rootType = JAVA_TEST_SOURCE_ROOT_TYPE))
  }

  @Test
  fun `should prefer test root if test and production sources are together`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val javaRoot = moduleRoot.resolve("src/main/java").createDirectories()
    val packageA1 = javaRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA2.resolve("File1.java").createFile()
    val file2 = packageA2.resolve("File2.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "packageA1.packageA2", rootType = JAVA_TEST_SOURCE_ROOT_TYPE),
        sourceRoot(file2, packagePrefix = "packageA1.packageA2"),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(javaRoot, packagePrefix = "", rootType = JAVA_TEST_SOURCE_ROOT_TYPE))
  }

  @Test
  fun `should not go higher than the BUILD file (baseDirectory)`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val packageA1 = moduleRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA2.resolve("File1.java").createFile()
    val file2 = packageA2.resolve("File2.java").createFile()

    // The base directory is the package directory itself: the algorithm must not climb above it.
    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = packageA2.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "packageA2"),
        sourceRoot(file2, packagePrefix = "packageA2"),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(packageA2, packagePrefix = "packageA2"))
  }

  @Test
  fun `should stop going up when directories stop matching package segments`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val packageA1 = moduleRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file = packageA2.resolve("File1.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(sourceRoot(file, packagePrefix = "org.example.packageA2")),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(packageA1, packagePrefix = "org.example"))
  }

  @Test
  fun `should not merge sources if there are shared sources`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val packageA1 = moduleRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA2.resolve("File1.java").createFile()

    val fileToTargets = mapOf(
      file1 to listOf(WorkspaceTargetKey(Label.parse("//:target1")), WorkspaceTargetKey(Label.parse("//:target2"))),
    )

    val result = DummyModuleSplitter(projectBasePath, File2TargetMapBuilder.build(fileToTargets)).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(sourceRoot(file1, packagePrefix = "packageA2")),
    )

    (result as DummyModuleSplitter.DummyModulesToAdd).dummies.size shouldBe 1
  }

  @Test
  fun `should prefer source root that has more votes`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val javaRoot = moduleRoot.resolve("src/main/java").createDirectories()
    val packageA1 = javaRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA2.resolve("File1.java").createFile()
    val file2 = packageA2.resolve("File2.java").createFile()
    val file3 = packageA2.resolve("File3.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "packageA2"),
        sourceRoot(file2, packagePrefix = "packageA2"),
        sourceRoot(file3, packagePrefix = "packageA1.packageA2"),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(packageA1, packagePrefix = ""))
  }

  @Test
  fun `should fall back to parent directory when sibling JVM file blocks restoration`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val packageA1 = moduleRoot.resolve("packageA1").createDirectories()
    val packageA2 = packageA1.resolve("packageA2").createDirectories()
    val file1 = packageA2.resolve("File1.java").createFile()
    packageA1.resolve("File2.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(sourceRoot(file1, packagePrefix = "packageA1.packageA2")),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(packageA2, packagePrefix = "packageA1.packageA2"))
  }

  @Test
  fun `should prefer empty prefix when longer prefix extends it (BAZEL-3050)`() {
    val moduleRoot = projectBasePath.resolve("project").createDirectories()
    val fooPackage = moduleRoot.resolve("foo").createDirectories()
    val file1 = fooPackage.resolve("File1.java").createFile()
    val file2 = fooPackage.resolve("File2.kt").createFile()
    val file3 = fooPackage.resolve("File3.kt").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "foo"),
        sourceRoot(file2, packagePrefix = "com.example.foo"),
        sourceRoot(file3, packagePrefix = "com.example.foo"),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(moduleRoot, packagePrefix = ""))
  }

  @Test
  fun `should prefer shorter non-empty prefix when longer prefix extends it (BAZEL-3050)`() {
    val moduleRoot = projectBasePath.resolve("module1").createDirectories()
    val srcDir = moduleRoot.resolve("src").createDirectories()
    val file1 = srcDir.resolve("File1.kt").createFile()
    val file2 = srcDir.resolve("File2.kt").createFile()
    val file3 = srcDir.resolve("File3.java").createFile()

    val result = DummyModuleSplitter(projectBasePath, File2TargetMap.EMPTY).split(
      baseDirectory = moduleRoot.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "org.company.project"),
        sourceRoot(file2, packagePrefix = "org.company.project"),
        sourceRoot(file3, packagePrefix = "project"),
      ),
    )

    (result as DummyModuleSplitter.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      listOf(sourceRoot(srcDir, packagePrefix = "project"))
  }

  @Test
  fun `should stop at the exact level where siblings contain JVM files`() {
    val moduleRoot = projectBasePath.resolve("module").createDirectories()
    val org = moduleRoot.resolve("org").createDirectories()
    val example = org.resolve("example").createDirectories()
    val app = example.resolve("app").createDirectories()
    val file = app.resolve("File1.java").createFile()
    moduleRoot.resolve("Sibling.java").createFile() // sibling JVM file at the project root level

    val fileToTargets = mapOf(file to listOf(WorkspaceTargetKey(Label.parse("//:t1")), WorkspaceTargetKey(Label.parse("//:t2"))))

    val result = DummyModuleSplitter(projectBasePath, File2TargetMapBuilder.build(fileToTargets)).split(
      baseDirectory = example.toAbsolutePath(),
      sourceRoots = listOf(sourceRoot(file, packagePrefix = "org.example.app")),
    )

    val dummies = (result as DummyModuleSplitter.DummyModulesToAdd).dummies
    dummies.size shouldBe 1
    dummies[0].sourceRoot.sourcePath shouldBe org.toAbsolutePath()
    dummies[0].sourceRoot.packagePrefix shouldBe "org"
  }

  @Test
  fun `should go higher when siblings contain no JVM files`() {
    val moduleRoot = projectBasePath.resolve("module").createDirectories()
    val com = moduleRoot.resolve("com").createDirectories()
    val example = com.resolve("example").createDirectories()
    val file = example.resolve("File1.java").createFile()
    moduleRoot.resolve("readme.txt").createFile() // non-JVM sibling

    val fileToTargets = mapOf(file to listOf(WorkspaceTargetKey(Label.parse("//:t1")), WorkspaceTargetKey(Label.parse("//:t2"))))

    val result = DummyModuleSplitter(projectBasePath, File2TargetMapBuilder.build(fileToTargets)).split(
      baseDirectory = com.toAbsolutePath(),
      sourceRoots = listOf(sourceRoot(file, packagePrefix = "com.example")),
    )

    val dummies = (result as DummyModuleSplitter.DummyModulesToAdd).dummies
    dummies.size shouldBe 1
    dummies[0].sourceRoot.sourcePath shouldBe moduleRoot.toAbsolutePath()
    dummies[0].sourceRoot.packagePrefix shouldBe ""
  }

  @Test
  fun `should go higher past own source files in sibling directories`() {
    val moduleRoot = projectBasePath.resolve("module").createDirectories()
    val com = moduleRoot.resolve("com").createDirectories()
    val example = com.resolve("example").createDirectories()
    val file1 = example.resolve("File1.java").createFile()
    val file2 = com.resolve("File2.java").createFile()

    val fileToTargets = mapOf(
      file1 to listOf(WorkspaceTargetKey(Label.parse("//:t1")), WorkspaceTargetKey(Label.parse("//:t2"))),
      file2 to listOf(WorkspaceTargetKey(Label.parse("//:t1")), WorkspaceTargetKey(Label.parse("//:t2"))),
    )

    val result = DummyModuleSplitter(projectBasePath, File2TargetMapBuilder.build(fileToTargets)).split(
      baseDirectory = com.toAbsolutePath(),
      sourceRoots = listOf(
        sourceRoot(file1, packagePrefix = "com.example"),
        sourceRoot(file2, packagePrefix = "com"),
      ),
    )

    val dummies = (result as DummyModuleSplitter.DummyModulesToAdd).dummies
    dummies.size shouldBe 1
    dummies[0].sourceRoot.sourcePath shouldBe moduleRoot.toAbsolutePath()
    dummies[0].sourceRoot.packagePrefix shouldBe ""
  }

  private fun sourceRoot(
    path: Path,
    packagePrefix: String,
    rootType: SourceRootTypeId = JAVA_SOURCE_ROOT_TYPE,
    generated: Boolean = false,
  ): ResolvedSourceRoot = ResolvedSourceRoot(
    sourcePath = path.toAbsolutePath(),
    generated = generated,
    packagePrefix = packagePrefix,
    rootType = rootType,
  )
}
