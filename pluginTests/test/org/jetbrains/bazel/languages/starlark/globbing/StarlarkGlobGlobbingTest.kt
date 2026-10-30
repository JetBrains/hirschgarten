package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo

@BazelTestApplication
internal class StarlarkGlobGlobbingTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should match simple pattern`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globSimple")

    myFixture.addFileToProject("globSimple/include1.java", "")
    myFixture.addFileToProject("globSimple/include2.java", "")
    myFixture.addFileToProject("globSimple/other.kt", "")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("*.java"))
        .build()
        .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe setOf("include1.java", "include2.java")
  }

  @Test
  fun `should respect excluding exact file`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globExcludes")

    myFixture.addFileToProject("globExcludes/include.java", "")
    myFixture.addFileToProject("globExcludes/exclude.java", "")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("*.java"))
        .addExcludes(listOf("exclude.java"))
        .build()
        .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe setOf("include.java")
  }

  @Test
  fun `should respect excluding by pattern`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globExcludes")

    myFixture.addFileToProject("globExcludes/include.kt", "")
    myFixture.addFileToProject("globExcludes/exclude.java", "")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("*"))
        .addExcludes(listOf("*.java"))
        .build()
        .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe setOf("include.kt")
  }

  @Test
  fun `should match recursive glob`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globRecursive")

    myFixture.addFileToProject("globRecursive/root.kt", "")
    myFixture.addFileToProject("globRecursive/dir1/file1.kt", "")
    myFixture.addFileToProject("globRecursive/dir2/subdir/file2.kt", "")

    val result = StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("**/*.kt"))
      .build()
      .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe setOf("root.kt", "dir1/file1.kt", "dir2/subdir/file2.kt")
  }

  @Test
  fun `should respect ExcludeDirectories flag`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globExcludeDirs")

    myFixture.addFileToProject("globExcludeDirs/file.kt", "")
    myFixture.tempDirFixture.findOrCreateDir("globExcludeDirs/subdir")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("**"))
        .setExcludeDirectories(true)
        .build()
        .executeWithProgress()

    val relPaths = result.relativePathsFrom(baseDir)
    relPaths shouldBe setOf("file.kt")
  }


  @Test
  fun `should be empty when no patterns provided`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globExcludeDirs")

    myFixture.addFileToProject("globExcludeDirs/file1.kt", "")
    myFixture.addFileToProject("globExcludeDirs/subdir/file2.kt", "")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .build()
        .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe emptySet()
  }

  @Test
  fun `double star can match empty path segments`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globDoubleStarEmpty")

    myFixture.addFileToProject("globDoubleStarEmpty/x/y.kt", "")
    myFixture.addFileToProject("globDoubleStarEmpty/x/dir/y.kt", "")
    myFixture.addFileToProject("globDoubleStarEmpty/x/dir/sub/y.kt", "")
    myFixture.addFileToProject("globDoubleStarEmpty/other.kt", "")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("x/**/y.kt"))
        .build()
        .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe
      setOf(
        "x/y.kt",
        "x/dir/y.kt",
        "x/dir/sub/y.kt",
      )
  }

  @Test
  fun `should exclude recursive subdirectory`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globExcludeRecursive")

    myFixture.addFileToProject("globExcludeRecursive/dir/include1.kt", "")
    myFixture.addFileToProject("globExcludeRecursive/dir/sub/include2.kt", "")
    myFixture.addFileToProject("globExcludeRecursive/dir/exclude/file2.kt", "")
    myFixture.addFileToProject("globExcludeRecursive/dir/exclude/sub/file2.kt", "")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("**/*.kt"))
        .addExcludes(listOf("dir/exclude/**"))
        .build()
        .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe setOf("dir/include1.kt", "dir/sub/include2.kt")
  }

  @Test
  fun `should respect directory filter`() {
    val baseDir = myFixture.tempDirFixture.findOrCreateDir("globDirFilter")

    myFixture.addFileToProject("globDirFilter/skipDir/notVisible.txt", "")
    myFixture.addFileToProject("globDirFilter/include/visible.txt", "")

    val result =
      StarlarkGlob
        .forPath(baseDir)
        .addPatterns(listOf("**/*.txt"))
        .setDirectoryFilter { dir -> dir.name != "skipDir" }
        .build()
        .executeWithProgress()

    result.relativePathsFrom(baseDir) shouldBe setOf("include/visible.txt")
  }

  /** A JUnit 5 test runs off the EDT, so [StarlarkGlob.execute] needs a progress indicator, as in the background highlighting. */
  private fun StarlarkGlob.executeWithProgress(): List<VirtualFile> =
    ProgressManager.getInstance().runProcess<List<VirtualFile>>({ execute() }, EmptyProgressIndicator())

  private fun List<VirtualFile>.relativePathsFrom(base: VirtualFile): Set<String> {
    return map { Path(it.path).relativeTo(Path(base.path)).invariantSeparatorsPathString }.toSet()
  }
}
