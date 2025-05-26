package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobFindUsagesTest : BasePlatformTestCase() {
  @Test
  fun `should find glob reference for single file with recursive pattern`() {
    val javaFile = myFixture.addFileToProject("src/Test.java", "// test file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["**/*.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find glob reference for single file with specific pattern`() {
    val javaFile = myFixture.addFileToProject("Test.java", "// test file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib", 
                srcs = glob(["*.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find glob reference with wildcard pattern`() {
    val javaFile = myFixture.addFileToProject("Test.java", "// test file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["T*t.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find glob references for multiple files`() {
    val testFile = myFixture.addFileToProject("Test.java", "// test file")
    val fooFile = myFixture.addFileToProject("Foo.java", "// foo file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["*.java"])
            )
        """.trimIndent())

    val testReferences = findUsagesOfElement(testFile)
    val fooReferences = findUsagesOfElement(fooFile)

    assertThat(testReferences).isNotEmpty()
    assertThat(fooReferences).isNotEmpty()
    assertThat(testReferences).anyMatch { containsGlob(it.element.containingFile) }
    assertThat(fooReferences).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find files in subdirectories with recursive glob`() {
    val javaFile = myFixture.addFileToProject("src/main/Test.java", "// test file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["**/*.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should respect glob exclude patterns`() {
    val testFile = myFixture.addFileToProject("test/Test.java", "// test file")
    val mainFile = myFixture.addFileToProject("Main.java", "// main file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib",
                srcs = glob(
                    ["**/*.java"],
                    exclude = ["test/*.java"]
                )
            )
        """.trimIndent())

    val mainReferences = findUsagesOfElement(mainFile)
    val testReferences = findUsagesOfElement(testFile)

    // Main file should be included
    assertThat(mainReferences).anyMatch { containsGlob(it.element.containingFile) }

    // Test file should be excluded (this test verifies exclude functionality)
    val testHasGlobReference = testReferences.any { containsGlob(it.element.containingFile) }
    assertFalse("Test file should not have glob references", testHasGlobReference)
  }

  @Test
  fun `should find references from multiple globs targeting same file`() {
    val javaFile = myFixture.addFileToProject("Test.java", "// test file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["*.java"])
            )
            
            java_test(
                name = "test",
                srcs = glob(["**/*.java"])  
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }

    // Verify that we get multiple references (one from each glob)
    val globReferences = references.filter { containsGlob(it.element.containingFile) }
    assertTrue("There should be 2 glob references", globReferences.count() == 2)
  }

  @Test
  fun `should handle directory references in globs`() {
    myFixture.addFileToProject("data/config.txt", "// config file")
    val dataDir = myFixture.findFileInTempDir("data")?.let {
      myFixture.psiManager.findDirectory(it)
    }
    checkNotNull(dataDir) { "Should find data directory" }

    myFixture.addFileToProject("BUILD", """
            filegroup(
                name = "all_files",
                srcs = glob(["**/*"], exclude_directories=0)
            )
        """.trimIndent())

    val references = findUsagesOfElement(dataDir)
    assertThat(references).isNotEmpty()
  }

  @Test
  fun `should not find references when glob pattern doesn't match`() {
    val pythonFile = myFixture.addFileToProject("script.py", "# python file")

    myFixture.addFileToProject("BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["*.java"])  # Only matches .java files
            )
        """.trimIndent())

    val references = findUsagesOfElement(pythonFile)
    val hasGlobReference = references.any { containsGlob(it.element.containingFile) }

    assertFalse("Python file should not be referenced by Java glob", hasGlobReference)
  }

  private fun findUsagesOfElement(element: PsiFile): Collection<PsiReference> =
    ReferencesSearch.search(element).findAll()

  private fun findUsagesOfElement(element: PsiDirectory): Collection<PsiReference> =
    ReferencesSearch.search(element).findAll()

  private fun containsGlob(file: PsiFile): Boolean = file.text.contains("glob(")

  // Helper assertion methods for better readability
  private fun assertThat(references: Collection<PsiReference>) = ReferenceAssertions(references)

  private class ReferenceAssertions(private val references: Collection<PsiReference>) {
    fun isNotEmpty(): ReferenceAssertions {
      assertTrue("Should find at least one reference", references.isNotEmpty())
      return this
    }

    fun anyMatch(predicate: (PsiReference) -> Boolean): ReferenceAssertions {
      assertTrue("Should have at least one matching reference", references.any(predicate))
      return this
    }
  }
}
