package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMapBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.importer.SourceRootBuilder.ResolvedSourceRoot
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories

internal class JavaSourceRootMergerTest : WorkspaceModelBaseTest() {
  private val packageRoot: Path
    get() = projectBasePath.resolve("pkg")

  private val generatedRoot: Path
    get() = projectBasePath.resolve("bazel-out/bin/pkg")

  @Test
  fun `should merge sources into the deepest common directory`() {
    val file1 = file("pkg/src/main/java/a/File1.java")
    val file2 = file("pkg/src/main/java/b/File2.kt")

    merge(sourceRoot(file1), sourceRoot(file2))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src/main/java")))
  }

  @Test
  fun `should merge a single source into its parent directory`() {
    val file = file("pkg/src/main/java/com/example/File1.java")

    merge(sourceRoot(file))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src/main/java/com/example")))
  }

  @Test
  fun `should merge nested sources into the directory of the outer one`() {
    val file1 = file("pkg/src/a/File1.java")
    val file2 = file("pkg/src/a/b/File2.java")

    merge(sourceRoot(file1), sourceRoot(file2))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src/a")))
  }

  @Test
  fun `should split by subdirectories when the common directory contains a foreign source`() {
    val file1 = file("pkg/src/a/File1.java")
    val file2 = file("pkg/src/b/File2.java")
    file("pkg/src/Foreign.java")

    merge(sourceRoot(file1), sourceRoot(file2))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src/a")), sourceRoot(packageRoot.resolve("src/b")))
  }

  @Test
  fun `should split by subdirectories when the common directory contains a resource of the target`() {
    val file1 = file("pkg/src/main/java/a/File1.java")
    val file2 = file("pkg/src/main/java/b/File2.java")
    val resource = file("pkg/src/main/java/messages/Bundle.properties")

    merge(sourceRoot(file1), sourceRoot(file2), resources = listOf(resource))
      .shouldContainExactlyInAnyOrder(
        sourceRoot(packageRoot.resolve("src/main/java/a")),
        sourceRoot(packageRoot.resolve("src/main/java/b")),
      )
  }

  @Test
  fun `should keep a source next to a resource of the target`() {
    val file = file("pkg/src/main/kotlin/com/example/Module.kt")
    val resource = file("pkg/src/main/kotlin/com/example/Adjacent.properties")

    merge(sourceRoot(file), resources = listOf(resource))
      .shouldContainExactlyInAnyOrder(sourceRoot(file))
  }

  @Test
  fun `should ignore resources of the target outside of the common directory`() {
    val file1 = file("pkg/src/main/java/a/File1.java")
    val file2 = file("pkg/src/main/java/b/File2.java")
    val resource = file("pkg/src/main/resources/messages/Bundle.properties")

    merge(sourceRoot(file1), sourceRoot(file2), resources = listOf(resource))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src/main/java")))
  }

  @Test
  fun `should not merge generated sources into a directory with a resource of the target`() {
    val genFile = file("bazel-out/bin/pkg/a/Gen1.kt")
    val resource = file("bazel-out/bin/pkg/a/generated.properties")

    merge(sourceRoot(genFile, generated = true), resources = listOf(resource))
      .shouldContainExactlyInAnyOrder(sourceRoot(genFile, generated = true))
  }

  @Test
  fun `should not merge sources located directly in the package root`() {
    val file1 = file("pkg/File1.java")
    val file2 = file("pkg/File2.java")

    merge(sourceRoot(file1), sourceRoot(file2))
      .shouldContainExactlyInAnyOrder(sourceRoot(file1), sourceRoot(file2))
  }

  @Test
  fun `should merge nested sources and keep sources located directly in the package root`() {
    val file1 = file("pkg/File1.java")
    val file2 = file("pkg/sub/File2.java")
    val file3 = file("pkg/sub/File3.java")

    merge(sourceRoot(file1), sourceRoot(file2), sourceRoot(file3))
      .shouldContainExactlyInAnyOrder(sourceRoot(file1), sourceRoot(packageRoot.resolve("sub")))
  }

  @Test
  fun `should never merge sources above the package root`() {
    val subPackageRoot = packageRoot.resolve("sub")
    val file = file("pkg/sub/a/File1.java")

    merge(sourceRoot(file), baseDirectory = subPackageRoot)
      .shouldContainExactlyInAnyOrder(sourceRoot(subPackageRoot.resolve("a")))
  }

