package org.jetbrains.bazel.symbols

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression

/**
 * Tests for rename functionality of Bazel targets using the Symbol API
 */
class BazelTargetRenameTest : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    myFixture.addFileToProject("WORKSPACE", "")
    myFixture.addFileToProject("MODULE.bazel", "")
  }

  fun testRenameTargetWithLocalReferences() {
    val buildFile = myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "old_name",
          srcs = ["Lib.java"],
      )
      
      java_test(
          name = "old_name_test",
          srcs = ["LibTest.java"],
          deps = [":old_name"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    
    // Find the target name element and rename it
    val targetNameElement = findTargetNameElement("old_name")
    assertNotNull("Should find target name element", targetNameElement)
    
    myFixture.renameElement(targetNameElement!!, "new_name")
    
    // Verify both definition and reference were updated
    val updatedContent = myFixture.file.text
    assertTrue("Definition should be renamed", updatedContent.contains("name = \"new_name\""))
    assertTrue("Reference should be updated", updatedContent.contains("\":new_name\""))
    assertFalse("Old name should be gone from definition", updatedContent.contains("name = \"old_name\""))
    assertFalse("Old name should be gone from reference", updatedContent.contains("\":old_name\""))
  }

  fun testRenameTargetWithExternalReferences() {
    // Create target to rename
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
    
    // Create another external reference
    myFixture.addFileToProject("test/BUILD", """
      java_test(
          name = "integration_test",
          srcs = ["IntegrationTest.java"],
          deps = ["//lib:common_lib"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    val targetNameElement = findTargetNameElement("common_lib")
    assertNotNull("Should find target name element", targetNameElement)
    
    myFixture.renameElement(targetNameElement!!, "shared_lib")
    
    // Verify definition was renamed
    val libBuildContent = myFixture.findFileInTempDir("lib/BUILD").loadText()
    assertTrue("Definition should be renamed", libBuildContent.contains("name = \"shared_lib\""))
    assertFalse("Old definition should be gone", libBuildContent.contains("name = \"common_lib\""))
    
    // Verify external references were updated
    val appBuildContent = myFixture.findFileInTempDir("app/BUILD").loadText()
    assertTrue("App reference should be updated", appBuildContent.contains("//lib:shared_lib"))
    assertFalse("Old app reference should be gone", appBuildContent.contains("//lib:common_lib"))
    
    val testBuildContent = myFixture.findFileInTempDir("test/BUILD").loadText()
    assertTrue("Test reference should be updated", testBuildContent.contains("//lib:shared_lib"))
    assertFalse("Old test reference should be gone", testBuildContent.contains("//lib:common_lib"))
  }

  fun testRenameTargetInProjectViewFile() {
    // Create target
    myFixture.addFileToProject("services/BUILD", """
      java_binary(
          name = "server",
          main_class = "Server", 
      )
    """.trimIndent())
    
    // Reference in .bazelproject file
    myFixture.addFileToProject(".bazelproject", """
      targets:
        //services:server
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("services/BUILD")
    )
    
    val targetNameElement = findTargetNameElement("server")
    assertNotNull("Should find target name element", targetNameElement)
    
    myFixture.renameElement(targetNameElement!!, "web_server")
    
    // Verify definition was renamed
    val buildContent = myFixture.findFileInTempDir("services/BUILD").loadText()
    assertTrue("Definition should be renamed", buildContent.contains("name = \"web_server\""))
    
    // Verify project view reference was updated
    val projectViewContent = myFixture.findFileInTempDir(".bazelproject").loadText()
    assertTrue("Project view reference should be updated", 
               projectViewContent.contains("//services:web_server"))
    assertFalse("Old project view reference should be gone",
                projectViewContent.contains("//services:server"))
  }

  fun testRenameTargetInBzlFile() {
    // Create target
    myFixture.addFileToProject("tools/BUILD", """
      genrule(
          name = "code_generator",
          cmd = "echo 'generated' > $@",
          outs = ["generated.txt"],
      )
    """.trimIndent())
    
    // Reference in .bzl file
    myFixture.addFileToProject("defs.bzl", """
      def generate_code():
          native.genrule(
              name = "gen_code",
              cmd = "$(location //tools:code_generator)",
              tools = ["//tools:code_generator"],
              outs = ["code.txt"],
          )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("tools/BUILD")
    )
    
    val targetNameElement = findTargetNameElement("code_generator")
    assertNotNull("Should find target name element", targetNameElement)
    
    myFixture.renameElement(targetNameElement!!, "codegen_tool")
    
    // Verify definition was renamed
    val buildContent = myFixture.findFileInTempDir("tools/BUILD").loadText()
    assertTrue("Definition should be renamed", buildContent.contains("name = \"codegen_tool\""))
    
    // Verify .bzl file references were updated
    val bzlContent = myFixture.findFileInTempDir("defs.bzl").loadText()
    assertTrue("First bzl reference should be updated",
               bzlContent.contains("//tools:codegen_tool"))
    assertEquals("Should have exactly 2 references to new name", 2,
                 bzlContent.split("//tools:codegen_tool").size - 1)
    assertFalse("Old bzl references should be gone",
                bzlContent.contains("//tools:code_generator"))
  }

  fun testRenameAlias() {
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
    
    // Reference the alias
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
    
    // Rename the alias
    val aliasNameElement = findTargetNameElement("lib_alias")
    assertNotNull("Should find alias name element", aliasNameElement)
    
    myFixture.renameElement(aliasNameElement!!, "common_lib")
    
    // Verify alias definition was renamed
    val buildContent = myFixture.findFileInTempDir("lib/BUILD").loadText()
    assertTrue("Alias definition should be renamed", 
               buildContent.contains("name = \"common_lib\""))
    assertFalse("Old alias definition should be gone",
                buildContent.contains("name = \"lib_alias\""))
    
    // Verify external reference was updated
    val appContent = myFixture.findFileInTempDir("app/BUILD").loadText()
    assertTrue("App reference should be updated", appContent.contains("//lib:common_lib"))
    assertFalse("Old app reference should be gone", appContent.contains("//lib:lib_alias"))
  }

  fun testRenameTargetWithComplexReferences() {
    myFixture.addFileToProject("base/BUILD", """
      java_library(
          name = "foundation",
          srcs = ["Foundation.java"],
      )
    """.trimIndent())
    
    // Multiple types of references
    myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "app1",
          main_class = "App1",
          deps = ["//base:foundation"],
      )
      
      java_binary(
          name = "app2",
          main_class = "App2", 
          runtime_deps = ["//base:foundation"],
      )
    """.trimIndent())
    
    myFixture.addFileToProject("test/BUILD", """
      java_test(
          name = "base_test",
          srcs = ["BaseTest.java"],
          deps = ["//base:foundation"],
      )
    """.trimIndent())
    
    myFixture.addFileToProject(".bazelproject", """
      targets:
        //base:foundation
        //app:app1
        //app:app2
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("base/BUILD")
    )
    
    val targetNameElement = findTargetNameElement("foundation")
    assertNotNull("Should find target name element", targetNameElement)
    
    myFixture.renameElement(targetNameElement!!, "core")
    
    // Verify all references were updated
    val baseContent = myFixture.findFileInTempDir("base/BUILD").loadText()
    assertTrue("Definition should be renamed", baseContent.contains("name = \"core\""))
    
    val appContent = myFixture.findFileInTempDir("app/BUILD").loadText()
    assertEquals("Should find 2 references in app BUILD", 2,
                 appContent.split("//base:core").size - 1)
    assertFalse("Old app references should be gone", appContent.contains("//base:foundation"))
    
    val testContent = myFixture.findFileInTempDir("test/BUILD").loadText()
    assertTrue("Test reference should be updated", testContent.contains("//base:core"))
    
    val projectContent = myFixture.findFileInTempDir(".bazelproject").loadText()
    assertTrue("Project view reference should be updated", projectContent.contains("//base:core"))
    assertFalse("Old project view reference should be gone", 
                projectContent.contains("//base:foundation"))
  }

  fun testRenameTargetUpdatesIndex() {
    myFixture.addFileToProject("pkg/BUILD", """
      java_library(
          name = "original_name",
          srcs = ["Lib.java"],
      )
    """.trimIndent())
    
    // Verify original name is indexed
    val originalTargets = BazelTargetIndex.getTargetsByName("original_name", myFixture.project)
    assertEquals("Should find original target", 1, originalTargets.size)
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("pkg/BUILD")
    )
    
    val targetNameElement = findTargetNameElement("original_name")
    myFixture.renameElement(targetNameElement!!, "renamed_target")
    
    // Verify index was updated
    val oldTargets = BazelTargetIndex.getTargetsByName("original_name", myFixture.project)
    assertEquals("Should not find old target name", 0, oldTargets.size)
    
    val newTargets = BazelTargetIndex.getTargetsByName("renamed_target", myFixture.project)
    assertEquals("Should find new target name", 1, newTargets.size)
    assertEquals("Target info should be updated", "renamed_target", newTargets.first().targetName)
  }

  fun testRenameInplaceSupport() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "testlib",
          srcs = ["TestLib.java"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    val targetNameElement = findTargetNameElement("testlib")
    assertNotNull("Should find target name element", targetNameElement)
    
    val refactoringSupport = BazelTargetRefactoringSupport()
    
    // Test in-place rename availability
    assertTrue("In-place rename should be available",
               refactoringSupport.isInplaceRenameAvailable(targetNameElement!!, null))
    
    // Test that member in-place rename is not available (Bazel targets don't have members)
    assertFalse("Member in-place rename should not be available",
                refactoringSupport.isMemberInplaceRenameAvailable(targetNameElement, null))
  }

  fun testRenameWithInvalidNewName() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "valid_name",
          srcs = ["Lib.java"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    val targetNameElement = findTargetNameElement("valid_name")
    assertNotNull("Should find target name element", targetNameElement)
    
    try {
      // Try to rename to invalid name (with spaces)
      myFixture.renameElement(targetNameElement!!, "invalid name with spaces")
      fail("Should throw exception for invalid target name")
    } catch (e: Exception) {
      // Expected - invalid target names should be rejected
      assertTrue("Should get meaningful error", e.message?.contains("invalid") == true ||
                                                e.message?.contains("space") == true)
    }
    
    // Verify original name is preserved
    val content = myFixture.file.text
    assertTrue("Original name should be preserved", content.contains("name = \"valid_name\""))
  }

  fun testRenameTargetReferenceHandling() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "target_to_rename",
          srcs = ["Lib.java"],
      )
    """.trimIndent())
    
    myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp",
          deps = ["//lib:target_to_rename"],
      )
    """.trimIndent())
    
    // Configure on the reference, not the definition
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("app/BUILD")
    )
    
    // Find the reference and test rename handling
    val references = findAllTargetReferences()
    val targetRef = references.find { it.canonicalText == "//lib:target_to_rename" }
    assertNotNull("Should find target reference", targetRef)
    
    // Test reference rename behavior
    val updatedReference = targetRef!!.handleElementRename("new_target_name")
    assertNotNull("Should handle rename", updatedReference)
    
    // The exact behavior depends on the reference implementation
    // This tests that the mechanism works without throwing exceptions
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

  private fun findAllTargetReferences(): List<com.intellij.psi.PsiReference> {
    val references = mutableListOf<com.intellij.psi.PsiReference>()
    val file = myFixture.file
    
    file.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
      override fun visitElement(element: com.intellij.psi.PsiElement) {
        super.visitElement(element)
        if (element is StarlarkStringLiteralExpression) {
          val elementReferences = element.references
          references.addAll(elementReferences.filterIsInstance<BazelTargetReference>())
        }
      }
    })
    
    return references
  }
}