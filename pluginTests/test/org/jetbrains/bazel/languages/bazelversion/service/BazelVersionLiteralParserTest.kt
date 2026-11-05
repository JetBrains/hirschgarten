package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.util.text.SemVer
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test

class BazelVersionLiteralParserTest {
  @Test
  fun `test official version`() {
    BazelVersionLiteral.fromString("8.2.1")!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("8.2.1")!!)
  }

  @Test
  fun `test official version with suffix`() {
    BazelVersionLiteral.fromString("8.2.1-rc1")!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("8.2.1-rc1")!!)
  }

  @Test
  fun `test forked version`() {
    BazelVersionLiteral.fromString("JetBrains/8.2.1-jb_20250522_59")!! shouldBeEqual
      BazelVersionLiteral.Forked(
        "JetBrains",
        BazelVersionLiteral.Specific(SemVer.parseFromText("8.2.1-jb_20250522_59")!!),
      )
  }

  @Test
  fun `test wildcard version with *`() {
    BazelVersionLiteral.fromString("4.*")!! shouldBeEqual BazelVersionLiteral.Other("4.*")
  }

  @Test
  fun `test wildcard version with +`() {
    BazelVersionLiteral.fromString("4.+")!! shouldBeEqual BazelVersionLiteral.Other("4.+")
  }

  @Test
  fun `test special cases`() {
    BazelVersionLiteral.fromString("latest")!! shouldBeEqual BazelVersionLiteral.Latest(0)
    BazelVersionLiteral.fromString("latest-2")!! shouldBeEqual BazelVersionLiteral.Latest(2)
    BazelVersionLiteral.fromString("last_green")!! shouldBeEqual BazelVersionLiteral.Special.LAST_GREEN
    BazelVersionLiteral.fromString("last_rc")!! shouldBeEqual BazelVersionLiteral.Special.LAST_RC
    BazelVersionLiteral.fromString("rolling")!! shouldBeEqual BazelVersionLiteral.Special.ROLLING
  }
}
