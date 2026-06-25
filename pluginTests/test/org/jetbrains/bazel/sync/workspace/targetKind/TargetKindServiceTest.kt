package org.jetbrains.bazel.sync.workspace.targetKind

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.ExecutableInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.JavaCommonInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.KotlinTargetInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass.Companion.JAVA
import org.jetbrains.bazel.commons.LanguageClass.Companion.KOTLIN
import org.jetbrains.bazel.commons.RuleType.BINARY
import org.jetbrains.bazel.commons.RuleType.LIBRARY
import org.jetbrains.bazel.commons.RuleType.TEST
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.workspace.mapper.AspectBazelProjectMapper
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test

@BazelTestApplication
class TargetKindServiceTest: WorkspaceModelBaseTest() {
  private val service = TargetKindService.getInstance()

  // -- fromTargetInfo ------------------------------------------------------

  @Test
  fun `fromTargetInfo resolves known kind`() {
    val target = targetInfo("java_library")
    guessTargetKind(target) shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromTargetInfo resolves known kind with transition prefix`() {
    val target = targetInfo("_transition_java_library")
    guessTargetKind(target) shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromTargetInfo infers library for unknown jvm kind when not executable`() {
    val target = targetInfo("custom_lib", executable = false, jvmTarget = true)
    guessTargetKind(target) shouldBe TargetKind("custom_lib", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromTargetInfo infers Kotlin JVM languages from providers`() {
    val target = targetInfo("custom_macro_library", executable = false, jvmTarget = true, kotlinTarget = true)
    guessTargetKind(target) shouldBe TargetKind("custom_macro_library", setOf(JAVA, KOTLIN), LIBRARY)
  }

  @Test
  fun `fromTargetInfo infers test for unknown executable jvm kind ending with _test`() {
    val target = targetInfo("custom_test", executable = true, jvmTarget = true)
    guessTargetKind(target) shouldBe TargetKind("custom_test", setOf(JAVA), TEST)
  }

  @Test
  fun `fromTargetInfo infers binary for unknown executable jvm kind`() {
    val target = targetInfo("custom_bin", executable = true, jvmTarget = true)
    guessTargetKind(target) shouldBe TargetKind("custom_bin", setOf(JAVA), BINARY)
  }

  @Test
  fun `fromTargetInfo falls back to library for unknown non-executable kind`() {
    val target = targetInfo("totally_unknown", executable = false)
    guessTargetKind(target) shouldBe TargetKind("totally_unknown", emptySet(), LIBRARY)
  }

  @Test
  fun `fromTargetInfo falls back to test for unknown executable kind ending with _test`() {
    val target = targetInfo("totally_unknown_test", executable = true)
    guessTargetKind(target) shouldBe TargetKind("totally_unknown_test", emptySet(), TEST)
  }

  @Test
  fun `fromTargetInfo falls back to binary for unknown executable kind`() {
    val target = targetInfo("totally_unknown_bin", executable = true)
    guessTargetKind(target) shouldBe TargetKind("totally_unknown_bin", emptySet(), BINARY)
  }

  // -- fromRuleName --------------------------------------------------------

  @Test
  fun `fromRuleName returns kind for known rule`() {
    service.findPredefinedRule("java_library") shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromRuleName returns null for unknown rule`() {
    service.findPredefinedRule("totally_unknown").shouldBeNull()
  }

  @Test
  fun `fromRuleName strips transition prefix`() {
    service.findPredefinedRule("_transition_java_library") shouldBe TargetKind("java_library", setOf(JAVA), LIBRARY)
  }

  @Test
  fun `fromRuleName returns null for unknown rule with transition prefix`() {
    service.findPredefinedRule("_transition_totally_unknown").shouldBeNull()
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

  private fun guessTargetKind(target: TargetIdeInfo): TargetKind =
    AspectBazelProjectMapper.inferTargetKind(target)
}

private fun targetInfo(
  kind: String,
  executable: Boolean = false,
  jvmTarget: Boolean = false,
  kotlinTarget: Boolean = false,
): TargetIdeInfo {
  val builder =
    TargetIdeInfo.newBuilder()
      .setKind(kind)
      .apply {
        if (executable) {
          setExecutableInfo(ExecutableInfo.getDefaultInstance())
        }
        if (jvmTarget) {
          javaCommon = JavaCommonInfo.newBuilder().setJvmTarget(true).build()
        }
        if (kotlinTarget) {
          kotlinTargetInfo = KotlinTargetInfo.getDefaultInstance()
        }
      }
  return builder.build()
}
