package org.jetbrains.bazel.jvm.run

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelHost
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

private val LOG = logger<BazelHost>()

private const val JAVA_AGENT_PREFIX = "-javaagent:"
private const val NATIVE_AGENT_PREFIX = "-agentpath:"
private const val JAR_SUFFIX = ".jar"

/**
 * Changes the VM options that the IDE computed for its own machine so that a JVM on [host] can use them.
 * The function changes nothing for the machine of the IDE.
 *
 * The IDE adds agents to the JVM that it starts: the debugger agent, the coroutine agent, a coverage agent or a profiler.
 * Each option holds the path of the agent as the IDE sees it.
 * A JVM in another environment, for example in WSL, cannot read that path.
 *
 * - A Java agent (`-javaagent:<jar>[=<options>]`) is a JAR, and a JAR works on every system.
 *   The function copies the JAR to the host, or converts the path if the JAR is on the host already.
 * - A native agent (`-agentpath:`) of the IDE is a library for the system of the IDE.
 *   It cannot load into a JVM of another system, so the function removes the option and reports a warning.
 *
 * Do not call this function on the event dispatch thread, because it can copy files.
 */
@ApiStatus.Internal
fun adaptVmOptionsForHost(host: BazelHost, vmOptions: List<String>): List<String> {
  if (host.isLocal) return vmOptions
  return vmOptions.mapNotNull { option ->
    when {
      option.startsWith(JAVA_AGENT_PREFIX) -> adaptJavaAgent(host, option)
      option.startsWith(NATIVE_AGENT_PREFIX) -> {
        LOG.warn("The native agent of '$option' cannot load into a JVM on $host, the option is removed")
        null
      }
      else -> option
    }
  }
}

private fun adaptJavaAgent(host: BazelHost, option: String): String {
  val value = option.removePrefix(JAVA_AGENT_PREFIX)
  // The options of the agent follow the first `=` after the path of the JAR. A Windows path cannot hold `=`, but a Posix path can.
  val jarEnd = value.indexOf("$JAR_SUFFIX=").takeIf { it >= 0 }?.plus(JAR_SUFFIX.length) ?: value.length
  val jar = value.substring(0, jarEnd)
  val agentOptions = value.substring(jarEnd)
  val jarPath = try {
    Path.of(jar)
  }
  catch (_: InvalidPathException) {
    return option
  }
  if (!jarPath.isAbsolute || !Files.isRegularFile(jarPath)) return option
  return JAVA_AGENT_PREFIX + host.makeAvailable(jarPath) + agentOptions
}
