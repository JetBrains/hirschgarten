package org.jetbrains.bazel.data

import org.jetbrains.bazel.test.framework.serializeBazelRcPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

internal class BazelTestFixtureConfigTest {
  @Test
  fun `project declarations require exact revisions`() {
    val project = simpleBazelProject(
      repositoryUrl = "https://example.invalid/fixtures.git",
      revision = "0123456789abcdef0123456789abcdef01234567",
      branch = "main",
      path = "simpleJavaTest",
    )

    assertEquals("simpleJavaTest", project.projectPath)
    listOf("0123456", "0123456789ABCDEF0123456789ABCDEF01234567", "g123456789abcdef0123456789abcdef01234567")
      .forEach { revision ->
        assertThrows(IllegalArgumentException::class.java) {
          simpleBazelProject(revision = revision, path = "simpleJavaTest")
        }
      }
  }

  @Test
  fun `project declarations default the fixture repository and branch`() {
    val project = simpleBazelProject(
      revision = "0123456789abcdef0123456789abcdef01234567",
      path = "nested/project",
    )

    assertEquals("https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git", project.repositoryUrl)
    assertEquals("main", project.branch)
    assertEquals("nested/project", project.projectPath)
  }

  @Test
  fun `nested Bazel uses short isolated roots only in Windows Bazel tests`(@TempDir projectRoot: Path) {
    val bazelTestEnvironment = mapOf(
      "TEST_TMPDIR" to "deep-test-output-root",
      "LOCALAPPDATA" to "Z:\\Users\\builder\\AppData\\Local",
      USE_BAZEL_VERSION_ENV to "7.5.0",
    )
    val userHome = "Z:\\Users\\builder"
    val windowsOptions = BazelProjectConfigurer.nestedBazelStartupOptions(
      projectRoot,
      bazelTestEnvironment,
      IdeStarterOs.WINDOWS,
      userHome,
      invocationId = "invocation-1",
    )

    assertEquals(4, windowsOptions.size)
    assertEquals("startup --batch", windowsOptions[0])
    assertEquals(
      listOf("u", "i", "b"),
      windowsOptions.drop(1).map { option ->
        checkNotNull(Regex("'Z:/Users/builder/ijr/[0-9a-f]{16}/([uib])'").find(option)).groupValues[1]
      },
    )
    assertEquals(
      1,
      windowsOptions.drop(1).map { option ->
        checkNotNull(Regex("/([0-9a-f]{16})/[uib]'").find(option)).groupValues[1]
      }.distinct().size,
    )
    assertTrue(windowsOptions[1].startsWith("startup --output_user_root="))
    assertTrue(windowsOptions[2].startsWith("startup --install_base="))
    assertTrue(windowsOptions[3].startsWith("startup --output_base="))
    assertTrue(windowsOptions.none { it.contains("deep-test-output-root") })
    assertEquals(
      emptyList<String>(),
      BazelProjectConfigurer.nestedBazelStartupOptions(projectRoot, bazelTestEnvironment, IdeStarterOs.LINUX),
    )
    assertEquals(
      emptyList<String>(),
      BazelProjectConfigurer.nestedBazelStartupOptions(projectRoot, emptyMap(), IdeStarterOs.WINDOWS),
    )
    assertEquals(
      windowsOptions,
      BazelProjectConfigurer.nestedBazelStartupOptions(
        projectRoot,
        bazelTestEnvironment,
        IdeStarterOs.WINDOWS,
        userHome,
        invocationId = "invocation-1",
      ),
    )
    assertNotEquals(
      windowsOptions,
      BazelProjectConfigurer.nestedBazelStartupOptions(
        projectRoot.resolve("other-project"),
        bazelTestEnvironment,
        IdeStarterOs.WINDOWS,
        userHome,
        invocationId = "invocation-1",
      ),
    )
    val otherVersionOptions = BazelProjectConfigurer.nestedBazelStartupOptions(
      projectRoot,
      bazelTestEnvironment + (USE_BAZEL_VERSION_ENV to "8.3.1"),
      IdeStarterOs.WINDOWS,
      userHome,
      invocationId = "invocation-1",
    )
    assertNotEquals(windowsOptions[1], otherVersionOptions[1])
    assertNotEquals(windowsOptions[2], otherVersionOptions[2])
    assertNotEquals(windowsOptions[3], otherVersionOptions[3])
    listOf(
      bazelTestEnvironment - "LOCALAPPDATA",
      bazelTestEnvironment + ("LOCALAPPDATA" to "Y:\\different\\location"),
    ).forEach { environment ->
      assertEquals(
        windowsOptions,
        BazelProjectConfigurer.nestedBazelStartupOptions(
          projectRoot,
          environment,
          IdeStarterOs.WINDOWS,
          userHome,
          invocationId = "invocation-1",
        ),
      )
    }

    val threadCount = 32
    val ready = CountDownLatch(threadCount)
    val start = CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(threadCount)
    val parallelRoots = try {
      val futures = (1..threadCount).map {
        executor.submit<String> {
          ready.countDown()
          start.await()
          BazelProjectConfigurer.nestedBazelStartupOptions(
            projectRoot,
            bazelTestEnvironment,
            IdeStarterOs.WINDOWS,
            userHome,
          )[2]
        }
      }
      ready.await()
      start.countDown()
      futures.map { it.get() }
    } finally {
      executor.shutdownNow()
    }
    assertEquals(parallelRoots.size, parallelRoots.toSet().size)

    val sequentialRoots = (1..64).map {
      BazelProjectConfigurer.nestedBazelStartupOptions(
        projectRoot,
        bazelTestEnvironment,
        IdeStarterOs.WINDOWS,
        userHome,
      )[2]
    }
    assertEquals(sequentialRoots.size, sequentialRoots.toSet().size)
    assertTrue(sequentialRoots.none { it == windowsOptions[2] })
  }

