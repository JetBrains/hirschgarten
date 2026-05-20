package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

@RunWith(JUnit4::class)
internal class StarlarkGlobGlobbingTest : BasePlatformTestCase() {

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
        .execute()

    assertEquals(setOf("include1.java", "include2.java"), result.relativePathsFrom(baseDir))
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
        .execute()

    assertEquals(setOf("include.java"), result.relativePathsFrom(baseDir))
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
        .execute()

    assertEquals(setOf("include.kt"), result.relativePathsFrom(baseDir))
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
      .execute()

    assertEquals(setOf("root.kt", "dir1/file1.kt", "dir2/subdir/file2.kt"), result.relativePathsFrom(baseDir))
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
        .execute()

    val relPaths = result.relativePathsFrom(baseDir)
    assertEquals(setOf("file.kt"), relPaths)
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
        .execute()

    assertEquals(emptySet<String>(), result.relativePathsFrom(baseDir))
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
        .execute()

    assertEquals(
      setOf(
        "x/y.kt",
        "x/dir/y.kt",
        "x/dir/sub/y.kt",
      ),
      result.relativePathsFrom(baseDir),
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
        .execute()

    assertEquals(setOf("dir/include1.kt", "dir/sub/include2.kt"), result.relativePathsFrom(baseDir))
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
        .execute()

    assertEquals(setOf("include/visible.txt"), result.relativePathsFrom(baseDir))
  }

  private fun List<VirtualFile>.relativePathsFrom(base: VirtualFile): Set<String> {
    return map { Path(it.path).relativeTo(Path(base.path)).toString() }.toSet()
  }
}
