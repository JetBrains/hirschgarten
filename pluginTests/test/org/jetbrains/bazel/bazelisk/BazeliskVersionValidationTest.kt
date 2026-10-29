package org.jetbrains.bazel.bazelisk

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BazeliskVersionValidationTest {
  @ParameterizedTest
  @ValueSource(strings = ["1.26.0", "0.0.1", "10.20.30", "1.26.0-rc1", "1.26.0-rc.1"])
  fun `accepts release versions`(version: String) {
    assertTrue(isValidBazeliskVersion(version))
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "",
      "1.26",
      "v1.26.0",
      "latest",
      "1.26.0/../../../tmp/evil",
      "../1.26.0",
      "1.26.0-..",
      "1.26.0-rc1/x",
      "1.26.0\\..\\x",
      "1.26.0 ",
      "1.26.0\n1.27.0",
    ],
  )
  fun `rejects other values`(version: String) {
    assertFalse(isValidBazeliskVersion(version))
  }
}