  @Test
  fun `generated settings go to user bazelrc and leave a committed import intact`(@TempDir projectRoot: Path) {
    val bazelrc = projectRoot.resolve(".bazelrc")
    val committed = "build --keep_going\ntry-import %workspace%/user.bazelrc\n"
    bazelrc.writeText(committed)

    BazelProjectConfigurer.writeGeneratedBazelSettings(
      projectRoot,
      listOf("startup --install_base='C:/Users/builder/ijr/first/i'"),
    )
    assertEquals(committed, bazelrc.toFile().readText())
    assertEquals(
      "startup --install_base='C:/Users/builder/ijr/first/i'\n",
      projectRoot.resolve("user.bazelrc").toFile().readText(),
    )

    BazelProjectConfigurer.writeGeneratedBazelSettings(
      projectRoot,
      listOf("startup --install_base='C:/Users/builder/ijr/second/i'"),
    )
    assertEquals(committed, bazelrc.toFile().readText())
    val regenerated = projectRoot.resolve("user.bazelrc").toFile().readText()
    assertFalse(regenerated.contains("ijr/first"))
    assertEquals("startup --install_base='C:/Users/builder/ijr/second/i'\n", regenerated)
  }

  @Test
  fun `user bazelrc import is appended once when the project does not commit it`(@TempDir projectRoot: Path) {
    val bazelrc = projectRoot.resolve(".bazelrc")
    bazelrc.writeText("build --keep_going")

    BazelProjectConfigurer.writeGeneratedBazelSettings(projectRoot, listOf("common --disk_cache='/tmp/cache'"))
    val afterFirst = bazelrc.toFile().readText()
    assertEquals("build --keep_going\ntry-import %workspace%/user.bazelrc\n", afterFirst)

    BazelProjectConfigurer.writeGeneratedBazelSettings(projectRoot, listOf("common --disk_cache='/tmp/cache'"))
    assertEquals(afterFirst, bazelrc.toFile().readText())

    val bare = projectRoot.resolve("bare").createDirectories()
    BazelProjectConfigurer.writeGeneratedBazelSettings(bare, listOf("common --disk_cache='/tmp/cache'"))
    assertEquals("try-import %workspace%/user.bazelrc\n", bare.resolve(".bazelrc").toFile().readText())
  }

  @Test
  fun `user bazelrc import is moved after settings that would override generated options`(@TempDir projectRoot: Path) {
    val bazelrc = projectRoot.resolve(".bazelrc")
    bazelrc.writeText(
      "try-import %workspace%/user.bazelrc\n" +
        "common --disk_cache=/committed/cache\n" +
        "# trailing comment\n",
    )

    BazelProjectConfigurer.writeGeneratedBazelSettings(projectRoot, listOf("common --disk_cache='/generated/cache'"))
    val relocated = bazelrc.toFile().readText()
    assertEquals(
      "common --disk_cache=/committed/cache\n" +
        "# trailing comment\n" +
        "try-import %workspace%/user.bazelrc\n",
      relocated,
    )

    BazelProjectConfigurer.writeGeneratedBazelSettings(projectRoot, listOf("common --disk_cache='/generated/cache'"))
    assertEquals(relocated, bazelrc.toFile().readText())
  }

