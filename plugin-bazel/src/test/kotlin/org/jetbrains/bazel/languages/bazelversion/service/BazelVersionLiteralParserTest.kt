package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.util.text.SemVer
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionLiteral
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test

class BazelVersionLiteralParserTest {
  @Test
  fun `test official version`() {
    "8.2.1".toBazelVersionLiteral()!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("8.2.1")!!)
  }

  @Test
  fun `test official version with suffix`() {
    "8.2.1-rc1".toBazelVersionLiteral()!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("8.2.1-rc1")!!)
  }

  @Test
  fun `test forked version`() {
    "JetBrains/8.2.1-jb_20250522_59".toBazelVersionLiteral()!! shouldBeEqual
      BazelVersionLiteral.Forked(
        "JetBrains",
        BazelVersionLiteral.Specific(SemVer.parseFromText("8.2.1-jb_20250522_59")!!),
      )
  }

  @Test
  fun `test wildcard version with *`() {
    "4.*".toBazelVersionLiteral()!! shouldBeEqual BazelVersionLiteral.Other("4.*")
  }

  @Test
  fun `test wildcard version with +`() {
    "4.+".toBazelVersionLiteral()!! shouldBeEqual BazelVersionLiteral.Other("4.+")
  }

  @Test
  fun `test special cases`() {
    "last_green".toBazelVersionLiteral()!! shouldBeEqual BazelVersionLiteral.Special.LAST_GREEN
    "last_rc".toBazelVersionLiteral()!! shouldBeEqual BazelVersionLiteral.Special.LAST_RC
    "rolling".toBazelVersionLiteral()!! shouldBeEqual BazelVersionLiteral.Special.ROLLING
  }
}
