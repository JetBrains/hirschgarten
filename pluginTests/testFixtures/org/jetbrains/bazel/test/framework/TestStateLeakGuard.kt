package org.jetbrains.bazel.test.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.testFramework.junit5.impl.testApplication
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.sync.environment.projectCtx
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.exists
import kotlin.jvm.optionals.getOrNull
import kotlin.streams.asSequence

/**
 * Fails a test that leaves global state changed for the tests that run after it.
 *
 * Each [StateProbe] reads one kind of global state. [capture] reads the state before a test, and [assertNothingLeaked]
 * compares it with the state after the test. The guard does not restore a leaked state. The next test takes a new
 * snapshot, so one leak fails only the test that caused it.
 *
 * `@TestApplication` already tracks threads, SDKs, libraries and VFS pointers, so the probes do not repeat them.
 */
@TestOnly
internal class TestStateLeakGuard private constructor(private val checks: List<Pair<String, () -> List<String>>>) {
  fun assertNothingLeaked(testName: String) {
    val leaks = checks.flatMap { (probeName, check) -> check().map { "$probeName: $it" } }
    if (leaks.isNotEmpty()) {
      throw AssertionError(
        "$testName leaks global state into the next tests. Restore the state in a finally block or with a test disposable.\n" +
        leaks.joinToString("\n") { "  - $it" },
      )
    }
  }

  companion object {
    fun capture(probes: List<StateProbe>): TestStateLeakGuard = TestStateLeakGuard(probes.map { it.name to it.capture() })
  }
}

/** Reads one kind of global state for [TestStateLeakGuard]. */
@TestOnly
internal interface StateProbe {
  val name: String

  /** Reads the state now. Returns a check that reads the state again and describes each difference. */
  fun capture(): () -> List<String>
}

/**
 * Runs [TestStateLeakGuard] around each test class, and around each `@Nested` class.
 *
 * Register this extension before the other extensions. JUnit then calls its `afterAll` last, after the class fixtures
 * are torn down and after the other extensions restore their state.
 */
@TestOnly
internal class TestStateLeakGuardExtension : BeforeAllCallback, AfterAllCallback {
  override fun beforeAll(context: ExtensionContext) {
    // Start the application before the snapshot. Otherwise the properties that the startup sets count as a leak.
    context.testApplication().getOrThrow()
    context.getStore(NAMESPACE).put(context.uniqueId, TestStateLeakGuard.capture(listOf(SystemPropertiesProbe, BazelServerProbe)))
  }

  override fun afterAll(context: ExtensionContext) {
    val guard = context.getStore(NAMESPACE).remove(context.uniqueId, TestStateLeakGuard::class.java) ?: return
    guard.assertNothingLeaked(context.displayName)
  }

  private companion object {
    val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create(TestStateLeakGuardExtension::class.java)
  }
}

/**
 * Detects a system property that had a value before a test, and that the test changes or clears and does not restore.
 *
 * The probe ignores a property that is new. The platform sets some properties once, on the first use of a subsystem, and
 * keeps them on purpose, for example `jna.platform.library.path` and `idea.vendor.name`. A test cannot restore them.
 */
@TestOnly
internal object SystemPropertiesProbe : StateProbe {
  override val name: String = "system property"

  override fun capture(): () -> List<String> {
    val before = read()
    return { describeChanges(before, read().filterKeys { it in before }) }
  }

  private fun read(): Map<String, String?> {
    val properties = System.getProperties()
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
  }
}

/**
 * Detects a Bazel server that a test starts and does not stop.
 *
 * The probe counts only a server whose workspace a test registers with [registerWorkspace], so the server of another
 * process does not count. On Windows, the JDK cannot read the arguments of another process, so the probe finds no server.
 */
@TestOnly
internal object BazelServerProbe : StateProbe {
  private const val WORKSPACE_ARGUMENT = "--workspace_directory="
  private const val EXIT_TIMEOUT_SECONDS = 10L

  private val workspaces: MutableSet<Path> = ConcurrentHashMap.newKeySet()

  override val name: String = "Bazel server"

  /** Marks [workspaceRoot] as a test workspace. */
  fun registerWorkspace(workspaceRoot: Path) {
    val absolute = workspaceRoot.toAbsolutePath().normalize()
    workspaces.add(absolute)
    // The server reports the real path, for example /private/var instead of /var on macOS.
    if (absolute.exists()) {
      workspaces.add(absolute.toRealPath())
    }
  }

  override fun capture(): () -> List<String> {
    val before = liveServers().keys.map { it.pid() }.toSet()
    return {
      liveServers()
        .filter { (process, _) -> process.pid() !in before && !process.exitsSoon() }
        .map { (process, workspace) -> "the server ${process.pid()} of $workspace still runs. Stop it, for example with stopBazelServer." }
    }
  }

  private fun liveServers(): Map<ProcessHandle, Path> {
    if (workspaces.isEmpty()) return emptyMap()
    return ProcessHandle.allProcesses().asSequence()
      .mapNotNull { process ->
        val workspace = process.info().arguments().getOrNull()
                          ?.firstOrNull { it.startsWith(WORKSPACE_ARGUMENT) }
                          ?.let { Path.of(it.removePrefix(WORKSPACE_ARGUMENT)).normalize() }
                        ?: return@mapNotNull null
        if (workspace in workspaces) process to workspace else null
      }
      .toMap()
  }

  // `bazel shutdown` can return before the server process is gone.
  private fun ProcessHandle.exitsSoon(): Boolean =
    try {
      onExit().get(EXIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      true
    }
    catch (_: TimeoutException) {
      false
    }
}

/**
 * Detects Bazel state that a test leaves on the light project.
 *
 * The light project stays open between test classes, so the next light test sees this state.
 */
@TestOnly
internal class LightProjectBazelStateProbe(private val project: Project) : StateProbe {
  override val name: String = "light project"

  override fun capture(): () -> List<String> {
    val before = read()
    return { describeChanges(before, read()) }
  }

  private fun read(): Map<String, String?> {
    val storeDescriptor = project.stateStore.storeDescriptor
    val context = project.projectCtx
    val versionCache = project.service<BazelVersionCheckerService>().state
    return mapOf(
      "isBazelProject" to project.isBazelProject.toString(),
      "store descriptor" to "${storeDescriptor.javaClass.simpleName}(${storeDescriptor.historicalProjectBasePath})",
      "Bazel release" to context.bazelRelease?.toString(),
      "workspace name" to context.workspaceName,
      "bazel-bin path" to context.bazelBinPath?.toString(),
      "execution root" to context.bazelExecPath?.toString(),
      "current Bazel version in BazelVersionCheckerService" to versionCache.currentBazelVersion,
      "latest Bazel version in BazelVersionCheckerService" to versionCache.latestBazelVersion,
    )
  }
}

private fun describeChanges(before: Map<String, String?>, after: Map<String, String?>): List<String> =
  (before.keys + after.keys).sorted().mapNotNull { key ->
    val old = before[key]
    val new = after[key]
    when {
      old == new -> null
      old == null -> "$key is set to '$new'. It was not set before."
      new == null -> "$key is not set. It was '$old' before."
      else -> "$key is '$new'. It was '$old' before."
    }
  }
