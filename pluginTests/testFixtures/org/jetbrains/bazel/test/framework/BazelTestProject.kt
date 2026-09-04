package org.jetbrains.bazel.test.framework

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.refreshVfs
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories

/**
 * Lays out a Bazel test project on disk and makes it visible to the platform.
 *
 * The copy uses a plain file copy and a VFS refresh, so it does not need a code-insight test fixture.
 */
internal object BazelTestProject {
  /**
   * Copies the base project, then the test project at [path], into [projectRoot].
   *
   * [path] is relative to [projectsRoot]. The base project always comes from the shared
   * [BazelPathManager.testProjectsRoot], and it is copied first, so a file from [path] overwrites a base
   * file with the same name.
   *
   * It also writes the Bazel settings and caches, refreshes the VFS, and waits for the indexes of
   * [project]. When [jvmToolchains] is set, it adds the JVM toolchains: the Java runtime and the Kotlin
   * standard library. A pure C++ project does not need them.
   */
  fun copy(
    project: Project,
    projectRoot: Path,
    path: String,
    projectsRoot: Path = BazelPathManager.testProjectsRoot,
    jvmToolchains: Boolean = true,
  ) {
    LOG.info("Copying the test project $path into $projectRoot (jvmToolchains=$jvmToolchains)")
    copyDir(BazelPathManager.testProjectsRoot.resolve("base"), projectRoot)
    BazelTestCaches.setupBazelRc(projectRoot, jvmToolchains)
    copyDir(projectsRoot.resolve(path), projectRoot)
    BazelTestCaches.configureBazelCaches(projectRoot, path)
    if (jvmToolchains) {
      LOG.info("Adding the JVM toolchains")
      BazelTestCaches.findKotlinStdlibInClasspath()
        .copyTo(projectRoot.resolve("toolchains").resolve("kotlin-stdlib.jar").createParentDirectories())
    }

    LOG.info("Refreshing the VFS of $projectRoot")
    projectRoot.refreshVfs()
    IndexingTestUtil.waitUntilIndexesAreReady(project)
  }

  // FileUtil.copyDir is the same recursive copy the platform CodeInsightTestFixture uses; it needs java.io.File.
  @Suppress("SSBasedInspection")
  private fun copyDir(from: Path, to: Path) {
    FileUtil.copyDir(from.toFile(), to.toFile())
  }

  private val LOG = logger<BazelTestProject>()
}
