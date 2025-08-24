package org.jetbrains.bazel.symbols

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.label.*

class BazelTargetSymbolTest : BasePlatformTestCase() {

  fun testSymbolCreation() {
    val label = ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf("java", "com", "example")),
      target = SingleTarget("mylib")
    )
    
    val symbol = BazelTargetSymbol(
      label = label,
      buildFilePath = "/workspace/java/com/example/BUILD",
      targetType = BazelTargetType.JAVA_LIBRARY
    )
    
    assertEquals("mylib", symbol.targetName)
    assertEquals("java/com/example", symbol.packagePath)
    assertTrue(symbol.isMainWorkspace)
    assertEquals(BazelTargetType.JAVA_LIBRARY, symbol.targetType)
  }

  fun testSymbolEquality() {
    val label = ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf("java", "test")),
      target = SingleTarget("testlib")
    )
    
    val symbol1 = BazelTargetSymbol(
      label = label,
      buildFilePath = "/workspace/java/test/BUILD",
      targetType = BazelTargetType.JAVA_TEST
    )
    
    val symbol2 = BazelTargetSymbol(
      label = label,
      buildFilePath = "/workspace/java/test/BUILD",
      targetType = BazelTargetType.JAVA_TEST
    )
    
    val symbol3 = BazelTargetSymbol(
      label = label,
      buildFilePath = "/workspace/java/test/BUILD.bazel", // Different path
      targetType = BazelTargetType.JAVA_TEST
    )
    
    assertEquals(symbol1, symbol2)
    assertNotEquals(symbol1, symbol3)
    assertEquals(symbol1.hashCode(), symbol2.hashCode())
  }

  fun testSymbolPointer() {
    val label = ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf("cpp")),
      target = SingleTarget("mybin")
    )
    
    val symbol = BazelTargetSymbol(
      label = label,
      buildFilePath = "/workspace/cpp/BUILD",
      targetType = BazelTargetType.CC_BINARY
    )
    
    val pointer = symbol.createPointer()
    val dereferenced = pointer.dereference()
    
    assertNotNull(dereferenced)
    assertEquals(symbol, dereferenced)
  }

  fun testSymbolWithAliases() {
    val label = ResolvedLabel(
      repo = Main,
      packagePath = Package(listOf("tools")),
      target = SingleTarget("tool")
    )
    
    val aliases = setOf("mytool", "tool_alias")
    val symbol = BazelTargetSymbol(
      label = label,
      buildFilePath = "/workspace/tools/BUILD",
      targetType = BazelTargetType.GENRULE,
      aliases = aliases
    )
    
    assertTrue(symbol.matchesTargetName("tool"))
    assertTrue(symbol.matchesTargetName("mytool"))
    assertTrue(symbol.matchesTargetName("tool_alias"))
    assertFalse(symbol.matchesTargetName("other_tool"))
    
    val newAliases = setOf("another_alias")
    val symbolWithMoreAliases = symbol.withAliases(newAliases)
    assertTrue(symbolWithMoreAliases.aliases.contains("mytool"))
    assertTrue(symbolWithMoreAliases.aliases.contains("another_alias"))
  }

  fun testTargetType() {
    assertEquals(BazelTargetType.JAVA_BINARY, BazelTargetType.fromRuleName("java_binary"))
    assertEquals(BazelTargetType.JAVA_LIBRARY, BazelTargetType.fromRuleName("java_library"))
    assertEquals(BazelTargetType.JAVA_TEST, BazelTargetType.fromRuleName("java_test"))
    assertEquals(BazelTargetType.CC_BINARY, BazelTargetType.fromRuleName("cc_binary"))
    assertEquals(BazelTargetType.GENRULE, BazelTargetType.fromRuleName("genrule"))
    assertEquals(BazelTargetType.CUSTOM_RULE, BazelTargetType.fromRuleName("custom_rule"))
    
    assertTrue(BazelTargetType.JAVA_BINARY.isBinaryTarget)
    assertTrue(BazelTargetType.JAVA_LIBRARY.isLibraryTarget)
    assertTrue(BazelTargetType.JAVA_TEST.isTestTarget)
    assertFalse(BazelTargetType.GENRULE.isBinaryTarget)
  }

  fun testSymbolPresentation() {
    val label = ResolvedLabel(
      repo = Canonical("external_repo"),
      packagePath = Package(listOf("pkg", "subpkg")),
      target = SingleTarget("my_target")
    )
    
    val symbol = BazelTargetSymbol(
      label = label,
      buildFilePath = "/workspace/external/pkg/subpkg/BUILD",
      targetType = BazelTargetType.JAVA_LIBRARY
    )
    
    val presentation = symbol.getSymbolPresentation()
    assertEquals("my_target", presentation.getShortDescription())
    assertEquals("@@external_repo//pkg/subpkg:my_target", presentation.getLongDescription())
    assertNotNull(presentation.getIcon(false))
  }

  fun testSyntheticSymbol() {
    val syntheticLabel = SyntheticLabel(SingleTarget("synthetic_target"))
    val symbol = BazelTargetSymbol(
      label = syntheticLabel,
      buildFilePath = "",
      targetType = BazelTargetType.UNKNOWN
    )
    
    assertTrue(symbol.label.isSynthetic)
    assertEquals("synthetic_target", symbol.targetName)
    assertEquals("", symbol.packagePath)
  }
}