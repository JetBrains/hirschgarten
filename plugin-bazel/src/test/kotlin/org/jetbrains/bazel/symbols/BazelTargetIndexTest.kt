package org.jetbrains.bazel.symbols

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.indexing.FileBasedIndex

class BazelTargetIndexTest : BasePlatformTestCase() {

  fun testIndexBuildFile() {
    val buildContent = """
      java_library(
          name = "test_lib",
          srcs = ["Lib.java"],
          deps = ["//base:common"],
      )
      
      java_test(
          name = "test_lib_test",
          srcs = ["LibTest.java"],
          deps = [":test_lib"],
      )
      
      alias(
          name = "lib_alias",
          actual = ":test_lib",
      )
    """.trimIndent()
    
    // Create a BUILD file in the test project
    val buildFile = myFixture.addFileToProject("java/test/BUILD", buildContent)
    
    // Get indexed targets
    val allTargetNames = BazelTargetIndex.getAllTargetNames(myFixture.project)
    
    // Verify targets are indexed
    assertTrue(allTargetNames.contains("test_lib"))
    assertTrue(allTargetNames.contains("test_lib_test"))
    assertTrue(allTargetNames.contains("lib_alias"))
    
    // Verify we can find targets by name
    val testLibTargets = BazelTargetIndex.getTargetsByName("test_lib", myFixture.project)
    assertEquals(1, testLibTargets.size)
    
    val testLib = testLibTargets.first()
    assertEquals("test_lib", testLib.targetName)
    assertEquals("java/test", testLib.packagePath)
    assertEquals(BazelTargetType.JAVA_LIBRARY, testLib.targetType)
    assertEquals("java_library", testLib.ruleName)
    assertEquals(listOf("//base:common"), testLib.dependencies)
    
    // Verify alias is indexed with correct information
    val aliasTargets = BazelTargetIndex.getTargetsByName("lib_alias", myFixture.project)
    assertEquals(1, aliasTargets.size)
    
    val alias = aliasTargets.first()
    assertEquals("lib_alias", alias.targetName)
    assertEquals(BazelTargetType.ALIAS, alias.targetType)
    assertTrue(alias.aliases.contains("lib_alias"))
  }

  fun testIndexMultiplePackages() {
    // Create BUILD files in different packages
    myFixture.addFileToProject("pkg1/BUILD", """
      java_library(name = "lib1", srcs = ["Lib1.java"])
    """.trimIndent())
    
    myFixture.addFileToProject("pkg2/BUILD", """
      java_library(name = "lib2", srcs = ["Lib2.java"])
    """.trimIndent())
    
    myFixture.addFileToProject("pkg1/subpkg/BUILD", """
      java_binary(name = "bin1", main_class = "Main", deps = ["//pkg1:lib1"])
    """.trimIndent())
    
    // Verify targets from different packages
    val targetsInPkg1 = BazelTargetIndex.getTargetsInPackage("pkg1", myFixture.project)
    assertEquals(1, targetsInPkg1.size)
    assertEquals("lib1", targetsInPkg1.first().targetName)
    
    val targetsInPkg2 = BazelTargetIndex.getTargetsInPackage("pkg2", myFixture.project)
    assertEquals(1, targetsInPkg2.size)
    assertEquals("lib2", targetsInPkg2.first().targetName)
    
    val targetsInSubPkg = BazelTargetIndex.getTargetsInPackage("pkg1/subpkg", myFixture.project)
    assertEquals(1, targetsInSubPkg.size)
    assertEquals("bin1", targetsInSubPkg.first().targetName)
    assertEquals(BazelTargetType.JAVA_BINARY, targetsInSubPkg.first().targetType)
  }

  fun testIndexRootPackage() {
    myFixture.addFileToProject("BUILD", """
      filegroup(
          name = "root_config",
          srcs = ["config.yaml"],
      )
    """.trimIndent())
    
    val rootTargets = BazelTargetIndex.getTargetsInPackage("", myFixture.project)
    assertEquals(1, rootTargets.size)
    assertEquals("root_config", rootTargets.first().targetName)
    assertEquals("", rootTargets.first().packagePath)
    assertEquals(BazelTargetType.FILEGROUP, rootTargets.first().targetType)
  }

