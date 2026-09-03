package org.jetbrains.bazel.sync.workspace.languages.python

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import org.jetbrains.bazel.python.debug.PythonDebugUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PythonLanguagePluginTest {
  @Test
  fun `python target args are extracted from aspect target info`() {
    val target =
      IntellijIdeInfo.TargetIdeInfo.newBuilder()
        .setPyIdeInfo(
          IntellijIdeInfo.PyIdeInfo.newBuilder()
            .addAllArgs(listOf("--flag", "two words")),
        ).build()

    assertEquals(listOf("--flag", "two words"), PythonDebugUtils.extractPythonTargetArgs(target))
  }
}
