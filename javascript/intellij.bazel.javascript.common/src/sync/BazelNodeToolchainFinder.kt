package org.jetbrains.bazel.javascript.sync

import com.intellij.util.system.CpuArch
import com.intellij.util.system.LowLevelLocalMachineAccess
import com.intellij.util.system.OS
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * A Node.js runtime downloaded by `rules_nodejs` into Bazel's output base.
 *
 * @param repositoryName canonical name of the external repository, e.g. `rules_nodejs++node+nodejs_darwin_arm64`
 * @param toolchainName name given to `node.toolchain(name = ...)`, `nodejs` by default
 * @param nodeBinary absolute path to the `node` executable inside the repository
 */
@ApiStatus.Internal
data class BazelNodeToolchain(
  val repositoryName: String,
  val toolchainName: String,
  val nodeBinary: Path,
)

/**
 * The `rules_nodejs` platform a Node.js runtime was downloaded for.
 *
 * @param repositorySuffix suffix `rules_nodejs` appends to the toolchain name to form the repository name, e.g. `darwin_arm64`
 * @param relativeNodeBinary location of the `node` executable inside the repository
 */
@ApiStatus.Internal
data class NodePlatform(
  val repositorySuffix: String,
  val relativeNodeBinary: String,
) {
  companion object {
    // Mirrors rules_nodejs `nodejs/repositories.bzl`: the archive is extracted to `bin/nodejs`
    private const val NODE_EXTRACT_DIR = "bin/nodejs"

    fun of(os: OS, cpuArch: CpuArch): NodePlatform? {
      val osName = when (os) {
        OS.macOS -> "darwin"
        OS.Linux -> "linux"
        OS.Windows -> "windows"
        else -> return null
      }
      val cpuName = when (cpuArch) {
        CpuArch.X86_64 -> "amd64"
        CpuArch.ARM64 -> "arm64"
        else -> return null
      }
      val relativeNodeBinary = if (os == OS.Windows) "$NODE_EXTRACT_DIR/node.exe" else "$NODE_EXTRACT_DIR/bin/node"
      return NodePlatform("${osName}_$cpuName", relativeNodeBinary)
    }

    // The runtime is registered as a local interpreter, so it has to match the machine the IDE runs on
    @OptIn(LowLevelLocalMachineAccess::class)
    fun host(): NodePlatform? = of(OS.CURRENT, CpuArch.CURRENT)
  }
}

/**
 * Finds the Node.js runtime `rules_nodejs` downloaded for [platform] into the `external` directory of Bazel's output base.
 *
 * `rules_nodejs` names its platform repositories `<toolchain name>_<platform>`, prefixed by the extension's canonical name
 * under Bzlmod (`rules_nodejs++node+nodejs_darwin_arm64` on Bazel 8, `rules_nodejs~~node~nodejs_darwin_arm64` on Bazel 7)
 * and unprefixed under WORKSPACE (`nodejs_darwin_arm64`).
 * When several toolchains were downloaded, the one with the default `nodejs` name wins.
 *
 * A repository only exists once Bazel fetched it, i.e., after a JS target was analyzed at least once.
 */
@ApiStatus.Internal
fun findBazelNodeToolchain(externalDir: Path, platform: NodePlatform): BazelNodeToolchain? {
  if (!externalDir.isDirectory()) return null
  val repositorySuffix = "_${platform.repositorySuffix}"
  return externalDir.listDirectoryEntries()
    .asSequence()
    .filter { it.name.endsWith(repositorySuffix) }
    .mapNotNull { repository ->
      val nodeBinary = repository.resolve(platform.relativeNodeBinary)
      if (!nodeBinary.isRegularFile()) return@mapNotNull null
      val toolchainName = repository.name.removeSuffix(repositorySuffix).substringAfterLast('+').substringAfterLast('~')
      BazelNodeToolchain(repository.name, toolchainName, nodeBinary)
    }
    .sortedWith(compareBy({ it.toolchainName != DEFAULT_TOOLCHAIN_NAME }, { it.repositoryName }))
    .firstOrNull()
}

private const val DEFAULT_TOOLCHAIN_NAME = "nodejs"
