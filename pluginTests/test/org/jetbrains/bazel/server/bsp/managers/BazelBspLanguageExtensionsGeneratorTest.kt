package org.jetbrains.bazel.server.bsp.managers

import com.intellij.bazel.python.backend.sync.PythonRuleSet
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.protobuf.ProtobufRuleSet
import org.jetbrains.bazel.sync.workspace.languages.java.JavaRuleSet
import org.jetbrains.bazel.util.isBundledFor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BazelBspLanguageExtensionsGeneratorTest {

  @Test
  fun `should treat protobuf as bundled only before bazel 8`() {
    ProtobufRuleSet.isBundledFor(BazelRelease(6, 4), emptyList()) shouldBe true
    ProtobufRuleSet.isBundledFor(BazelRelease(7, 4), emptyList()) shouldBe true
    ProtobufRuleSet.isBundledFor(BazelRelease(8, 0), emptyList()) shouldBe false
    ProtobufRuleSet.isBundledFor(BazelRelease(9, 0), emptyList()) shouldBe false
    // Protobuf has no autoloadHints, so it never falls back to bundled even when autoloaded
    ProtobufRuleSet.isBundledFor(BazelRelease(8, 0), listOf("protobuf")) shouldBe false
  }

  @Test
  fun `should treat java as bundled before bazel 8 and via autoloads on bazel 8`() {
    JavaRuleSet.isBundledFor(BazelRelease(7, 4), emptyList()) shouldBe true
    JavaRuleSet.isBundledFor(BazelRelease(8, 0), emptyList()) shouldBe false
    // Ruleset-name autoload (e.g. --incompatible_autoload_externally=+rules_java)
    JavaRuleSet.isBundledFor(BazelRelease(8, 0), listOf("rules_python", "rules_java", "rules_android")) shouldBe true
    // Symbol-name autoload (e.g. --incompatible_autoload_externally=+JavaInfo)
    JavaRuleSet.isBundledFor(BazelRelease(8, 0), listOf("JavaInfo")) shouldBe true
    PythonRuleSet.isBundledFor(BazelRelease(8, 0), listOf("PyInfo")) shouldBe true
  }
}
