package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import java.nio.file.Path

class JdkResolver(private val bazelPathsResolver: BazelPathsResolver) {
  fun resolve(targets: Sequence<TargetInfo>): Jdk? {
    val allCandidates = targets.mapNotNull { resolveJdkData(it) }.toList()
    if (allCandidates.none()) return null
    val bestJdk = allCandidates.sortByFrequency().maxBy { it.jdkType.priority }
    return Jdk(bestJdk.javaHome)
  }

  private fun resolveJdkData(targetInfo: TargetInfo): JdkCandidate? {
    return if (targetInfo.hasJavaToolchainInfo() && (targetInfo.javaToolchainInfo.hasBootClasspathJavaHome() || !targetInfo.hasJavaRuntimeInfo())) {
      val javaToolchainInfo = targetInfo.javaToolchainInfo
      val javaHome = if (javaToolchainInfo.hasBootClasspathJavaHome()) {
        javaToolchainInfo.bootClasspathJavaHome
      } else if (javaToolchainInfo.hasJavaHome()) {
        javaToolchainInfo.javaHome
      } else {
        null
      }

      JdkCandidate(
        jdkType = if (javaToolchainInfo.hasBootClasspathJavaHome()) JdkType.BOOT_CLASSPATH else JdkType.TOOLCHAIN,
        javaHome = javaHome?.let(bazelPathsResolver::resolve) ?: return null,
      )
    } else if (targetInfo.hasJavaRuntimeInfo()) {
      val javaRuntimeInfo = targetInfo.javaRuntimeInfo
      val javaHome = if (javaRuntimeInfo.hasJavaHome()) javaRuntimeInfo.javaHome else null

      JdkCandidate(
        jdkType = JdkType.RUNTIME,
        javaHome = javaHome?.let(bazelPathsResolver::resolve) ?: return null,
      )
    } else {
      null
    }
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
}
