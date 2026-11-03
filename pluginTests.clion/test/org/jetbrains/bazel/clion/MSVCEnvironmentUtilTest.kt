package org.jetbrains.bazel.clion

import com.intellij.openapi.Disposable
import com.intellij.testFramework.TestModeFlags
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.disposableFixture
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.workspace.environment.MSVCEnvironmentUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path

@EnabledOnOs(OS.WINDOWS)
@TestApplication
internal class MSVCEnvironmentUtilTest {
  private val testDisposable: Disposable by disposableFixture()

  private fun setBazelVC(value: String?) {
    TestModeFlags.set(MSVCEnvironmentUtil.BAZEL_VC_KEY, value, testDisposable)
  }

  @Test
  fun getToolSetPath_usesBazelVC() {
    setBazelVC("C:\\Program Files\\Microsoft Visual Studio\\2022\\Community\\VC")

    assertThat(MSVCEnvironmentUtil.getToolSetPath(Path.of("")))
      .isEqualTo("C:\\Program Files\\Microsoft Visual Studio\\2022\\Community")
    assertThat(MSVCEnvironmentUtil.getToolSetPath(null))
      .isEqualTo("C:\\Program Files\\Microsoft Visual Studio\\2022\\Community")
  }

  @Test
  fun getToolSetPath_emptyBazelVC() {
    setBazelVC("")

    assertThat(MSVCEnvironmentUtil.getToolSetPath(Path.of(""))).isNull()
    assertThat(MSVCEnvironmentUtil.getToolSetPath(null)).isNull()
  }

  @Test
  fun getToolSetPath_fromCompilerPath() {
    setBazelVC("")

    val path = "C:\\Program Files\\Microsoft Visual Studio\\2022\\Community\\VC\\Tools\\MSVC\\14.40.33807\\bin\\Hostx64\\x64\\cl.exe"

    assertThat(MSVCEnvironmentUtil.getToolSetPath(Path.of(path)))
      .isEqualTo("c:\\program files\\microsoft visual studio\\2022\\community")
  }
}
