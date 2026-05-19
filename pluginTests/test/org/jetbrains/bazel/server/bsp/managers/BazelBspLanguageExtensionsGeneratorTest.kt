package org.jetbrains.bazel.server.bsp.managers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BazelBspLanguageExtensionsGeneratorTest {
  internal class BazelExternalRulesetsQueryMock(private val rulesetNames: List<String>) : BazelExternalRulesetsQuery {
    override suspend fun fetchExternalRulesetNames(): List<String> = rulesetNames
  }

  private val noAutoloads : List<String> = listOf()
  private val fullAutoloads = listOf("rules_python", "rules_java", "rules_android")

  @Test
  fun `should treat protobuf as bundled only before bazel 8`() {
    Language.Protobuf.isBundledFor(BazelRelease(6, 4), noAutoloads) shouldBe true
    Language.Protobuf.isBundledFor(BazelRelease(7, 4), noAutoloads) shouldBe true
    Language.Protobuf.isBundledFor(BazelRelease(8, 0), noAutoloads) shouldBe false
    Language.Protobuf.isBundledFor(BazelRelease(9, 0), noAutoloads) shouldBe false
    // Protobuf has no autoloadHints, so it never falls back to bundled even when autoloaded
    Language.Protobuf.isBundledFor(BazelRelease(8, 0), listOf("protobuf")) shouldBe false
  }

  @Test
  fun `should treat java as bundled before bazel 8 and via autoloads on bazel 8`() {
    Language.Java.isBundledFor(BazelRelease(7, 4), noAutoloads) shouldBe true
    Language.Java.isBundledFor(BazelRelease(8, 0), noAutoloads) shouldBe false
    // Ruleset-name autoload (e.g. --incompatible_autoload_externally=+rules_java)
    Language.Java.isBundledFor(BazelRelease(8, 0), fullAutoloads) shouldBe true
    // Symbol-name autoload (e.g. --incompatible_autoload_externally=+JavaInfo)
    Language.Java.isBundledFor(BazelRelease(8, 0), listOf("JavaInfo")) shouldBe true
    Language.Python.isBundledFor(BazelRelease(8, 0), listOf("PyInfo")) shouldBe true
  }
}
