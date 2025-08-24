package org.jetbrains.bazel.symbols

import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression

/**
 * Tests for safe delete functionality of Bazel targets
 */
class BazelTargetSafeDeleteTest : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    myFixture.addFileToProject("WORKSPACE", "")
    myFixture.addFileToProject("MODULE.bazel", "")
  }

  fun testSafeDeleteUnusedTarget() {
    // Create a target that is not referenced anywhere
    val buildFile = myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "unused_lib",
          srcs = ["UnusedLib.java"],
      )
      
      java_library(
          name = "used_lib",
          srcs = ["UsedLib.java"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    
    // Find the unused target element
    val unusedTargetElement = createBazelTargetElement("unused_lib")
    assertNotNull("Should find unused target", unusedTargetElement)
    
    // Check that it's safe to delete
    assertTrue("Unused target should be safe to delete", 
               canSafelyDeleteTarget(unusedTargetElement!!))
    
    // Perform safe delete
    val initialContent = buildFile.text
    unusedTargetElement.delete()
    
    // Verify target was removed
    val updatedContent = myFixture.file.text
    assertFalse("Unused target should be removed", 
                updatedContent.contains("unused_lib"))
    assertTrue("Used target should remain", 
               updatedContent.contains("used_lib"))
  }

  fun testSafeDeleteTargetWithLocalReferences() {
    val buildFile = myFixture.addFileToProject("pkg/BUILD", """
      java_library(
          name = "base_lib",
          srcs = ["BaseLib.java"],
      )
      
      java_library(
          name = "extension_lib",
          srcs = ["ExtensionLib.java"],
          deps = [":base_lib"],
      )
      
      java_test(
          name = "base_lib_test",
          srcs = ["BaseLibTest.java"],
          deps = [":base_lib"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    
    val baseLibElement = createBazelTargetElement("base_lib")
    assertNotNull("Should find base_lib target", baseLibElement)
    
    // Find all references to base_lib
    val references = ReferencesSearch.search(baseLibElement!!).findAll()
    assertEquals("Should find 2 references to base_lib", 2, references.size)
    
    // Should not be safe to delete due to local references
    assertFalse("Target with local references should not be safe to delete",
                canSafelyDeleteTarget(baseLibElement))
  }

  fun testSafeDeleteTargetWithExternalReferences() {
    // Create target to delete
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "common_lib",
          srcs = ["CommonLib.java"],
      )
    """.trimIndent())
    
    // Create external reference
    myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp", 
          main_class = "App",
          deps = ["//lib:common_lib"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    val commonLibElement = createBazelTargetElement("common_lib")
    assertNotNull("Should find common_lib target", commonLibElement)
    
    // Should not be safe to delete due to external references
    assertFalse("Target with external references should not be safe to delete",
                canSafelyDeleteTarget(commonLibElement!!))
  }

  fun testSafeDeleteTargetWithTestReferences() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "mylib",
          srcs = ["MyLib.java"], 
      )
    """.trimIndent())
    
    // Create reference from test
    myFixture.addFileToProject("lib/test/BUILD", """
      java_test(
          name = "mylib_test",
          srcs = ["MyLibTest.java"],
          deps = ["//lib:mylib"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    val mylibElement = createBazelTargetElement("mylib")
    assertNotNull("Should find mylib target", mylibElement)
    
    // Test references might be considered "safe" depending on policy
    // For this test, assume test references are safe to break
    val references = ReferencesSearch.search(mylibElement!!).findAll()
    assertTrue("Should find test references", references.isNotEmpty())
    
    val safeReferences = references.filter { isSafeReference(it.element) }
    assertEquals("Test references should be considered safe", references.size, safeReferences.size)
  }

  fun testSafeDeleteAlias() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "actual_lib",
          srcs = ["ActualLib.java"],
      )
      
      alias(
          name = "lib_alias",
          actual = ":actual_lib",
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    val aliasElement = createBazelTargetElement("lib_alias")
    assertNotNull("Should find alias target", aliasElement)
    
    // Alias with no external references should be safe to delete
    assertTrue("Unused alias should be safe to delete",
               canSafelyDeleteTarget(aliasElement!!))
    
    aliasElement.delete()
    
    val updatedContent = myFixture.file.text
    assertFalse("Alias should be removed", updatedContent.contains("lib_alias"))
    assertTrue("Actual target should remain", updatedContent.contains("actual_lib"))
  }

  fun testSafeDeleteWithAliasReferences() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "core_lib",
          srcs = ["CoreLib.java"],
      )
      
      alias(
          name = "lib_alias",
          actual = ":core_lib",
      )
    """.trimIndent())
    
    // Reference the alias, not the original
    myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp",
          main_class = "App",
          deps = ["//lib:lib_alias"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    val coreLibElement = createBazelTargetElement("core_lib")
    assertNotNull("Should find core_lib target", coreLibElement)
    
    // Original lib has no direct references, but alias does
    val directReferences = ReferencesSearch.search(coreLibElement!!).findAll()
    val aliasReferences = findAliasReferences("lib_alias")
    
    assertTrue("Core lib should have no direct references", directReferences.isEmpty())
    assertTrue("Alias should have references", aliasReferences.isNotEmpty())
    
    // Core lib should not be safe to delete if its alias is used
    assertFalse("Target should not be safe to delete when its alias is used",
                canSafelyDeleteTarget(coreLibElement))
  }

  fun testSafeDeleteGenrule() {
    myFixture.addFileToProject("gen/BUILD", """
      genrule(
          name = "generate_config", 
          cmd = "echo 'config' > $@",
          outs = ["config.txt"],
      )
      
      java_library(
          name = "lib_with_config",
          srcs = ["Lib.java"],
          data = [":generate_config"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("gen/BUILD")
    )
    
    val genruleElement = createBazelTargetElement("generate_config")
    assertNotNull("Should find genrule target", genruleElement)
    
    // Genrule with references should not be safe to delete
    assertFalse("Genrule with references should not be safe to delete",
                canSafelyDeleteTarget(genruleElement!!))
  }

  fun testSafeDeleteRefactoringSupport() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "testlib",
          srcs = ["TestLib.java"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    // Find the target name string literal
    val targetNameElement = findTargetNameElement("testlib")
    assertNotNull("Should find target name element", targetNameElement)
    
    val refactoringSupport = BazelTargetRefactoringSupport()
    
    // Test that safe delete is available for target elements
    assertTrue("Safe delete should be available for target elements",
               refactoringSupport.isSafeDeleteAvailable(targetNameElement!!))
    
    // Test that in-place rename is available
    assertTrue("In-place rename should be available for target elements",
               refactoringSupport.isInplaceRenameAvailable(targetNameElement, null))
  }

  fun testSafeDeleteWithDependencyChain() {
    myFixture.addFileToProject("chain/BUILD", """
      java_library(
          name = "base",
          srcs = ["Base.java"],
      )
      
      java_library(
          name = "middle",
          srcs = ["Middle.java"],
          deps = [":base"],
      )
      
      java_library(
          name = "top",
          srcs = ["Top.java"],
          deps = [":middle"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("chain/BUILD")
    )
    
    // Base is used by middle - not safe to delete
    val baseElement = createBazelTargetElement("base")
    assertFalse("Base target should not be safe to delete",
                canSafelyDeleteTarget(baseElement!!))
    
    // Middle is used by top - not safe to delete
    val middleElement = createBazelTargetElement("middle")
    assertFalse("Middle target should not be safe to delete", 
                canSafelyDeleteTarget(middleElement!!))
    
    // Top is not used - safe to delete
    val topElement = createBazelTargetElement("top")
    assertTrue("Top target should be safe to delete",
               canSafelyDeleteTarget(topElement!!))
  }

  private fun createBazelTargetElement(targetName: String): BazelTargetElement? {
    val targetNameElement = findTargetNameElement(targetName) ?: return null
    val callExpression = findTargetCallExpression(targetNameElement) ?: return null
    val packagePath = getPackagePath()
    
    return BazelTargetElement(callExpression, targetName, packagePath)
  }

  private fun findTargetNameElement(targetName: String): com.intellij.psi.PsiElement? {
    var result: com.intellij.psi.PsiElement? = null
    
    myFixture.file.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
      override fun visitElement(element: com.intellij.psi.PsiElement) {
        super.visitElement(element)
        if (element is StarlarkStringLiteralExpression && 
            element.stringValue == targetName) {
          result = element
        }
      }
    })
    
    return result
  }

  private fun findTargetCallExpression(element: com.intellij.psi.PsiElement): org.jetbrains.bazel.languages.starlark.psi.StarlarkCallExpression? {
    var current = element.parent
    while (current != null) {
      if (current is org.jetbrains.bazel.languages.starlark.psi.StarlarkCallExpression) {
        return current
      }
      current = current.parent
    }
    return null
  }

  private fun canSafelyDeleteTarget(targetElement: BazelTargetElement): Boolean {
    val references = ReferencesSearch.search(targetElement).findAll()
    return references.all { isSafeReference(it.element) }
  }

  private fun isSafeReference(element: com.intellij.psi.PsiElement): Boolean {
    val file = element.containingFile
    val fileName = file?.name ?: ""
    
    // Consider references in test files as "safe" to break
    return fileName.contains("test", ignoreCase = true) || 
           fileName.contains("Test")
  }

  private fun findAliasReferences(aliasName: String): List<com.intellij.psi.PsiReference> {
    val references = mutableListOf<com.intellij.psi.PsiReference>()
    
    // Search all files for references to the alias
    myFixture.findFileInTempDir("app/BUILD")?.let { file ->
      val psiFile = myFixture.psiManager.findFile(file)
      psiFile?.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
        override fun visitElement(element: com.intellij.psi.PsiElement) {
          super.visitElement(element)
          if (element is StarlarkStringLiteralExpression &&
              element.stringValue.contains(aliasName)) {
            references.addAll(element.references.filterIsInstance<BazelTargetReference>())
          }
        }
      })
    }
    
    return references
  }

  private fun getPackagePath(): String {
    val file = myFixture.file
    val virtualFile = file.virtualFile ?: return ""
    val filePath = virtualFile.path
    
    // Extract package path from file path
    val buildIndex = filePath.lastIndexOf("/BUILD")
    if (buildIndex == -1) return ""
    
    val packagePath = filePath.substring(0, buildIndex)
    val lastSlash = packagePath.lastIndexOf("/")
    return if (lastSlash == -1) "" else packagePath.substring(lastSlash + 1)
  }
}