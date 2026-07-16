package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.projectRoots.impl.JavaHomeFinder
import com.intellij.platform.eel.provider.LocalEelDescriptor
import org.jetbrains.bazel.sync.workspace.languages.java.Jdk
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

internal class JdkResolver(
  private val allTargets: Map<WorkspaceTargetKey, WorkspaceTarget>,
  private val ideJavaHomeOverride: Path? = null,
) {
  fun resolve(): Jdk? {
    ideJavaHomeOverride?.let { return Jdk(javaHome = it) }
    val allCandidates = allTargets.values.mapNotNull { resolveJdkData(it) }.toList()
    if (allCandidates.none()) return localJdkFallback()
    val bestJdk = allCandidates.sortByFrequency().maxBy { it.jdkType.priority }
    return Jdk(bestJdk.javaHome)
  }

  private fun resolveJdkData(target: WorkspaceTarget): JdkCandidate? {
    val toolchain = target.findBuildData<JavaToolchainData>()
    val bootClasspath = toolchain?.bootClasspathJavaHome
    val toolchainHome = toolchain?.javaHome
    val (javaHome, jdkType) = when {
      bootClasspath != null -> bootClasspath to JdkType.BOOT_CLASSPATH
      toolchainHome != null -> toolchainHome to JdkType.TOOLCHAIN
      else -> {
        val runtimeHome = target.rawBuildTarget.dependencies.asSequence()
                            .mapNotNull { allTargets[it.targetKey] }
                            .firstNotNullOfOrNull { it.findBuildData<JavaToolchainData>()?.javaHome } ?: return null
        runtimeHome to JdkType.RUNTIME
      }
    }
    return JdkCandidate(jdkType = jdkType, javaHome = javaHome.takeIf { it.exists() } ?: return null)
  }

  private data class JdkCandidate(val jdkType: JdkType, val javaHome: Path)

  /**
   * We can get the JDK from aspect in different ways, some of which are preferable to others.
   */
  enum class JdkType(val priority: Int) {
    /** The actual JDK used during compilation (rules_java bootclasspath). */
    BOOT_CLASSPATH(3),

    /** Runtime JDK (e.g. `--java_runtime_version`); only surfaced by `java_binary` targets. */
    RUNTIME(2),

    /** Bazel 7 or older with no runtime JDK set explicitly. */
    TOOLCHAIN(1),
  }

  private fun <A> List<A>.sortByFrequency(): List<A> = groupBy { it }.values.sortedByDescending { it.size }.map { it.first() }

  private fun localJdkFallback(): Jdk? {
    /**
     * From: [JavaHomeFinder.JdkEntry.compareTo] docs:
     * "An entry should appear before another one if it has a **more recent** version or a shorter path."
     */
    val jdkWithNewestVersion = JavaHomeFinder.findJdks(LocalEelDescriptor, false)
      .minOrNull() ?: return null
    return Jdk(Path(jdkWithNewestVersion.path))
  }

}
