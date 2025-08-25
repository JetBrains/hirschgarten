package org.jetbrains.bazel.util

import com.intellij.testFramework.utils.io.createFile
import org.jetbrains.bazel.commons.EnvironmentProvider
import java.nio.file.Path

class MockBazelBinaryGenerator(private val workspaceRoot: Path) {
  fun generateAndGetProvider(): EnvironmentProvider {
    createFakeExecutable()
    return EnvironmentPathProvider(workspaceRoot.toString())
  }

  private fun createFakeExecutable(): Path =
    workspaceRoot.createFile("bazel").also {
      it.toFile().setExecutable(true)
    }

  private class EnvironmentPathProvider(private val environmentPathValue: String) : EnvironmentProvider {
    override fun getValue(name: String): String? =
      if (name == "PATH") {
        environmentPathValue
      } else {
        null
      }
  }
}
