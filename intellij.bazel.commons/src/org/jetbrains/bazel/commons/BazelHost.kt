package org.jetbrains.bazel.commons

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.EelDescriptor
import com.intellij.platform.eel.EelOsFamily
import com.intellij.platform.eel.isWindows
import com.intellij.platform.eel.path.EelPath
import com.intellij.platform.eel.path.EelPathException
import com.intellij.platform.eel.pathSeparator
import com.intellij.platform.eel.provider.LocalEelDescriptor
import com.intellij.platform.eel.provider.asEelPath
import com.intellij.platform.eel.provider.asNioPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.openapi.application.PathManager
import com.intellij.platform.eel.provider.toEelApiBlocking
import com.intellij.platform.eel.provider.utils.EelPathUtils
import org.jetbrains.annotations.ApiStatus
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.toPath

private val LOG = logger<BazelHost>()

private val hostTempDirectories = ConcurrentHashMap<EelDescriptor, Path>()

/**
 * The environment where Bazel and the processes it starts run.
 *
 * This is the machine of the IDE for a local project.
 * It is another environment when the IDE opens a project of WSL, a Docker container or a similar environment.
 * Then the IDE and Bazel see the same file under different paths:
 * the IDE reads `\\wsl.localhost\Ubuntu\home\user\project`, and Bazel reads `/home/user/project`.
 *
 * A *host path* is a path in the form that Bazel reads and prints.
 * An *IDE path* is a [Path] that the IDE can use for file access.
 * Convert each path that crosses the border between the IDE and Bazel:
 * use [toIdePath] for a path that Bazel prints, and [toHostPath] for a path that the IDE passes to a process.
 *
 * Both conversions change nothing for a local project.
 */
@ApiStatus.Internal
class BazelHost(val descriptor: EelDescriptor) {
  /** `true` when Bazel runs on the machine of the IDE. */
  val isLocal: Boolean get() = descriptor === LocalEelDescriptor

  val osFamily: EelOsFamily get() = descriptor.osFamily

  val isWindows: Boolean get() = osFamily.isWindows

  /** The separator between the entries of a variable like `PATH` or `CLASSPATH`. */
  val pathSeparator: String get() = osFamily.pathSeparator

  /** The separator between the names of a host path. */
  val fileSeparator: String get() = if (isWindows) "\\" else "/"

  /** Returns the file name of the executable [name]: `<name>.exe` on Windows, and [name] on other systems. */
  fun executableName(name: String): String = if (isWindows) "$name.exe" else name

  /**
   * Converts an absolute host path to an IDE path.
   *
   * @throws java.nio.file.InvalidPathException or [EelPathException] if [hostPath] is not a valid absolute path of this host.
   */
  fun toIdePath(hostPath: String): Path =
    if (isLocal) Paths.get(hostPath) else EelPath.parse(hostPath, descriptor).asNioPath()

  /** Does the same as [toIdePath], but returns `null` if [hostPath] is not a valid absolute path of this host. */
  fun toIdePathOrNull(hostPath: String): Path? =
    try {
      toIdePath(hostPath).takeIf { it.isAbsolute }
    }
    catch (_: EelPathException) {
      null
    }
    catch (_: java.nio.file.InvalidPathException) {
      null
    }

  /**
   * Converts a path that Bazel prints or that a value of the plugin holds to an IDE path.
   * It accepts more forms than [toIdePath]: an absolute host path, a relative path, which stays relative,
   * and a path that is an IDE path already.
   * The last form appears when the plugin stores an IDE path in a value that usually holds a host path.
   */
  fun toIdePathAllowingRelative(hostPath: String): Path =
    if (isLocal || !isAbsoluteHostPath(hostPath) || isIdePathOfPosixHost(hostPath)) Paths.get(hostPath) else toIdePath(hostPath)

  /**
   * A Posix host has no path that starts with two separators.
   * But the IDE path of a file on such a host does, for example `\\wsl.localhost\Ubuntu\home`.
   */
  private fun isIdePathOfPosixHost(path: String): Boolean =
    !isWindows && (path.startsWith("\\\\") || path.startsWith("//"))

  /** Returns `true` if [hostPath] is an absolute path in the form of this host. */
  fun isAbsoluteHostPath(hostPath: String): Boolean =
    when {
      isLocal -> runCatching { Paths.get(hostPath).isAbsolute }.getOrDefault(false)
      isWindows -> hostPath.length >= 3 && hostPath[1] == ':' && (hostPath[2] == '\\' || hostPath[2] == '/') || hostPath.startsWith("\\\\")
      else -> hostPath.startsWith("/")
    }

