package org.jetbrains.bazel.golang.ui.gutters

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BazelGoRunLineMarkerContributorTest {
  @Test
  fun `should correctly regexify test filter`() {
    val regexified = regexifyTestFilter("Test/with/subtest(good \\ or \\ bad)")
    regexified shouldBe "^Test/with/subtest\\(good \\\\ or \\\\ bad\\)$"
  }
}
