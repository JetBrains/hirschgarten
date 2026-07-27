package org.jetbrains.bazel.tests.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class HirschgartenUpgradeConfigurationTest {

  @Test
  fun `Windows configuration keeps compatible modules and excludes unsupported formatting target`(@TempDir projectHome: Path) {
    projectHome.resolve("MODULE.bazel").writeText(
      """
      module(name = "hirschgarten")
      bazel_dep(name = "rules_apple", version = "3.20.1")
      single_version_override(
          module_name = "rules_apple",
          version = "3.17.1",
      )
      """.trimIndent(),
    )
    projectHome.resolve("tools/intellij").toFile().mkdirs()
    projectHome.resolve("tools/intellij/.managed.bazelproject").writeText(
      """
      derive_targets_from_directories: true

      targets:
        //commons/...
        $HIRSCHGARTEN_FORMAT_TARGET

      directories:
        .
      """.trimIndent(),
    )
    projectHome.resolve("projectview.bazelproject").writeText("targets:\n  //...")
    projectHome.resolve(".bazelrc").writeText("common --color=yes\n")
    projectHome.resolve("MODULE.bazel.lock").writeText("stale lock")

    configureHirschgartenForWindows(projectHome, Path.of("C:/Program Files/Git/bin/bash.exe"))

    val module = projectHome.resolve("MODULE.bazel").readText()
    assertTrue("bazel_dep(name = \"rules_apple\", version = \"3.20.1\")" in module)
    assertTrue("module_name = \"rules_apple\"" in module)
    assertTrue("version = \"3.17.1\"" in module)
    assertTrue("module_name = \"rules_buf\"" in module)
    assertTrue("version = \"0.5.2\"" in module)
    assertTrue("module_name = \"toolchain_utils\"" in module)
    assertTrue("patches = [\"//:$WINDOWS_TOOLCHAIN_UTILS_PATCH_FILE\"]" in module)
    assertFalse(projectHome.resolve("MODULE.bazel.lock").exists())

    val projectView = projectHome.resolve("projectview.bazelproject").readText()
    assertFalse(HIRSCHGARTEN_FORMAT_TARGET in projectView)
    assertFalse("derive_targets_from_directories: true" in projectView)
    assertTrue("derive_targets_from_directories: false" in projectView)
    assertTrue("//commons/..." in projectView)
    assertTrue("directories:\n  ." in projectView)
    assertTrue(HIRSCHGARTEN_FORMAT_TARGET in projectHome.resolve("tools/intellij/.managed.bazelproject").readText())

    val bazelRc = projectHome.resolve(".bazelrc").readText()
    assertTrue("common --color=yes" in bazelRc)
    assertTrue("common --legacy_external_runfiles" in bazelRc)
    assertTrue("common --shell_executable='C:/Program Files/Git/bin/bash.exe'" in bazelRc)

    val patch = projectHome.resolve(WINDOWS_TOOLCHAIN_UTILS_PATCH_FILE).readText()
    assertTrue("${'$'}WarningPreference = 'SilentlyContinue'; Get-Package" in patch)
    assertFalse("splitlines()" in patch)
    assertEquals(1, patch.lineSequence().count { it.startsWith("-") && !it.startsWith("---") })
    assertEquals(1, patch.lineSequence().count { it.startsWith("+") && !it.startsWith("+++") })
  }
}
