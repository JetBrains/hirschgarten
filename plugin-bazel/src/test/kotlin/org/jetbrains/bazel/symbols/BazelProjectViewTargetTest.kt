package org.jetbrains.bazel.symbols

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for target resolution in .bazelproject files
 */
class BazelProjectViewTargetTest : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    // Set up workspace
    myFixture.addFileToProject("WORKSPACE", "")
    myFixture.addFileToProject("MODULE.bazel", "")
    
    // Create some targets to reference
    myFixture.addFileToProject("java/lib/BUILD", """
      java_library(
          name = "common",
          srcs = ["Common.java"],
      )
      
      java_test(
          name = "common_test",
          srcs = ["CommonTest.java"],
          deps = [":common"],
      )
    """.trimIndent())
    
    myFixture.addFileToProject("cpp/lib/BUILD", """
      cc_library(
          name = "utils",
          srcs = ["utils.cc"],
          hdrs = ["utils.h"],
      )
    """.trimIndent())
    
    myFixture.addFileToProject("tools/BUILD", """
      genrule(
          name = "generator",
          cmd = "echo 'hello' > $@",
          outs = ["hello.txt"],
      )
    """.trimIndent())
  }

  fun testBasicProjectViewTargets() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      directories:
        java/lib
        cpp/lib
      
      targets:
        //java/lib:common
        //cpp/lib:utils
        //tools:generator
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    // Find target references in the project view file
    val references = findAllTargetReferences()
    
    assertEquals("Should find 3 target references", 3, references.size)
    
    val targets = references.map { it.canonicalText }
    assertTrue("Should reference //java/lib:common", targets.contains("//java/lib:common"))
    assertTrue("Should reference //cpp/lib:utils", targets.contains("//cpp/lib:utils"))
    assertTrue("Should reference //tools:generator", targets.contains("//tools:generator"))
    
    // Test that references resolve correctly
    references.forEach { ref ->
      val resolved = ref.resolve()
      assertNotNull("Reference ${ref.canonicalText} should resolve", resolved)
    }
  }

  fun testProjectViewWithWildcards() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      targets:
        //java/lib:all
        //cpp/...:*
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 2 wildcard target references", 2, references.size)
    
    val targets = references.map { it.canonicalText }
    assertTrue("Should reference //java/lib:all", targets.contains("//java/lib:all"))
    assertTrue("Should reference //cpp/...:*", targets.contains("//cpp/...:*"))
  }

  fun testProjectViewWithTestTargets() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      targets:
        //java/lib:common
      
      test_targets:
        //java/lib:common_test
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 2 target references", 2, references.size)
    
    val targets = references.map { it.canonicalText }
    assertTrue("Should reference //java/lib:common in targets", targets.contains("//java/lib:common"))
    assertTrue("Should reference //java/lib:common_test in test_targets", targets.contains("//java/lib:common_test"))
  }

  fun testProjectViewWithBuildFlags() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      build_flags:
        --define
        feature=enabled
        
      targets:
        //java/lib:common
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 1 target reference", 1, references.size)
    
    val ref = references.first()
    assertEquals("//java/lib:common", ref.canonicalText)
    assertNotNull("Target reference should resolve", ref.resolve())
  }

  fun testProjectViewWithAdditionalLanguages() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      additional_languages:
        javascript
        python
      
      targets:
        //java/lib:common
        //cpp/lib:utils
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 2 target references", 2, references.size)
    
    references.forEach { ref ->
      assertNotNull("Reference should resolve: ${ref.canonicalText}", ref.resolve())
    }
  }

  fun testProjectViewWithImports() {
    // Create another project view to import
    myFixture.addFileToProject("base.bazelproject", """
      targets:
        //java/lib:common
    """.trimIndent())
    
    val mainProjectView = myFixture.addFileToProject(".bazelproject", """
      import:
        base.bazelproject
        
      targets:
        //cpp/lib:utils
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(mainProjectView.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 1 target reference in main file", 1, references.size)
    assertEquals("//cpp/lib:utils", references.first().canonicalText)
  }

  fun testProjectViewTargetCompletion() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      targets:
        <caret>
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    val completions = myFixture.getCompletionVariants(".bazelproject")
    
    assertNotNull("Should provide completion variants", completions)
    
    // Check that available targets are suggested
    val completionTexts = completions!!.map { it.toString() }
    assertTrue("Should suggest common target", 
               completionTexts.any { it.contains("common") })
    assertTrue("Should suggest utils target",
               completionTexts.any { it.contains("utils") })
    assertTrue("Should suggest generator target",
               completionTexts.any { it.contains("generator") })
  }

  fun testProjectViewWithInvalidTargets() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      targets:
        //nonexistent:target
        //java/lib:common
        //also/nonexistent:target
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 3 target references", 3, references.size)
    
    // Valid target should resolve
    val validRef = references.find { it.canonicalText == "//java/lib:common" }
    assertNotNull("Should find valid reference", validRef)
    assertNotNull("Valid reference should resolve", validRef!!.resolve())
    
    // Invalid targets should not resolve
    val invalidRefs = references.filter { it.canonicalText != "//java/lib:common" }
    assertEquals("Should have 2 invalid references", 2, invalidRefs.size)
    invalidRefs.forEach { ref ->
      assertNull("Invalid reference should not resolve: ${ref.canonicalText}", ref.resolve())
    }
  }

  fun testProjectViewWithExternalTargets() {
    val projectView = myFixture.addFileToProject(".bazelproject", """
      targets:
        @maven//:junit
        @external_repo//pkg:target
        //java/lib:common
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(projectView.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 3 target references", 3, references.size)
    
    val targets = references.map { it.canonicalText }
    assertTrue("Should reference @maven//:junit", targets.contains("@maven//:junit"))
    assertTrue("Should reference @external_repo//pkg:target", targets.contains("@external_repo//pkg:target"))
    assertTrue("Should reference //java/lib:common", targets.contains("//java/lib:common"))
    
    // Only local target should resolve in test environment
    val localRef = references.find { it.canonicalText == "//java/lib:common" }
    assertNotNull("Local reference should resolve", localRef!!.resolve())
  }

  private fun findAllTargetReferences(): List<com.intellij.psi.PsiReference> {
    val references = mutableListOf<com.intellij.psi.PsiReference>()
    val file = myFixture.file
    
    file.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
      override fun visitElement(element: com.intellij.psi.PsiElement) {
        super.visitElement(element)
        val elementReferences = element.references
        references.addAll(elementReferences.filterIsInstance<BazelTargetReference>())
      }
    })
    
    return references
  }
}