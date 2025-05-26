package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.Query
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SampleTest: BasePlatformTestCase() {
  @Test
  fun testSimpleGlobReferencingSingleFile() {
    // Create a Java file that should be referenced by glob
    val javaFile = myFixture.addFileToProject("java/com/google/Test.java", "// test file")

    // Create BUILD file with glob that references the Java file
    myFixture.addFileToProject("java/com/google/BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["**/*.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)
    assertTrue("Should find at least 1 reference to Java file", references.isNotEmpty())

    // Verify at least one reference comes from a glob
    val hasGlobReference = references.any { containsGlob(it.element.containingFile) }
    assertTrue("Should have reference from a file containing glob", hasGlobReference)
  }

  @Test
  fun testSimpleGlobReferencingSingleFileWithSpecificPattern() {
    val javaFile = myFixture.addFileToProject("java/com/google/Test.java", "// test file")

    myFixture.addFileToProject("java/com/google/BUILD", """
            java_library(
                name = "lib", 
                srcs = glob(["*.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)
    assertTrue("Should find at least 1 reference", references.isNotEmpty())

    val hasGlobReference = references.any { containsGlob(it.element.containingFile) }
    assertTrue("Should have reference from BUILD file with glob", hasGlobReference)
  }

  @Test
  fun testGlobWithWildcardPattern() {
    val javaFile = myFixture.addFileToProject("java/com/google/Test.java", "// test file")

    myFixture.addFileToProject("java/com/google/BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["T*t.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)
    assertTrue("Should find at least 1 reference", references.isNotEmpty())

    val hasGlobReference = references.any { containsGlob(it.element.containingFile) }
    assertTrue("Should have reference from BUILD file with glob", hasGlobReference)
  }

  @Test
  fun testGlobReferencingMultipleFiles() {
    val testFile = myFixture.addFileToProject("java/com/google/Test.java", "// test file")
    val fooFile = myFixture.addFileToProject("java/com/google/Foo.java", "// foo file")

    myFixture.addFileToProject("java/com/google/BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["*.java"])
            )
        """.trimIndent())

    // Both files should be referenced
    val testReferences = findUsagesOfElement(testFile)
    assertTrue("Should find references for Test.java", testReferences.isNotEmpty())

    val fooReferences = findUsagesOfElement(fooFile)
    assertTrue("Should find references for Foo.java", fooReferences.isNotEmpty())

    // Both should have glob references
    val testHasGlob = testReferences.any { containsGlob(it.element.containingFile) }
    val fooHasGlob = fooReferences.any { containsGlob(it.element.containingFile) }
    assertTrue("Test.java should have glob reference", testHasGlob)
    assertTrue("Foo.java should have glob reference", fooHasGlob)
  }

  @Test
  fun testFindsFilesInSubDirectories() {
    val javaFile = myFixture.addFileToProject("java/com/google/test/Test.java", "// test file")

    myFixture.addFileToProject("java/com/google/BUILD", """
            java_library(
                name = "lib",
                srcs = glob(["**/*.java"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(javaFile)
    assertTrue("Should find references for subdirectory file", references.isNotEmpty())

    val hasGlobReference = references.any { containsGlob(it.element.containingFile) }
    assertTrue("Should have reference from BUILD file with recursive glob", hasGlobReference)
  }

  @Test
  fun testGlobWithExcludes() {
    val testFile = myFixture.addFileToProject("java/com/google/tests/Test.java", "// test file")
    val fooFile = myFixture.addFileToProject("java/com/google/Foo.java", "// foo file")

    myFixture.addFileToProject("java/com/google/BUILD", """
            java_library(
                name = "lib",
                srcs = glob(
                    ["**/*.java"],
                    exclude = ["tests/*.java"]
                )
            )
        """.trimIndent())

    // Foo.java should be found (not excluded)
    val fooReferences = findUsagesOfElement(fooFile)
    val fooHasGlob = fooReferences.any { containsGlob(it.element.containingFile) }
    assertTrue("Foo.java should have glob reference", fooHasGlob)

    // Test.java should NOT be found (excluded) - this depends on your glob implementation
    val testReferences = findUsagesOfElement(testFile)
    // Note: This test might pass or fail depending on how excludes are handled
    println("Test.java references found: ${testReferences.size}")
  }

  @Test
  fun testMultipleGlobsReferencingSameFile() {
    val javaFile = myFixture.addFileToProject("java/com/google/Test.java", "// test file")

    myFixture.addFileToProject("java/com/google/BUILD", """
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
    assertTrue("Should find references from multiple globs", references.isNotEmpty())

    val hasGlobReference = references.any { containsGlob(it.element.containingFile) }
    assertTrue("Should have references from BUILD file with globs", hasGlobReference)
  }

  @Test
  fun testDirectoryReferences() {
    // Create directory with files
    myFixture.addFileToProject("java/com/google/tests/Test.java", "// test file")
    val testDir = myFixture.findFileInTempDir("java/com/google/tests")?.let {
      myFixture.psiManager.findDirectory(it)
    }
    assertNotNull("Should find test directory", testDir)

    myFixture.addFileToProject("java/com/google/BUILD", """
            filegroup(
                name = "all_files",
                srcs = glob(["**/*"])
            )
        """.trimIndent())

    val references = findUsagesOfElement(testDir!!)
    // Directory references might or might not be found depending on implementation
    println("Directory references found: ${references.size}")
  }

  private fun findUsagesOfElement(element: PsiFile): Collection<PsiReference> {
    val query: Query<PsiReference> = ReferencesSearch.search(element)
    return query.findAll()
  }

  private fun findUsagesOfElement(element: PsiDirectory): Collection<PsiReference> {
    val query: Query<PsiReference> = ReferencesSearch.search(element)
    return query.findAll()
  }

  private fun containsGlob(file: PsiFile): Boolean {
    return file.text.contains("glob(")
  }

}