  /**
   * Converts a `file:` URI that Bazel prints to an IDE path.
   */
  fun uriToIdePath(uri: String): Path {
    val parsed = URI.create(uri)
    if (isLocal) return parsed.toPath()
    val path = parsed.path
    // A Windows host prints `file:///C:/dir/file`, and the path of this URI is `/C:/dir/file`.
    val hostPath = if (isWindows && path.length >= 3 && path[0] == '/' && path[2] == ':') path.substring(1) else path
    return toIdePath(hostPath)
  }

  /**
   * Converts an IDE path to a host path.
   * A relative path stays relative, but gets the file separator of this host.
   *
   * A path that is not on this host has no host path.
   * Then this function reports a warning and returns the path as the IDE sees it.
   * The process on the host can probably not read it.
   */
  fun toHostPath(path: Path): String {
    if (isLocal) return path.toString()
    if (!path.isAbsolute) return if (isWindows) path.toString() else path.invariantSeparatorsPathString
    val eelPath = try {
      path.asEelPath()
    }
    catch (e: EelPathException) {
      LOG.warn("Cannot convert $path to a path on $descriptor", e)
      return path.toString()
    }
    if (eelPath.descriptor === LocalEelDescriptor) {
      LOG.warn("Path $path is not on $descriptor, a process there can probably not read it", Throwable())
      return path.toString()
    }
    return eelPath.toString()
  }

  /**
   * Returns a directory for temporary files that both the IDE and a process on this host can read and write.
   *
   * This is the temporary directory of the IDE for a local project.
   * For another host, this is a directory in the temporary directory of that host.
   * The first call for such a host connects to the host, so do not call this function on the event dispatch thread.
   * The plugin keeps one directory for each host until the IDE exits.
   */
  fun tempDirectory(): Path {
    if (isLocal) return PathManager.getTempDir().createDirectories()
    val cached = hostTempDirectories[descriptor]
    if (cached != null && Files.isDirectory(cached)) return cached
    return synchronized(hostTempDirectories) {
      hostTempDirectories[descriptor]?.takeIf { Files.isDirectory(it) }
      ?: EelPathUtils.createTemporaryDirectory(descriptor.toEelApiBlocking(), prefix = "bazel-plugin-", deleteOnExit = true)
        .also { hostTempDirectories[descriptor] = it }
    }
  }

  /**
   * Creates an empty temporary file in [tempDirectory].
   */
  fun createTempFile(prefix: String, suffix: String? = null): Path = Files.createTempFile(tempDirectory(), prefix, suffix)

  /**
   * Returns a host path where a process on this host can read the content of [path], which is a file or a directory.
   *
   * A path on this host converts as in [toHostPath].
   * A file of the IDE machine, for example an agent library of the IDE, is not visible to a process on another host.
   * So this function copies it to a temporary directory on the host first.
   * The IDE keeps the copy for later calls and copies again only what changed.
   * Do not call this function on the event dispatch thread, because it can copy files.
   */
  fun makeAvailable(path: Path): String {
    if (isLocal || !path.isAbsolute || path.getEelDescriptor() !== LocalEelDescriptor) return toHostPath(path)
    val copy = EelPathUtils.transferLocalContentToRemote(path, EelPathUtils.TransferTarget.Temporary(descriptor))
    return toHostPath(copy)
  }

  /** Returns `true` if [path] is a path on this host. */
  fun owns(path: Path): Boolean =
    if (isLocal) path.getEelDescriptor() === LocalEelDescriptor else path.isAbsolute && path.getEelDescriptor() == descriptor

  override fun equals(other: Any?): Boolean = other is BazelHost && other.descriptor == descriptor

  override fun hashCode(): Int = descriptor.hashCode()

  override fun toString(): String = "BazelHost($descriptor)"

  companion object {
    /** The machine of the IDE. */
    @JvmField
    val LOCAL: BazelHost = BazelHost(LocalEelDescriptor)

    /** Returns the host of the Bazel workspace of [project]. */
    @JvmStatic
    fun of(project: Project): BazelHost = of(project.getEelDescriptor())

    /** Returns the host that holds [path], for example the workspace root. */
    @JvmStatic
    fun of(path: Path): BazelHost = of(path.getEelDescriptor())

    @JvmStatic
    fun of(descriptor: EelDescriptor): BazelHost = if (descriptor === LocalEelDescriptor) LOCAL else BazelHost(descriptor)
  }
}

/** The host of the Bazel workspace of this project. See [BazelHost]. */
@get:ApiStatus.Internal
val Project.bazelHost: BazelHost get() = BazelHost.of(this)
