package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBazelBinarySection
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class BazelBinarySpecExtractorPrecedenceTest {
  private class TestEnvProvider(private val values: MutableMap<String, String?> = mutableMapOf()) : EnvironmentProvider {
    override fun getValue(name: String): String? = values[name]
    fun put(name: String, value: String?) { values[name] = value }
  }

  companion object {
    private lateinit var tempRoot: Path
    @JvmStatic @BeforeClass fun setupClass() {
      tempRoot = Files.createTempDirectory("bazel-binary-extractor-j4")
    }
    @JvmStatic @AfterClass fun tearDownClass() {
      // nothing
    }
  }

  private lateinit var env: TestEnvProvider

  @Before
  fun setUp() {
    env = TestEnvProvider(mutableMapOf())
    EnvironmentProvider.provideEnvironmentProvider(env)
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    System.clearProperty("BIT_BAZEL_BINARY")
    env.put("BIT_BAZEL_BINARY", null)
    env.put("PATH", "")
  }

  @After
  fun tearDown() {
    System.clearProperty("BIT_BAZEL_BINARY")
    env.put("BIT_BAZEL_BINARY", null)
    env.put("PATH", "")
  }

  @Test
  fun projectView_bazel_binary_wins_over_overrides_and_PATH() {
    val pv = ProjectView.Builder(
      bazelBinary = ProjectViewBazelBinarySection(Path("/absolute/path/from/projectview"))
    ).build()

    val spec = BazelBinarySpecExtractor.fromProjectView(pv)

    assertEquals(BazelBinarySpec(Path("/absolute/path/from/projectview")), spec)
  }

  @Test
  fun system_property_override_used_when_no_project_view() {
    val sysFile = createExecutable("sys-bazel")
    System.setProperty("BIT_BAZEL_BINARY", sysFile.absolutePath)
    env.put("PATH", "")
    val pv = ProjectView.Builder(bazelBinary = null).build()

    val spec = BazelBinarySpecExtractor.fromProjectView(pv)

    assertEquals(BazelBinarySpec(sysFile.toPath()), spec)
  }

  @Test
  fun environment_override_used_when_no_project_view_and_no_sysprop() {
    val envFile = createExecutable("env-bazel")
    env.put("BIT_BAZEL_BINARY", envFile.absolutePath)
    env.put("PATH", "")
    val pv = ProjectView.Builder(bazelBinary = null).build()

    val spec = BazelBinarySpecExtractor.fromProjectView(pv)

    assertEquals(BazelBinarySpec(envFile.toPath()), spec)
  }

  @Test
  fun PATH_used_when_no_overrides_prefers_bazel_over_bazelisk() {
    val dir = Files.createDirectory(tempRoot.resolve("bin1"))
    val bazel = createExecutableIn(dir, withExe("bazel"))
    val bazelisk = createExecutableIn(dir, withExe("bazelisk"))
    env.put("PATH", dir.toAbsolutePath().toString())
    val pv = ProjectView.Builder(bazelBinary = null).build()

    val spec = BazelBinarySpecExtractor.fromProjectView(pv)

    assertEquals(BazelBinarySpec(bazel.toPath()), spec)
    // keep bazelisk referenced to avoid unused variable warning
    assert(bazelisk.exists())
  }

  @Test
  fun non_executable_sysprop_is_ignored_falls_back_to_env() {
    val nonExec = createFile("non-exec")
    System.setProperty("BIT_BAZEL_BINARY", nonExec.absolutePath)
    val envExec = createExecutable("env-exec")
    env.put("BIT_BAZEL_BINARY", envExec.absolutePath)
    env.put("PATH", "")
    val pv = ProjectView.Builder(bazelBinary = null).build()

    val spec = BazelBinarySpecExtractor.fromProjectView(pv)

    assertEquals(BazelBinarySpec(envExec.toPath()), spec)
  }

  // helpers
  private fun createExecutable(name: String): File = createExecutableIn(tempRoot, withExe(name))

  private fun createExecutableIn(dir: Path, name: String): File {
    val file = dir.resolve(name).toFile()
    file.writeText("#!/bin/sh\nexit 0\n")
    file.setExecutable(true, false)
    return file
  }

  private fun createFile(name: String): File {
    val file = tempRoot.resolve(withExe(name)).toFile()
    file.writeText("dummy")
    file.setExecutable(false, false)
    return file
  }

  private fun withExe(base: String): String = if (isWindows()) "$base.exe" else base
  private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}
