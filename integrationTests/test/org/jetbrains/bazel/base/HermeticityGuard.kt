package org.jetbrains.bazel.base

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.process.ProcessInfo
import org.jetbrains.bazel.data.BazelProjectConfigurer
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * Fails an IDE Starter test that does not run in isolation from the other tests.
 *
 * Another test can change the project dir or the Bazel output base of a test. Then the result of the test depends on the test order
 * and on the other runs on the same host. Each context needs a project dir that no other context used,
 * and its own `--output_user_root` and `--output_base`.
 * The repository cache and the disk cache stay shared, because Bazel supports their concurrent use.
 *
 * The guard remembers the project dirs in memory, so it finds a reused project dir only in this JVM.
 * Other processes get other dirs by construction: each fixture copy is a new temp dir, and each Bazel root key contains the pid.
 * The guard also fails when another process wrote the `user.bazelrc` of a shared checkout.
 */
internal object HermeticityGuard {
  private val usedProjectHomes: MutableSet<Path> = ConcurrentHashMap.newKeySet()

  /**
   * A context that [ownsProjectDir] must use a project dir that no other context used.
   * A shared checkout, such as the Hirschgarten clone, is reused on purpose. It still needs its own Bazel roots.
   *
   * Returns the output base of [context]. Keep it for the teardown, because another context can later write the same `user.bazelrc`.
   */
  fun assertContextIsolated(context: IDETestContext, ownsProjectDir: Boolean): String {
    val projectHome = with(BazelProjectConfigurer) { context.resolvedBazelProjectHome }
    val outputBase = outputBase(projectHome)
    val violations = (if (ownsProjectDir) projectHomeViolations(projectHome) else emptyList()) +
                     bazelRootViolations(projectHome, BazelProjectConfigurer.registeredOutputBases())
    if (violations.isNotEmpty() || outputBase == null) {
      throw AssertionError(
        "The test context for $projectHome is not isolated from the other tests.\n" + violations.joinToString("\n") { "  - $it" },
      )
    }
    return outputBase
  }

  /** Remembers [projectHome]. Reports it when an earlier context in this JVM already used it. */
  fun projectHomeViolations(projectHome: Path): List<String> {
    val home = projectHome.toAbsolutePath().normalize()
    if (usedProjectHomes.add(home)) return emptyList()
    return listOf("The project dir was used by an earlier context. Copy the fixture to a dir that only this context uses.")
  }

  /**
   * Reports each startup root that the generated `user.bazelrc` in [projectHome] does not set.
   * Also reports an output base that is not in [registeredOutputBases]. Then another process wrote this `user.bazelrc`.
   */
  fun bazelRootViolations(projectHome: Path, registeredOutputBases: Set<String>): List<String> {
    val userBazelrc = projectHome.resolve(USER_BAZELRC)
    val missingRoots = listOf("--output_user_root", "--output_base").mapNotNull { flag ->
      if (startupOption(projectHome, flag) != null) null
      else "$userBazelrc has no 'startup $flag='. Bazel then uses a default root that other tests share."
    }
    val outputBase = outputBase(projectHome)
    if (outputBase == null || outputBase in registeredOutputBases) return missingRoots
    return missingRoots + "$userBazelrc sets the output base '$outputBase'. This JVM did not set it up, so another process wrote the file."
  }

  /** The output base that the `user.bazelrc` in [projectHome] sets. */
  fun outputBase(projectHome: Path): String? = startupOption(projectHome, "--output_base")

  /**
   * Fails when the teardown would stop a Bazel server that no context of the test owns.
   * [candidates] are the Bazel servers that the teardown is about to stop.
   */
  fun assertTeardownOwnsProcesses(ownedOutputBases: Set<String>, candidates: List<ProcessInfo>) {
    val violations = teardownViolations(ownedOutputBases, candidates)
    if (violations.isNotEmpty()) {
      throw AssertionError("The teardown does not stop only the processes of this test.\n" + violations.joinToString("\n") { "  - $it" })
    }
  }

  fun teardownViolations(ownedOutputBases: Set<String>, candidates: List<ProcessInfo>): List<String> =
    candidates.mapNotNull { candidate ->
      val outputBase = candidate.arguments.firstNotNullOfOrNull { it.removePrefix("--output_base=").takeIf { value -> value != it } }
      if (outputBase != null && normalize(outputBase) in ownedOutputBases) null
      else "The teardown stops the Bazel server ${candidate.pid} with the output base '$outputBase'. This test does not own it."
    }

  private fun startupOption(projectHome: Path, flag: String): String? {
    val userBazelrc = projectHome.resolve(USER_BAZELRC)
    if (!userBazelrc.exists()) return null
    return userBazelrc.readLines().firstNotNullOfOrNull { line ->
      line.trim().removePrefix("startup $flag=").takeIf { it != line.trim() }?.removeSurrounding("'")?.let(::normalize)
    }
  }

  /** The form in which the guard compares paths. */
  fun normalize(path: String): String = path.replace('\\', '/').trimEnd('/')

  private const val USER_BAZELRC = "user.bazelrc"
}
