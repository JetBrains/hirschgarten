package org.jetbrains.bazel.symbols

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.openapi.vfs.VirtualFile

class BazelTargetParserTest : BasePlatformTestCase() {

  fun testParseSimpleBuildFile() {
    val buildContent = """
      java_library(
          name = "mylib",
          srcs = ["MyLib.java"],
          deps = ["//other:dep"],
      )
      
      java_binary(
          name = "mybin",
          main_class = "com.example.Main",
          runtime_deps = [":mylib"],
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("java/com/example/BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    assertEquals(2, targets.size)
    
    val lib = targets.find { it.targetName == "mylib" }
    assertNotNull(lib)
    assertEquals(BazelTargetType.JAVA_LIBRARY, lib!!.targetType)
    assertEquals("java_library", lib.ruleName)
    assertEquals("java/com/example", lib.packagePath)
    assertEquals(listOf("//other:dep"), lib.dependencies)
    
    val bin = targets.find { it.targetName == "mybin" }
    assertNotNull(bin)
    assertEquals(BazelTargetType.JAVA_BINARY, bin!!.targetType)
    assertEquals("java_binary", bin.ruleName)
    assertEquals(listOf(":mylib"), bin.dependencies)
  }

  fun testParseRootPackageBuildFile() {
    val buildContent = """
      filegroup(
          name = "config",
          srcs = ["config.yaml"],
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    assertEquals(1, targets.size)
    val target = targets.first()
    assertEquals("config", target.targetName)
    assertEquals("", target.packagePath) // Root package
    assertEquals(BazelTargetType.FILEGROUP, target.targetType)
  }

  fun testParseAliasTarget() {
    val buildContent = """
      alias(
          name = "my_alias",
          actual = "//other/pkg:real_target",
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("tools/BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    assertEquals(1, targets.size)
    val alias = targets.first()
    assertEquals("my_alias", alias.targetName)
    assertEquals(BazelTargetType.ALIAS, alias.targetType)
    assertTrue(alias.aliases.contains("my_alias"))
  }

  fun testParseComplexDependencies() {
    val buildContent = """
      cc_library(
          name = "complex_lib",
          srcs = ["lib.cc"],
          hdrs = ["lib.h"],
          deps = [
              "//base:common",
              "//util:strings",
              "@external_repo//pkg:target",
          ],
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("cpp/lib/BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    assertEquals(1, targets.size)
    val lib = targets.first()
    assertEquals("complex_lib", lib.targetName)
    assertEquals(BazelTargetType.CC_LIBRARY, lib.targetType)
    assertEquals("cpp/lib", lib.packagePath)
    
    val expectedDeps = listOf(
      "//base:common",
      "//util:strings", 
      "@external_repo//pkg:target"
    )
    assertEquals(expectedDeps, lib.dependencies)
  }

  fun testParseCustomRules() {
    val buildContent = """
      custom_rule(
          name = "my_custom",
          config = "config.json",
      )
      
      proto_library(
          name = "my_proto",
          srcs = ["my.proto"],
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("proto/BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    assertEquals(2, targets.size)
    
    val custom = targets.find { it.targetName == "my_custom" }
    assertNotNull(custom)
    assertEquals(BazelTargetType.CUSTOM_RULE, custom!!.targetType)
    assertEquals("custom_rule", custom.ruleName)
    
    val proto = targets.find { it.targetName == "my_proto" }
    assertNotNull(proto)
    assertEquals(BazelTargetType.PROTO_LIBRARY, proto!!.targetType)
    assertEquals("proto_library", proto.ruleName)
  }

  fun testParseKotlinTargets() {
    val buildContent = """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_binary", "kt_jvm_test")
      
      kt_jvm_library(
          name = "kotlin_lib",
          srcs = ["Lib.kt"],
      )
      
      kt_jvm_binary(
          name = "kotlin_bin",
          srcs = ["Main.kt"],
          main_class = "com.example.MainKt",
          deps = [":kotlin_lib"],
      )
      
      kt_jvm_test(
          name = "kotlin_test",
          srcs = ["LibTest.kt"],
          test_class = "com.example.LibTest",
          deps = [":kotlin_lib"],
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("kotlin/BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    assertEquals(3, targets.size)
    
    val lib = targets.find { it.targetName == "kotlin_lib" }
    assertNotNull(lib)
    assertEquals(BazelTargetType.KOTLIN_LIBRARY, lib!!.targetType)
    assertTrue(lib.targetType.isLibraryTarget)
    
    val bin = targets.find { it.targetName == "kotlin_bin" }
    assertNotNull(bin)
    assertEquals(BazelTargetType.KOTLIN_BINARY, bin!!.targetType)
    assertTrue(bin.targetType.isBinaryTarget)
    assertEquals(listOf(":kotlin_lib"), bin.dependencies)
    
    val test = targets.find { it.targetName == "kotlin_test" }
    assertNotNull(test)
    assertEquals(BazelTargetType.KOTLIN_TEST, test!!.targetType)
    assertTrue(test.targetType.isTestTarget)
  }

  fun testParseInvalidBuildFile() {
    val buildContent = """
      # This is not a valid target definition
      some_function_call()
      
      # This is missing name parameter
      java_library(
          srcs = ["Something.java"],
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("invalid/BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    // Should not find any targets since they don't have proper name parameters
    assertEquals(0, targets.size)
  }

  fun testParseTargetWithComplexName() {
    val buildContent = """
      genrule(
          name = "generate-something_v2",
          cmd = "echo 'generated' > $@",
          outs = ["generated.txt"],
      )
    """.trimIndent()
    
    val mockFile = createMockVirtualFile("gen/BUILD", buildContent)
    val targets = BazelTargetParser.parseTargetsFromContent(buildContent, mockFile)
    
    assertEquals(1, targets.size)
    val target = targets.first()
    assertEquals("generate-something_v2", target.targetName)
    assertEquals(BazelTargetType.GENRULE, target.targetType)
  }

  private fun createMockVirtualFile(path: String, content: String): VirtualFile {
    val tempDir = myFixture.tempDirFixture
    val file = tempDir.createFile(path, content)
    return file
  }
}