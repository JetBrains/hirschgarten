package org.jetbrains.bazel.symbols

import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression

class BazelTargetReferenceTest : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    // Set up a mock Bazel workspace structure
    myFixture.addFileToProject("WORKSPACE", "")
    myFixture.addFileToProject("MODULE.bazel", "")
  }

  fun testReferenceInBuildFile() {
    // Create a target that can be referenced
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "mylib",
          srcs = ["MyLib.java"],
      )
    """.trimIndent())
    
    // Create a BUILD file that references the target
    val buildFile = myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp",
          main_class = "com.example.Main",
          deps = ["//lib:mylib"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    
    // Find the reference to //lib:mylib
    val references = findTargetReferences()
    assertTrue("Should find reference to //lib:mylib", references.isNotEmpty())
    
    val reference = references.find { it.canonicalText == "//lib:mylib" }
    assertNotNull("Should find reference to //lib:mylib", reference)
    
    // Test resolution
    val resolved = reference!!.resolve()
    assertNotNull("Reference should resolve", resolved)
  }

  fun testReferenceInBzlFile() {
    // Create target to reference
    myFixture.addFileToProject("tools/BUILD", """
      genrule(
          name = "generator",
          cmd = "echo 'generated' > $@",
          outs = ["generated.txt"],
      )
    """.trimIndent())
    
    // Create .bzl file that references the target
    val bzlFile = myFixture.addFileToProject("defs.bzl", """
      def my_macro():
          native.java_binary(
              name = "tool",
              deps = ["//tools:generator"],
          )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(bzlFile.virtualFile)
    
    val references = findTargetReferences()
    val generatorRef = references.find { it.canonicalText == "//tools:generator" }
    assertNotNull("Should find reference to //tools:generator in .bzl file", generatorRef)
  }

  fun testRelativeReference() {
    myFixture.addFileToProject("pkg/BUILD", """
      java_library(
          name = "lib1",
          srcs = ["Lib1.java"],
      )
      
      java_library(
          name = "lib2", 
          srcs = ["Lib2.java"],
          deps = [":lib1"],  # Relative reference
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("pkg/BUILD")
    )
    
    val references = findTargetReferences()
    val relativeRef = references.find { it.canonicalText == ":lib1" }
    assertNotNull("Should find relative reference :lib1", relativeRef)
    
    val resolved = relativeRef!!.resolve()
    assertNotNull("Relative reference should resolve", resolved)
  }

  fun testExternalRepoReference() {
    myFixture.addFileToProject("BUILD", """
      java_library(
          name = "mylib",
          deps = [
              "@maven//:junit",
              "@external_repo//pkg:target",
          ],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("BUILD")
    )
    
    val references = findTargetReferences()
    
    val mavenRef = references.find { it.canonicalText == "@maven//:junit" }
    assertNotNull("Should find reference to external repo @maven//:junit", mavenRef)
    
    val externalRef = references.find { it.canonicalText == "@external_repo//pkg:target" }
    assertNotNull("Should find reference to @external_repo//pkg:target", externalRef)
  }

  fun testInvalidReference() {
    myFixture.addFileToProject("BUILD", """
      java_library(
          name = "mylib",
          deps = ["//nonexistent:target"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("BUILD")
    )
    
    val references = findTargetReferences()
    val invalidRef = references.find { it.canonicalText == "//nonexistent:target" }
    assertNotNull("Should create reference even for non-existent target", invalidRef)
    
    val resolved = invalidRef!!.resolve()
    assertNull("Invalid reference should not resolve", resolved)
  }

  fun testCompletionVariants() {
    // Create several targets
    myFixture.addFileToProject("lib1/BUILD", """
      java_library(name = "common", srcs = ["Common.java"])
    """.trimIndent())
    
    myFixture.addFileToProject("lib2/BUILD", """
      java_library(name = "utils", srcs = ["Utils.java"]) 
    """.trimIndent())
    
    myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp",
          deps = ["<caret>"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("app/BUILD")
    )
    
    val completions = myFixture.getCompletionVariants("app/BUILD")
    
    assertNotNull("Should provide completion variants", completions)
    assertTrue("Should suggest 'common' target", completions!!.any { it.toString().contains("common") })
    assertTrue("Should suggest 'utils' target", completions.any { it.toString().contains("utils") })
  }

  fun testRenameTarget() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "oldname",
          srcs = ["Lib.java"],
      )
    """.trimIndent())
    
    myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp",
          deps = ["//lib:oldname"],
      )
    """.trimIndent())
    
    // Configure on the target definition
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    // Find the target name element and rename it
    val targetNameElement = findTargetNameElement("oldname")
    assertNotNull("Should find target name element", targetNameElement)
    
    myFixture.renameElement(targetNameElement!!, "newname")
    
    // Verify the reference was updated
    val appBuildContent = myFixture.findFileInTempDir("app/BUILD").loadText()
    assertTrue("Reference should be updated to newname", 
               appBuildContent.contains("//lib:newname"))
    assertFalse("Old reference should be gone",
                appBuildContent.contains("//lib:oldname"))
  }

  private fun findTargetReferences(): List<PsiReference> {
    val references = mutableListOf<PsiReference>()
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
}