  @Test
  fun `registered Windows roots are isolated and cleaned after sequential use`(@TempDir tempDir: Path) {
    val environment = mapOf(
      "TEST_TMPDIR" to "deep-test-output-root",
      USE_BAZEL_VERSION_ENV to "8.5.1",
    )
    val projectRoot = tempDir.resolve("project").createDirectories()
    val userHome = tempDir.resolve("home").createDirectories().toString()

    val firstInstallBase = BazelProjectConfigurer.nestedBazelStartupOptions(
      projectRoot,
      environment,
      IdeStarterOs.WINDOWS,
      userHome,
      registerCleanup = true,
    )[2].substringAfter('=').removeSurrounding("'").let(Path::of)
    firstInstallBase.resolve("embedded_tools").createDirectories()
      .resolve("MODULE.bazel").writeText("module(name = \"bazel_tools\")")
    Files.walk(firstInstallBase).use { paths ->
      paths.sorted(Comparator.reverseOrder()).forEach(Files::delete)
    }

    val secondInstallBase = BazelProjectConfigurer.nestedBazelStartupOptions(
      projectRoot,
      environment,
      IdeStarterOs.WINDOWS,
      userHome,
      registerCleanup = true,
    )[2].substringAfter('=').removeSurrounding("'").let(Path::of)
    secondInstallBase.resolve("embedded_tools").createDirectories()
      .resolve("MODULE.bazel").writeText("module(name = \"bazel_tools\")")

    assertNotEquals(firstInstallBase, secondInstallBase)
    assertTrue(secondInstallBase.resolve("embedded_tools/MODULE.bazel").exists())
    BazelProjectConfigurer.cleanupWindowsNestedBazelRoots()
    assertFalse(secondInstallBase.exists())
  }

  @Test
  fun `downloader config flag honors the matrix Bazel version override`(@TempDir projectRoot: Path) {
    projectRoot.resolve(".bazelversion").writeText("7.4.1\n")
    assertEquals(
      "experimental_downloader_config",
      BazelProjectConfigurer.resolveDownloaderConfigFlag(projectRoot, emptyMap()),
    )
    assertEquals(
      "downloader_config",
      BazelProjectConfigurer.resolveDownloaderConfigFlag(projectRoot, mapOf(USE_BAZEL_VERSION_ENV to "9.0.0")),
    )

    projectRoot.resolve(".bazelversion").writeText("8.5.1\n")
    assertEquals(
      "downloader_config",
      BazelProjectConfigurer.resolveDownloaderConfigFlag(projectRoot, emptyMap()),
    )
    assertEquals(
      "experimental_downloader_config",
      BazelProjectConfigurer.resolveDownloaderConfigFlag(projectRoot, mapOf(USE_BAZEL_VERSION_ENV to "7.6.1")),
    )

    val withoutVersionFile = projectRoot.resolve("bare").createDirectories()
    assertEquals(
      "experimental_downloader_config",
      BazelProjectConfigurer.resolveDownloaderConfigFlag(withoutVersionFile, emptyMap()),
    )
  }

  @Test
  fun `cache settings quote Bazelrc paths`() {
    val repositoryCache = Path.of("repository cache")
    val diskCache = Path.of("disk cache")

    assertEquals(
      "common --repository_cache=${repositoryCache.toAbsolutePath().toBazelRcPathForTest()}",
      BazelProjectConfigurer.bazelCacheSetting("repository_cache", repositoryCache),
    )
    assertEquals(
      "common --disk_cache=${diskCache.toAbsolutePath().toBazelRcPathForTest()}",
      BazelProjectConfigurer.bazelCacheSetting("disk_cache", diskCache),
    )
  }

  @Test
  fun `Windows cache paths use forward slashes in Bazelrc`() {
    assertEquals(
      "'Z:/BuildAgent/system/.persistent_cache/ide-starter cache'",
      serializeBazelRcPath("Z:\\BuildAgent\\system\\.persistent_cache\\ide-starter cache"),
    )
  }

  private fun Path.toBazelRcPathForTest(): String =
    serializeBazelRcPath(toString())
}