  fun testIndexWithAliases() {
    myFixture.addFileToProject("tools/BUILD", """
      java_binary(
          name = "tool",
          main_class = "Tool",
      )
      
      alias(
          name = "my_tool",
          actual = ":tool",
      )
      
      alias(
          name = "tool_alias",
          actual = ":tool", 
      )
    """.trimIndent())
    
    // Original target should be indexed
    val toolTargets = BazelTargetIndex.getTargetsByName("tool", myFixture.project)
    assertEquals(1, toolTargets.size)
    assertEquals(BazelTargetType.JAVA_BINARY, toolTargets.first().targetType)
    assertFalse(toolTargets.first().isAlias)
    
    // Aliases should be indexed separately
    val myToolTargets = BazelTargetIndex.getTargetsByName("my_tool", myFixture.project)
    assertEquals(1, myToolTargets.size)
    assertTrue(myToolTargets.first().isAlias)
    assertEquals("tool", myToolTargets.first().originalTargetName)
    
    val toolAliasTargets = BazelTargetIndex.getTargetsByName("tool_alias", myFixture.project)
    assertEquals(1, toolAliasTargets.size)
    assertTrue(toolAliasTargets.first().isAlias)
  }

  fun testBazelTargetInfoSerialization() {
    val targetInfo = BazelTargetInfo(
      targetName = "test_target",
      packagePath = "java/com/example",
      buildFilePath = "/workspace/java/com/example/BUILD",
      targetType = BazelTargetType.JAVA_LIBRARY,
      ruleName = "java_library",
      aliases = setOf("alias1", "alias2"),
      dependencies = listOf("//base:common", ":util"),
      isAlias = false,
      originalTargetName = null
    )
    
    // Test conversion to symbol
    val symbol = targetInfo.toSymbol()
    assertEquals("test_target", symbol.targetName)
    assertEquals("java/com/example", symbol.packagePath)
    assertEquals(BazelTargetType.JAVA_LIBRARY, symbol.targetType)
    assertTrue(symbol.aliases.contains("alias1"))
    assertTrue(symbol.aliases.contains("alias2"))
  }

  fun testIndexIgnoresNonBuildFiles() {
    // Create various files - only BUILD files should be indexed
    myFixture.addFileToProject("src/Main.java", "public class Main {}")
    myFixture.addFileToProject("README.md", "# Project")
    myFixture.addFileToProject("config.yaml", "setting: value")
    
    // These should not be indexed for targets
    val allTargetNames = BazelTargetIndex.getAllTargetNames(myFixture.project)
    assertTrue(allTargetNames.isEmpty())
    
    // Add a BUILD file - this should be indexed
    myFixture.addFileToProject("BUILD", """
      filegroup(name = "files", srcs = ["**/*"])
    """.trimIndent())
    
    val targetsAfterBuild = BazelTargetIndex.getAllTargetNames(myFixture.project)
    assertTrue(targetsAfterBuild.contains("files"))
  }

  fun testIndexHandlesComplexDependencies() {
    myFixture.addFileToProject("complex/BUILD", """
      java_library(
          name = "complex_lib",
          srcs = ["Lib.java"],
          deps = [
              "//base:foundation",
              "//util:strings", 
              "//util:collections",
              "@maven//:junit",
              "@external_repo//pkg:target",
          ],
      )
    """.trimIndent())
    
    val complexTargets = BazelTargetIndex.getTargetsByName("complex_lib", myFixture.project)
    assertEquals(1, complexTargets.size)
    
    val complexLib = complexTargets.first()
    assertEquals(5, complexLib.dependencies.size)
    assertTrue(complexLib.dependencies.contains("//base:foundation"))
    assertTrue(complexLib.dependencies.contains("@maven//:junit"))
    assertTrue(complexLib.dependencies.contains("@external_repo//pkg:target"))
  }
}