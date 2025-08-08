package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import java.nio.file.Path
import kotlin.io.path.exists

class JdkResolver(private val bazelPathsResolver: BazelPathsResolver, private val jdkVersionResolver: JdkVersionResolver) {
  fun resolve(targets: Sequence<TargetInfo>): Jdk? {
    val allCandidates = targets.mapNotNull { resolveJdkData(it) }.sortByFrequency().map { JdkCandidate(it) }
    if (allCandidates.none()) return null
    val latestVersion = candidatesWithLatestVersion(allCandidates)
    val complete = allCandidates.filter { it.isComplete }
    val latestVersionAndComplete = latestVersion.filter { it.isComplete }
    return (
      pickCandidateFromJvmRuntime(latestVersionAndComplete)
        ?: pickAnyCandidate(latestVersionAndComplete)
        ?: pickCandidateFromJvmRuntime(complete)
        ?: pickAnyCandidate(complete)
        ?: pickAnyCandidate(allCandidates)
    )?.asJdk()
  }

  fun resolveJdk(target: TargetInfo): Jdk? = resolveJdkData(target)?.let { JdkCandidate(it).asJdk() }

  private fun candidatesWithLatestVersion(candidates: Sequence<JdkCandidate>): Sequence<JdkCandidate> =
    findLatestVersion(candidates)
      ?.let { version -> candidates.filter { it.version == version } }
      .orEmpty()

  private fun findLatestVersion(candidates: Sequence<JdkCandidate>): String? =
    candidates.mapNotNull { it.version }.maxByOrNull { Integer.parseInt(it) }

  private fun pickCandidateFromJvmRuntime(candidates: Sequence<JdkCandidate>) = candidates.find { it.isRuntime }

  private fun pickAnyCandidate(candidates: Sequence<JdkCandidate>): JdkCandidate? = candidates.firstOrNull()

  private fun resolveJdkData(targetInfo: TargetInfo): JdkCandidateData? {
    val hasRuntimeJavaHome =
      targetInfo.hasJavaRuntimeInfo() &&
        targetInfo.javaRuntimeInfo.hasJavaHome()
    val hasToolchainJavaHome =
      targetInfo.hasJavaToolchainInfo() &&
        targetInfo.javaToolchainInfo.hasJavaHome()

    val javaHomeFile =
      if (hasRuntimeJavaHome) {
        targetInfo.javaRuntimeInfo.javaHome
      } else if (hasToolchainJavaHome) {
        targetInfo.javaToolchainInfo.javaHome
      } else {
        null
      }
    val javaHome = javaHomeFile?.let { bazelPathsResolver.resolve(it) }

    return JdkCandidateData(hasRuntimeJavaHome, javaHome)
      .takeIf { javaHome != null }
  }

  private inner class JdkCandidate(private val data: JdkCandidateData) {
    val version =
      data.javaHome
        ?.takeIf { it.exists() }
        ?.let { jdkVersionResolver.resolve(it) }
        ?.toString()
    val javaHome by data::javaHome
    val isRuntime by data::isRuntime
    val isComplete = javaHome != null && version != null

    fun asJdk(): Jdk? = version?.let { Jdk(it, javaHome) }
  }

  private data class JdkCandidateData(val isRuntime: Boolean, val javaHome: Path?)

  private fun <A> Sequence<A>.sortByFrequency(): Sequence<A> =
    groupBy { it }
      .values
      .sortedByDescending { it.size }
      .map { it.first() }
      .asSequence()
}