  @Test
  fun `should keep sources from another package and merge the rest`() {
    val foreignPackageFile = file("other/src/File1.java")
    val file2 = file("pkg/src/File2.java")

    merge(sourceRoot(foreignPackageFile), sourceRoot(file2))
      .shouldContainExactlyInAnyOrder(sourceRoot(foreignPackageFile), sourceRoot(packageRoot.resolve("src")))
  }

  @Test
  fun `should merge sources outside of the project`(@TempDir outsideDir: Path) {
    val outsideFile = outsideDir.resolve("pkg/src/File1.java").createParentDirectories().createFile()

    merge(sourceRoot(outsideFile), baseDirectory = outsideDir.resolve("pkg"))
      .shouldContainExactlyInAnyOrder(sourceRoot(outsideDir.resolve("pkg/src")))
  }

  @Test
  fun `should stop below the directory that contains a foreign source`() {
    val file = file("pkg/src/a/File1.java")
    file("pkg/src/Foreign.java")

    merge(sourceRoot(file))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src/a")))
  }

  @Test
  fun `should consider foreign sources in nested directories`() {
    val file1 = file("pkg/src/a/File1.java")
    val file2 = file("pkg/src/b/File2.java")
    file("pkg/src/b/c/Foreign.kt")

    merge(sourceRoot(file1), sourceRoot(file2))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src/a")), sourceRoot(file2))
  }

  @Test
  fun `should ignore foreign non-JVM files`() {
    val file = file("pkg/src/File1.java")
    file("pkg/src/BUILD")
    file("pkg/src/README.md")

    merge(sourceRoot(file))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src")))
  }

  @Test
  fun `should not merge shared sources and their directories`() {
    val sharedFile = file("pkg/a/Shared.java")
    val file2 = file("pkg/a/File2.java")
    val file3 = file("pkg/b/File3.java")
    val fileToTargets = File2TargetMapBuilder.build(mapOf(sharedFile to listOf(targetKey("//pkg:t1"), targetKey("//pkg:t2"))))

    merge(sourceRoot(sharedFile), sourceRoot(file2), sourceRoot(file3), fileToTargets = fileToTargets)
      .shouldContainExactlyInAnyOrder(sourceRoot(sharedFile), sourceRoot(file2), sourceRoot(packageRoot.resolve("b")))
  }

  @Test
  fun `should not consider a source shared when it belongs to the same target several times`() {
    val file = file("pkg/src/File1.java")
    val fileToTargets = File2TargetMapBuilder.build(mapOf(file to listOf(targetKey("//pkg:t1"), targetKey("//pkg:t1"))))

    merge(sourceRoot(file), fileToTargets = fileToTargets)
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src")))
  }

  @Test
  fun `should merge generated sources into their parent directory`() {
    val genFile1 = file("bazel-out/bin/pkg/a/Gen1.kt")
    val genFile2 = file("bazel-out/bin/pkg/a/Gen2.java")

    merge(sourceRoot(genFile1, generated = true), sourceRoot(genFile2, generated = true))
      .shouldContainExactlyInAnyOrder(sourceRoot(generatedRoot.resolve("a"), generated = true))
  }

  @Test
  fun `should not merge generated sources more than one level up`() {
    val genFile1 = file("bazel-out/bin/pkg/a/Gen1.kt")
    val genFile2 = file("bazel-out/bin/pkg/a/nested/Gen2.kt")

    merge(sourceRoot(genFile1, generated = true), sourceRoot(genFile2, generated = true))
      .shouldContainExactlyInAnyOrder(
        sourceRoot(generatedRoot.resolve("a"), generated = true),
        sourceRoot(generatedRoot.resolve("a/nested"), generated = true),
      )
  }

  @Test
  fun `should keep generated sources next to foreign generated source and still merge real sources`() {
    val file = file("pkg/src/File1.java")
    val genFile = file("bazel-out/bin/pkg/a/Gen1.kt")
    file("bazel-out/bin/pkg/a/Foreign.java")

    merge(sourceRoot(file), sourceRoot(genFile, generated = true))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src")), sourceRoot(genFile, generated = true))
  }

