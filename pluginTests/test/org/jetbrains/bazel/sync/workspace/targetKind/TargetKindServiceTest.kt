package org.jetbrains.bazel.sync.workspace.targetKind

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jetbrains.bazel.commons.LanguageClass.JAVA
import org.jetbrains.bazel.commons.RuleType.BINARY
import org.jetbrains.bazel.commons.RuleType.LIBRARY
import org.jetbrains.bazel.commons.RuleType.TEST
import org.jetbrains.bazel.commons.RuleType.UNKNOWN
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.JavaCommonInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
class TargetKindServiceTest {
  private val service = TargetKindService.getInstance()

  // -- fromTargetInfo ------------------------------------------------------

  @Test
  fun `fromTargetInfo resolves known kind`() {
    val target = targetInfo("java_library")
    service.fromTargetInfo(target) shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromTargetInfo resolves known kind with transition prefix`() {
    val target = targetInfo("_transition_java_library")
    service.fromTargetInfo(target) shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromTargetInfo infers library for unknown jvm kind when not executable`() {
    val target = targetInfo("custom_lib", executable = false, jvmTarget = true)
    service.fromTargetInfo(target) shouldBe TargetKind("custom_lib", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromTargetInfo infers test for unknown executable jvm kind ending with _test`() {
    val target = targetInfo("custom_test", executable = true, jvmTarget = true)
    service.fromTargetInfo(target) shouldBe TargetKind("custom_test", setOf(JAVA), TEST)
  }

  @Test
  fun `fromTargetInfo infers binary for unknown executable jvm kind`() {
    val target = targetInfo("custom_bin", executable = true, jvmTarget = true)
    service.fromTargetInfo(target) shouldBe TargetKind("custom_bin", setOf(JAVA), BINARY)
  }

  @Test
  fun `fromTargetInfo falls back to library for unknown non-executable kind`() {
    val target = targetInfo("totally_unknown", executable = false)
    service.fromTargetInfo(target) shouldBe TargetKind("totally_unknown", emptySet(), LIBRARY)
  }

  @Test
  fun `fromTargetInfo falls back to test for unknown executable kind ending with _test`() {
    val target = targetInfo("totally_unknown_test", executable = true)
    service.fromTargetInfo(target) shouldBe TargetKind("totally_unknown_test", emptySet(), TEST)
  }

  @Test
  fun `fromTargetInfo falls back to binary for unknown executable kind`() {
    val target = targetInfo("totally_unknown_bin", executable = true)
    service.fromTargetInfo(target) shouldBe TargetKind("totally_unknown_bin", emptySet(), BINARY)
  }

  // -- fromRuleName --------------------------------------------------------

  @Test
  fun `fromRuleName returns kind for known rule`() {
    service.fromRuleName("java_library") shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromRuleName returns null for unknown rule`() {
    service.fromRuleName("totally_unknown").shouldBeNull()
  }

  @Test
  fun `fromRuleName strips transition prefix`() {
    service.fromRuleName("_transition_java_library") shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromRuleName returns null for unknown rule with transition prefix`() {
    service.fromRuleName("_transition_totally_unknown").shouldBeNull()
  }

  // -- cacheIfNecessary ----------------------------------------------------

  @Test
  fun `cacheIfNecessary returns existing kind when already cached`() {
    val existing = service.fromRuleName("java_library")!!
    val duplicate = TargetKind("java_library", emptySet(), UNKNOWN)
    service.cacheIfNecessary(duplicate) shouldBeSameInstanceAs existing
  }

  @Test
  fun `cacheIfNecessary caches and returns new kind`() {
    val newKind = TargetKind("my_unique_rule_for_test", setOf(JAVA), LIBRARY)
    service.cacheIfNecessary(newKind) shouldBeSameInstanceAs newKind
    service.fromRuleName("my_unique_rule_for_test") shouldBeSameInstanceAs newKind
  }

  // -- guessFromRuleName ---------------------------------------------------

  @Test
  fun `guessFromRuleName returns known kind`() {
    service.guessFromRuleName("java_test") shouldBe TargetKind("java_test", setOf(JAVA), TEST)
  }

  @Test
  fun `guessFromRuleName guesses test for suffix _test`() {
    service.guessFromRuleName("foo_test") shouldBe TargetKind("foo_test", emptySet(), TEST)
  }

  @Test
  fun `guessFromRuleName guesses test for test_suite suffix`() {
    service.guessFromRuleName("custom_test_suite") shouldBe TargetKind("custom_test_suite", emptySet(), TEST)
  }

  @Test
  fun `guessFromRuleName guesses test for test_suites suffix`() {
    service.guessFromRuleName("custom_test_suites") shouldBe TargetKind("custom_test_suites", emptySet(), TEST)
  }

  @Test
  fun `guessFromRuleName resolves known kind with transition prefix`() {
    service.guessFromRuleName("_transition_java_binary") shouldBe TargetKind("java_binary", setOf(JAVA), BINARY)
  }

  @Test
  fun `guessFromRuleName guesses binary for suffix _binary`() {
    service.guessFromRuleName("foo_binary") shouldBe TargetKind("foo_binary", emptySet(), BINARY)
  }

  @Test
  fun `guessFromRuleName defaults to library for unrecognized suffix`() {
    service.guessFromRuleName("foo_whatever") shouldBe TargetKind("foo_whatever", emptySet(), LIBRARY)
  }
}

private fun targetInfo(
  kind: String,
  executable: Boolean = false,
  jvmTarget: Boolean = false,
): TargetInfo =
  TargetInfo.newBuilder()
    .setKind(kind)
    .apply {
      if (executable) {
        setExecutableInfo(BspTargetInfo.ExecutableInfo.getDefaultInstance())
      }
      if (jvmTarget) {
        javaCommon = JavaCommonInfo.newBuilder().setJvmTarget(true).build()
      }
    }
    .build()
