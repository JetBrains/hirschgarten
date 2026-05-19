package org.jetbrains.bazel.sync.workspace.languages.java

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.projectRoots.impl.JavaHomeFinder
import com.intellij.platform.eel.provider.LocalEelDescriptor
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.Label
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

internal class JdkResolver(private val bazelPathsResolver: BazelPathsResolver) {
  fun resolve(targets: Map<Label, TargetIdeInfo>, repoMapping: RepoMapping): Jdk? {
    val localRepositories = repoMapping.getLocalRepositories()
    val allCandidates = targets.values.mapNotNull { resolveJdkData(it, localRepositories, targets) }.toList()
    if (allCandidates.none()) return localJdkFallback()
    val bestJdk = allCandidates.sortByFrequency().maxBy { it.jdkType.priority }
    return Jdk(bestJdk.javaHome)
  }

  private fun resolveJdkData(
    targetInfo: TargetIdeInfo,
    localRepositories: LocalRepositoryMapping,
    targets: Map<Label, TargetIdeInfo>,
  ): JdkCandidate? {
    val javaToolchainInfo = if (targetInfo.hasJavaToolchainInfo()) targetInfo.javaToolchainInfo else null
    val (javaHome, jdkType) = when {
        javaToolchainInfo != null && javaToolchainInfo.hasBootClasspathJavaHome() -> javaToolchainInfo.bootClasspathJavaHome to JdkType.BOOT_CLASSPATH
        javaToolchainInfo != null && javaToolchainInfo.hasJavaHome() -> javaToolchainInfo.javaHome to JdkType.TOOLCHAIN
        else -> targetInfo.depsList
        .asSequence()
        .mapNotNull { Label.parseOrNull(it.target.label) }
        .mapNotNull { targets[it] }
        .find { it.javaToolchainInfo.hasJavaHome() }
        ?.let { it.javaToolchainInfo.javaHome to JdkType.RUNTIME }
        ?: return null
    }
    return JdkCandidate(
          jdkType = jdkType,
          javaHome = javaHome.let { bazelPathsResolver.resolve(it, localRepositories) }.takeIf { it.exists() } ?: return null,
        )
  }

  private data class JdkCandidate(
    val jdkType: JdkType,
    val javaHome: Path,
  )

  /**
   * We can get the JDK from aspect in different ways, some of which are preferable to others.
   */
  enum class JdkType(val priority: Int) {
    /**
     * The actual JDK used during compilation.
     * It is the same as [RUNTIME] unless `--@rules_java//java:incompatible_language_version_bootclasspath` is added to `.bazelrc`, see
     * [rules_java commit](https://github.com/bazelbuild/rules_java/commit/449303e723185a8197794c42711602e66b1a9296)
     * This is not available if built-in Bazel rules are used instead of rules_java (Bazel 7 or older).
     */
    BOOT_CLASSPATH(3),

    /**
     * Runtime JDK, set via, e.g., `common --java_runtime_version=remotejdk_17` in `.bazelrc`.
     * This is used for running `java_binary` targets, but also usually used for compilation (see [BOOT_CLASSPATH]).
     * The provider used (JavaRuntimeInfo) is only be returned by `java_binary` targets, so this won't work in a project which only
     * contains `java_library`.
     */
    RUNTIME(2),

    /**
     * On Bazel 7 or older with no runtime JDK set explicitly, this is the going to be used
     */
    TOOLCHAIN(1),
  }

  private fun <A> List<A>.sortByFrequency(): List<A> =
    groupBy { it }
      .values
      .sortedByDescending { it.size }
      .map { it.first() }

  private fun localJdkFallback(): Jdk? {
    /**
     * From: [JavaHomeFinder.JdkEntry.compareTo] docs:
     * "An entry should appear before another one if it has a **more recent** version or a shorter path."
     */
    val jdkWithNewestVersion = JavaHomeFinder.findJdks(LocalEelDescriptor, false).minOrNull() ?: return null
    return Jdk(Path(jdkWithNewestVersion.path))
  }
}
