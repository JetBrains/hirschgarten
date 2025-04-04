package org.jetbrains.bazel.target

import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.junit.jupiter.api.Test

class BuildTargetCapabilitiesConverterTest {
  @Test
  fun `should (de-)serialize the BuildTargetCapabilities correctly`() {
    val buildTargetCapabilities = BuildTargetCapabilities(canCompile = false, canTest = true, canRun = true)
    val converter = BuildTargetCapabilitiesConverter()
    buildTargetCapabilities shouldBe converter.fromString(converter.toString(buildTargetCapabilities))
  }
}
