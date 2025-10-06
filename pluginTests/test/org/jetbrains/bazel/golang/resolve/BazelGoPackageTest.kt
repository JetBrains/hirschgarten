package org.jetbrains.bazel.golang.resolve

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.testing.IS_IN_IDE_STARTER_TEST
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.invariantSeparatorsPathString

/**
 * TODO: test [toRealFile] on Windows
 * BAZEL-2231 Test Go support on Windows
 */
class BazelGoPackageTest {
  @Test
  fun `should correctly shorten external path`() {
    val originalPath =
      "/private/var/tmp/_bazel/5dac6b1ab7f3c3b055628efa7ab248d6/execroot/_main/_tmp/external/gazelle++go_deps+com_github_pmezard_go_difflib/difflib/difflib.go"
    val expectedPath =
      "/private/var/tmp/_bazel/5dac6b1ab7f3c3b055628efa7ab248d6/external/gazelle++go_deps+com_github_pmezard_go_difflib/difflib/difflib.go"
    toRealFile(Paths.get(originalPath)).invariantSeparatorsPathString shouldBe expectedPath
  }

  @Test
  fun `should not shorten external path if in IDE Starter Test`() {
    System.setProperty(IS_IN_IDE_STARTER_TEST, "true")
    val originalPath =
      "/private/var/tmp/_bazel/5dac6b1ab7f3c3b055628efa7ab248d6/execroot/_main/_tmp/external/gazelle++go_deps+com_github_pmezard_go_difflib/difflib/difflib.go"
    toRealFile(Paths.get(originalPath)).invariantSeparatorsPathString shouldBe originalPath
    System.clearProperty(IS_IN_IDE_STARTER_TEST)
  }
}
