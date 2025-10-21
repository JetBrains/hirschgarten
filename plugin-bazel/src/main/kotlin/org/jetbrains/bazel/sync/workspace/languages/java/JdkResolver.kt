package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.config.BazelFeatureFlags
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
      ?.let { version -> candidates.filter { it.sourceVersion == version } }
      .orEmpty()

  private fun findLatestVersion(candidates: Sequence<JdkCandidate>): String? =
    candidates.mapNotNull { it.sourceVersion }.maxByOrNull { Integer.parseInt(it) }

  private fun pickCandidateFromJvmRuntime(candidates: Sequence<JdkCandidate>) = candidates.find { it.isRuntime }

  private fun pickAnyCandidate(candidates: Sequence<JdkCandidate>): JdkCandidate? = candidates.firstOrNull()

  private fun resolveJdkData(targetInfo: TargetInfo): JdkCandidateData? {
    val jdkCandidateProviders = mutableListOf(::javaRuntimeInfoJdkCandidateProvider)
    if (BazelFeatureFlags.isJdkResolverToolchainFirst) {
      jdkCandidateProviders.add(0, ::javaToolchainInfoJdkCandidateProvider)
    } else {
      jdkCandidateProviders.add(::javaToolchainInfoJdkCandidateProvider)
    }
    return jdkCandidateProviders.firstNotNullOfOrNull { it(targetInfo, bazelPathsResolver) }
  }

  private inner class JdkCandidate(private val data: JdkCandidateData) {
    val sourceVersion: String?
      get() =
        data.sourceVersion ?: javaHome
          ?.takeIf { it.exists() }
          ?.let(jdkVersionResolver::resolve)
          ?.toString()

    val targetVersion: String?
      get() = data.targetVersion

    val javaHome by data::javaHome
    val isRuntime by data::isRuntime

    val isComplete: Boolean
      get() = javaHome != null && sourceVersion != null

    fun asJdk(): Jdk? =
      sourceVersion?.let {
        Jdk(it, javaHome)
      }
  }

  private data class JdkCandidateData(
    val isRuntime: Boolean,
    val javaHome: Path?,
    val sourceVersion: String? = null,
    val targetVersion: String? = null,
  )

  private fun <A> Sequence<A>.sortByFrequency(): Sequence<A> =
    groupBy { it }
      .values
      .sortedByDescending { it.size }
      .map { it.first() }
      .asSequence()

  companion object {
    private fun javaRuntimeInfoJdkCandidateProvider(targetInfo: TargetInfo, bazelPathsResolver: BazelPathsResolver): JdkCandidateData? {
      if (targetInfo.hasJavaRuntimeInfo()) {
        val javaRuntimeInfo = targetInfo.javaRuntimeInfo
        val javaHome = if (javaRuntimeInfo.hasJavaHome()) javaRuntimeInfo.javaHome else null

        JdkCandidateData(
          isRuntime = true,
          javaHome = javaHome?.let(bazelPathsResolver::resolve),
        )
      }
      return null
    }

    private fun javaToolchainInfoJdkCandidateProvider(targetInfo: TargetInfo, bazelPathsResolver: BazelPathsResolver): JdkCandidateData? {
      val javaToolchainInfo = targetInfo.javaToolchainInfo
      val javaHome = if (javaToolchainInfo.hasJavaHome()) javaToolchainInfo.javaHome else null

      JdkCandidateData(
        isRuntime = false,
        javaHome = javaHome?.let(bazelPathsResolver::resolve),
        sourceVersion = javaToolchainInfo.sourceVersion,
        targetVersion = javaToolchainInfo.targetVersion,
      )
      return null
    }
  }
}
