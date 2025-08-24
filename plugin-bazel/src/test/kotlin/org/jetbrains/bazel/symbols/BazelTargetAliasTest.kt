package org.jetbrains.bazel.symbols

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for Bazel target alias support
 */
class BazelTargetAliasTest : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    myFixture.addFileToProject("WORKSPACE", "")
    myFixture.addFileToProject("MODULE.bazel", "")
  }

  fun testSimpleAlias() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "mylib",
          srcs = ["MyLib.java"],
      )
      
      alias(
          name = "lib_alias",
          actual = ":mylib",
      )
    """.trimIndent())
    
    // Verify both the original target and alias are indexed
    val allTargets = BazelTargetIndex.getAllTargetNames(myFixture.project)
    assertTrue("Should index original target", allTargets.contains("mylib"))
    assertTrue("Should index alias", allTargets.contains("lib_alias"))
    
    // Verify original target
    val originalTargets = BazelTargetIndex.getTargetsByName("mylib", myFixture.project)
    assertEquals(1, originalTargets.size)
    val original = originalTargets.first()
    assertEquals("mylib", original.targetName)
    assertEquals(BazelTargetType.JAVA_LIBRARY, original.targetType)
    assertFalse("Original target should not be marked as alias", original.isAlias)
    assertNull("Original target should not have originalTargetName", original.originalTargetName)
    
    // Verify alias
    val aliasTargets = BazelTargetIndex.getTargetsByName("lib_alias", myFixture.project)
    assertEquals(1, aliasTargets.size)
    val alias = aliasTargets.first()
    assertEquals("lib_alias", alias.targetName)
    assertEquals(BazelTargetType.ALIAS, alias.targetType)
    assertTrue("Alias should be marked as alias", alias.isAlias)
    assertEquals("lib_alias", alias.originalTargetName)
  }

  fun testMultipleAliases() {
    myFixture.addFileToProject("tools/BUILD", """
      genrule(
          name = "generator",
          cmd = "echo 'hello' > $@",
          outs = ["hello.txt"],
      )
      
      alias(
          name = "gen_tool",
          actual = ":generator",
      )
      
      alias(
          name = "tool",
          actual = ":generator",
      )
      
      alias(
          name = "my_generator",
          actual = ":generator",
      )
    """.trimIndent())
    
    val allTargets = BazelTargetIndex.getAllTargetNames(myFixture.project)
    assertTrue("Should index original target", allTargets.contains("generator"))
    assertTrue("Should index first alias", allTargets.contains("gen_tool"))
    assertTrue("Should index second alias", allTargets.contains("tool"))
    assertTrue("Should index third alias", allTargets.contains("my_generator"))
    
    // Verify all aliases point to the original
    val aliasNames = listOf("gen_tool", "tool", "my_generator")
    for (aliasName in aliasNames) {
      val aliasTargets = BazelTargetIndex.getTargetsByName(aliasName, myFixture.project)
      assertEquals("Should find exactly one alias for $aliasName", 1, aliasTargets.size)
      val alias = aliasTargets.first()
      assertTrue("$aliasName should be marked as alias", alias.isAlias)
      assertEquals("$aliasName should point to original", aliasName, alias.originalTargetName)
    }
  }

  fun testAliasReference() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "common",
          srcs = ["Common.java"],
      )
      
      alias(
          name = "shared_lib",
          actual = ":common",
      )
    """.trimIndent())
    
    // Create a BUILD file that references both the original and alias
    val buildFile = myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "app1",
          main_class = "App1",
          deps = ["//lib:common"],  # Reference original
      )
      
      java_binary(
          name = "app2", 
          main_class = "App2",
          deps = ["//lib:shared_lib"],  # Reference alias
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    
    val references = findAllTargetReferences()
    assertEquals("Should find 2 target references", 2, references.size)
    
    // Both references should resolve
    val originalRef = references.find { it.canonicalText == "//lib:common" }
    assertNotNull("Should find reference to original", originalRef)
    assertNotNull("Original reference should resolve", originalRef!!.resolve())
    
    val aliasRef = references.find { it.canonicalText == "//lib:shared_lib" }
    assertNotNull("Should find reference to alias", aliasRef)
    assertNotNull("Alias reference should resolve", aliasRef!!.resolve())
  }

  fun testAliasSymbol() {
    val aliasInfo = BazelTargetInfo(
      targetName = "my_alias",
      packagePath = "tools",
      buildFilePath = "/workspace/tools/BUILD",
      targetType = BazelTargetType.ALIAS,
      ruleName = "alias",
      aliases = setOf("my_alias"),
      dependencies = emptyList(),
      isAlias = true,
      originalTargetName = "my_alias"
    )
    
    val symbol = aliasInfo.toSymbol()
    assertEquals("my_alias", symbol.targetName)
    assertEquals("tools", symbol.packagePath)
    assertEquals(BazelTargetType.ALIAS, symbol.targetType)
    assertTrue("Symbol should match alias name", symbol.matchesTargetName("my_alias"))
    assertTrue("Symbol should contain alias in aliases set", symbol.aliases.contains("my_alias"))
  }

  fun testAliasChain() {
    // Test alias pointing to another alias
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "original",
          srcs = ["Original.java"],
      )
      
      alias(
          name = "first_alias",
          actual = ":original",
      )
      
      alias(
          name = "second_alias", 
          actual = ":first_alias",
      )
    """.trimIndent())
    
    val allTargets = BazelTargetIndex.getAllTargetNames(myFixture.project)
    assertTrue("Should index all targets", allTargets.containsAll(
      listOf("original", "first_alias", "second_alias")
    ))
    
    // Verify alias chain indexing
    val firstAlias = BazelTargetIndex.getTargetsByName("first_alias", myFixture.project).first()
    assertTrue("First alias should be marked as alias", firstAlias.isAlias)
    
    val secondAlias = BazelTargetIndex.getTargetsByName("second_alias", myFixture.project).first()
    assertTrue("Second alias should be marked as alias", secondAlias.isAlias)
  }

  fun testCrossPackageAlias() {
    myFixture.addFileToProject("core/BUILD", """
      java_library(
          name = "base",
          srcs = ["Base.java"],
      )
    """.trimIndent())
    
    myFixture.addFileToProject("api/BUILD", """
      alias(
          name = "foundation",
          actual = "//core:base",
      )
    """.trimIndent())
    
    // Reference the cross-package alias
    val buildFile = myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp",
          main_class = "App",
          deps = ["//api:foundation"],
      )
    """.trimIndent())
    
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    
    val references = findAllTargetReferences()
    val aliasRef = references.find { it.canonicalText == "//api:foundation" }
    assertNotNull("Should find cross-package alias reference", aliasRef)
    
    // Verify alias is indexed correctly
    val aliasTargets = BazelTargetIndex.getTargetsByName("foundation", myFixture.project)
    assertEquals(1, aliasTargets.size)
    val alias = aliasTargets.first()
    assertEquals("api", alias.packagePath)
    assertTrue("Cross-package alias should be marked as alias", alias.isAlias)
  }

  fun testAliasWithSymbolAPI() {
    myFixture.addFileToProject("tools/BUILD", """
      sh_binary(
          name = "script",
          srcs = ["script.sh"],
      )
      
      alias(
          name = "tool_script",
          actual = ":script",
      )
    """.trimIndent())
    
    // Test symbol creation for alias
    val aliasTargets = BazelTargetIndex.getTargetsByName("tool_script", myFixture.project)
    assertEquals(1, aliasTargets.size)
    val aliasInfo = aliasTargets.first()
    
    val aliasSymbol = aliasInfo.toSymbol()
    assertEquals("tool_script", aliasSymbol.targetName)
    assertTrue("Alias symbol should match its name", aliasSymbol.matchesTargetName("tool_script"))
    
    // Test symbol presentation
    val presentation = aliasSymbol.getSymbolPresentation()
    assertEquals("tool_script", presentation.getShortDescription())
    assertEquals("//tools:tool_script", presentation.getLongDescription())
  }

  fun testAliasRename() {
    myFixture.addFileToProject("lib/BUILD", """
      java_library(
          name = "mylib", 
          srcs = ["MyLib.java"],
      )
      
      alias(
          name = "lib_alias",
          actual = ":mylib",
      )
    """.trimIndent())
    
    myFixture.addFileToProject("app/BUILD", """
      java_binary(
          name = "myapp",
          deps = ["//lib:lib_alias"],
      )
    """.trimIndent())
    
    // Configure on the alias definition
    myFixture.configureFromExistingVirtualFile(
      myFixture.findFileInTempDir("lib/BUILD")
    )
    
    // Find and rename the alias
    val aliasNameElement = findTargetNameElement("lib_alias")
    assertNotNull("Should find alias name element", aliasNameElement)
    
    myFixture.renameElement(aliasNameElement!!, "new_alias")
    
    // Verify references are updated
    val appBuildContent = myFixture.findFileInTempDir("app/BUILD").loadText()
    assertTrue("Reference should be updated", appBuildContent.contains("//lib:new_alias"))
    assertFalse("Old reference should be gone", appBuildContent.contains("//lib:lib_alias"))
    
    // Verify alias is re-indexed with new name
    val newAliasTargets = BazelTargetIndex.getTargetsByName("new_alias", myFixture.project)
    assertEquals("Should find renamed alias", 1, newAliasTargets.size)
    
    val oldAliasTargets = BazelTargetIndex.getTargetsByName("lib_alias", myFixture.project)
    assertEquals("Should not find old alias", 0, oldAliasTargets.size)
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

  private fun findTargetNameElement(targetName: String): com.intellij.psi.PsiElement? {
    var result: com.intellij.psi.PsiElement? = null
    
    myFixture.file.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
      override fun visitElement(element: com.intellij.psi.PsiElement) {
        super.visitElement(element)
        if (element is org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression && 
            element.stringValue == targetName) {
          result = element
        }
      }
    })
    
    return result
  }
}