  @Test
  fun `should merge generated sources even when real sources can't be merged`() {
    val sharedFile = file("pkg/src/Shared.java")
    val genFile1 = file("bazel-out/bin/pkg/Gen1.kt")
    val genFile2 = file("bazel-out/bin/pkg/Gen2.kt")
    val fileToTargets = File2TargetMapBuilder.build(mapOf(sharedFile to listOf(targetKey("//pkg:t1"), targetKey("//pkg:t2"))))

    merge(
      sourceRoot(sharedFile),
      sourceRoot(genFile1, generated = true),
      sourceRoot(genFile2, generated = true),
      fileToTargets = fileToTargets,
    ).shouldContainExactlyInAnyOrder(sourceRoot(sharedFile), sourceRoot(generatedRoot, generated = true))
  }

  @Test
  fun `should keep directory and non-JVM source roots as is`() {
    val file = file("pkg/src/File1.java")
    val directory = projectBasePath.resolve("pkg/srcdir").createDirectories()
    val srcJar = file("bazel-out/bin/pkg/ksp.srcjar")
    val resource = file("pkg/src/resource.xml")

    merge(sourceRoot(file), sourceRoot(directory), sourceRoot(srcJar, generated = true), sourceRoot(resource))
      .shouldContainExactlyInAnyOrder(
        sourceRoot(packageRoot.resolve("src")),
        sourceRoot(directory),
        sourceRoot(srcJar, generated = true),
        sourceRoot(resource),
      )
  }

  @Test
  fun `should merge test sources into test root`() {
    val file1 = file("pkg/src/File1.java")
    val file2 = file("pkg/src/File2.java")

    merge(sourceRoot(file1, rootType = JAVA_TEST_SOURCE_ROOT_TYPE), sourceRoot(file2, rootType = JAVA_TEST_SOURCE_ROOT_TYPE))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src"), rootType = JAVA_TEST_SOURCE_ROOT_TYPE))
  }

  @Test
  fun `should prefer test root type on tie`() {
    val file1 = file("pkg/src/File1.java")
    val file2 = file("pkg/src/File2.java")

    merge(sourceRoot(file1, rootType = JAVA_TEST_SOURCE_ROOT_TYPE), sourceRoot(file2))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src"), rootType = JAVA_TEST_SOURCE_ROOT_TYPE))
  }

  @Test
  fun `should prefer the root type of the majority of sources`() {
    val file1 = file("pkg/src/File1.java")
    val file2 = file("pkg/src/File2.java")
    val file3 = file("pkg/src/File3.java")

    merge(sourceRoot(file1, rootType = JAVA_TEST_SOURCE_ROOT_TYPE), sourceRoot(file2), sourceRoot(file3))
      .shouldContainExactlyInAnyOrder(sourceRoot(packageRoot.resolve("src")))
  }

  @Test
  fun `should not merge anything when merging is disabled`() {
    Registry.get(BazelFeatureFlags.MERGE_SOURCE_ROOTS).setValue(false, disposable)
    val file1 = file("pkg/src/File1.java")
    val genFile = file("bazel-out/bin/pkg/Gen1.kt")

    merge(sourceRoot(file1), sourceRoot(genFile, generated = true))
      .shouldContainExactlyInAnyOrder(sourceRoot(file1), sourceRoot(genFile, generated = true))
  }

  private fun merge(
    vararg sourceRoots: ResolvedSourceRoot,
    baseDirectory: Path = packageRoot,
    fileToTargets: FileToTargetMap = FileToTargetMap.EMPTY,
    resources: List<Path> = emptyList(),
  ): List<ResolvedSourceRoot> =
    JavaSourceRootMerger(fileToTargets).merge(baseDirectory, sourceRoots.toList(), resources)

  private fun file(relativePath: String): Path =
    projectBasePath.resolve(relativePath).createParentDirectories().createFile()

  private fun targetKey(label: String): WorkspaceTargetKey = WorkspaceTargetKey(Label.parse(label))

  private fun sourceRoot(
    path: Path,
    rootType: SourceRootTypeId = JAVA_SOURCE_ROOT_TYPE,
    generated: Boolean = false,
  ): ResolvedSourceRoot = ResolvedSourceRoot(
    sourcePath = path,
    generated = generated,
    rootType = rootType,
  )
}
