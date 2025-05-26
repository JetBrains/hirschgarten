package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FindUsagesInGlobsTest : BasePlatformTestCase() {
  @Test
  fun `should find glob reference for single kotlin file with recursive pattern`() {
    val kotlinFile = myFixture.addFileToProject("src/Test.kt", "// test file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib",
          srcs = glob(["**/*.kt"])
      )
      """.trimIndent(),
    )

    val references = findUsagesOfElement(kotlinFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find glob reference for single kotlin file with specific pattern`() {
    val kotlinFile = myFixture.addFileToProject("Test.kt", "// test file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib", 
          srcs = glob(["*.kt"])
      )
      """.trimIndent(),
    )

    val references = findUsagesOfElement(kotlinFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find glob reference with wildcard pattern for kotlin`() {
    val kotlinFile = myFixture.addFileToProject("Test.kt", "// test file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib",
          srcs = glob(["T*t.kt"])
      )
      """.trimIndent(),
    )

    val references = findUsagesOfElement(kotlinFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find glob references for multiple kotlin files`() {
    val testFile = myFixture.addFileToProject("Test.kt", "// test file")
    val fooFile = myFixture.addFileToProject("Foo.kt", "// foo file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib",
          srcs = glob(["*.kt"])
      )
      """.trimIndent(),
    )

    val testReferences = findUsagesOfElement(testFile)
    val fooReferences = findUsagesOfElement(fooFile)

    assertThat(testReferences).isNotEmpty()
    assertThat(fooReferences).isNotEmpty()
    assertThat(testReferences).anyMatch { containsGlob(it.element.containingFile) }
    assertThat(fooReferences).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should find kotlin files in subdirectories with recursive glob`() {
    val kotlinFile = myFixture.addFileToProject("src/main/Test.kt", "// test file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib",
          srcs = glob(["**/*.kt"])
      )
      """.trimIndent(),
    )

    val references = findUsagesOfElement(kotlinFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }
  }

  @Test
  fun `should respect glob exclude patterns for kotlin files`() {
    val testFile = myFixture.addFileToProject("test/Test.kt", "// test file")
    val mainFile = myFixture.addFileToProject("Main.kt", "// main file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib",
          srcs = glob(
              ["**/*.kt"],
              exclude = ["test/*.kt"]
          )
      )
      """.trimIndent(),
    )

    val mainReferences = findUsagesOfElement(mainFile)
    val testReferences = findUsagesOfElement(testFile)

    // Main file should be included
    assertThat(mainReferences).anyMatch { containsGlob(it.element.containingFile) }

    // Test file should be excluded (this test verifies exclude functionality)
    val testHasGlobReference = testReferences.any { containsGlob(it.element.containingFile) }
    assertFalse("Test file should not have glob references", testHasGlobReference)
  }

  @Test
  fun `should find references from multiple globs targeting same kotlin file`() {
    val kotlinFile = myFixture.addFileToProject("Test.kt", "// test file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib",
          srcs = glob(["*.kt"])
      )
      
      kt_jvm_test(
          name = "test",
          srcs = glob(["**/*.kt"])  
      )
      """.trimIndent(),
    )

    val references = findUsagesOfElement(kotlinFile)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }

    // Verify that we get multiple references (one from each glob)
    val globReferences = references.filter { containsGlob(it.element.containingFile) }
    assertTrue("There should be 2 glob references", globReferences.count() == 2)
  }

  @Test
  fun `should find proper kotlin class file with matching name`() {
    val kotlinFile =
      myFixture.addFileToProject(
        "TestClassWithDefinition.kt",
        """
        package com.example
        
        class TestClassWithDefinition {
        }
        """.trimIndent(),
      )

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "services",
          srcs = glob(["*.kt"])
      )
      """.trimIndent(),
    )

    // In IntelliJ, when the kotlin file contains a proper class definition,
    // [Right Click] -> [Find Usages] on TestClassWithDefinition.kt is called with the KtClass type (not PsiFile).
    // This code checks if [Find Usages] works in this case as well (see KotlinPsiFileProvider extension point).
    val kotlinClass = kotlinFile.getChildOfType<KtClass>()!!
    val references = findUsagesOfElement(kotlinClass)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }

    // This should use extension point for proper class file handling
    val globReferences = references.filter { containsGlob(it.element.containingFile) }
    assertTrue(
      "Should find references through proper class file extension point",
      globReferences.isNotEmpty(),
    )
  }

  @Test
  fun `should find proper java class file with matching name`() {
    val javaFile =
      myFixture.addFileToProject(
        "TestClassWithDefinition.java",
        """
        package com.example;
        
        public class TestClassWithDefinition {
        }
        """.trimIndent(),
      )

    myFixture.addFileToProject(
      "BUILD",
      """
      java_library(
          name = "repositories",
          srcs = glob(["*.java"])
      )
      """.trimIndent(),
    )

    // In IntelliJ, when the java file contains a proper class definition,
    // [Right Click] -> [Find Usages] on TestClassWithDefinition.kt is called with the PsiClass type (not PsiFile).
    // This code checks if [Find Usages] works in this case as well (see JavaPsiFileProvider extension point).
    val javaClass = javaFile.getChildOfType<PsiClass>()!!
    val references = findUsagesOfElement(javaClass)

    assertThat(references).isNotEmpty()
    assertThat(references).anyMatch { containsGlob(it.element.containingFile) }

    // This should use extension point for proper class file handling
    val globReferences = references.filter { containsGlob(it.element.containingFile) }
    assertTrue(
      "Should find references through proper class file extension point",
      globReferences.isNotEmpty(),
    )
  }

  @Test
  fun `should handle directory references in globs`() {
    myFixture.addFileToProject("data/config.txt", "// config file")
    val dataDir =
      myFixture.findFileInTempDir("data")?.let {
        myFixture.psiManager.findDirectory(it)
      }
    checkNotNull(dataDir) { "Should find data directory" }

    myFixture.addFileToProject(
      "BUILD",
      """
      filegroup(
          name = "all_files",
          srcs = glob(["**/*"], exclude_directories=0)
      )
      """.trimIndent(),
    )

    val references = findUsagesOfElement(dataDir)
    assertThat(references).isNotEmpty()
  }

  @Test
  fun `should not find references when glob pattern doesn't match kotlin files`() {
    val pythonFile = myFixture.addFileToProject("script.py", "# python file")

    myFixture.addFileToProject(
      "BUILD",
      """
      kt_jvm_library(
          name = "lib",
          srcs = glob(["*.kt"])  # Only matches .kt files
      )
      """.trimIndent(),
    )

    val references = findUsagesOfElement(pythonFile)
    val hasGlobReference = references.any { containsGlob(it.element.containingFile) }

    assertFalse("Python file should not be referenced by Kotlin glob", hasGlobReference)
  }

  @Test
  fun `should find mixed java and kotlin files in same glob`() {
    val kotlinFile =
      myFixture.addFileToProject(
        "Service.kt",
        """
        class Service {
            fun process() = "processed"
        }
        """.trimIndent(),
      )

    val javaFile =
      myFixture.addFileToProject(
        "Helper.java",
        """
        public class Helper {
            public static String help() { return "help"; }
        }
        """.trimIndent(),
      )

    myFixture.addFileToProject(
      "BUILD",
      """
      java_library(
          name = "mixed",
          srcs = glob(["*.kt", "*.java"])
      )
      """.trimIndent(),
    )

    val kotlinReferences = findUsagesOfElement(kotlinFile)
    val javaReferences = findUsagesOfElement(javaFile)

    assertThat(kotlinReferences).isNotEmpty()
    assertThat(javaReferences).isNotEmpty()
    assertThat(kotlinReferences).anyMatch { containsGlob(it.element.containingFile) }
    assertThat(javaReferences).anyMatch { containsGlob(it.element.containingFile) }
  }

  private fun findUsagesOfElement(element: PsiElement): Collection<PsiReference> = ReferencesSearch.search(element).findAll()

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
