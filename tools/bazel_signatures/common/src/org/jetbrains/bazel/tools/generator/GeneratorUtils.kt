package org.jetbrains.bazel.tools.generator

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream

private const val PLUGIN_SIGNATURES_RELATIVE_PATH = "plugins/bazel/intellij.bazel.core/resources/bazelSignatures"

fun resolveRepo(repoPath: String, tempDirPrefix: String): Path {
  if (repoPath.startsWith("https://") || repoPath.startsWith("git@") || repoPath.endsWith(".git")) {
    val tempDir = createTempDirectory(tempDirPrefix)
    println()
    println("# Cloning $repoPath into $tempDir")
    runCommand(tempDir.parent, "git", "clone", repoPath, tempDir.toString())
    return tempDir
  }

  val localPath = Path(repoPath)
  if (localPath.isDirectory()) {
    require(localPath.resolve(".git").exists()) { "Not a git repository: $localPath" }
    return localPath
  }

  error("Invalid repo path: '$repoPath'")
}

fun gitCheckout(repoDir: Path, ref: String) {
  println()
  println("# Checking out '$ref'")
  runCommand(repoDir, "git", "checkout", ref)
}

fun gitResetHard(repoDir: Path) {
  println()
  println("# Resetting repository")
  runCommand(repoDir, "git", "reset", "--hard")
}

fun gitApplyPatch(repoDir: Path, patchResourcePath: String) {
  println("# Applying patch")
  val patchStream = object {}.javaClass.getResourceAsStream(patchResourcePath)
                    ?: error("Patch resource not found: $patchResourcePath")

  patchStream.use { input ->
    val tempPatch = createTempFile("bazel-patch-", ".patch")
    try {
      tempPatch.outputStream().use { output -> input.copyTo(output) }
      runCommand(repoDir, "git", "apply", "--3way", tempPatch.toString())
    }
    finally {
      tempPatch.deleteIfExists()
    }
  }
}

fun bazeliskBuild(repoDir: Path, vararg args: String) {
  println()
  println("# Building ${args.filterNot { it.startsWith("--") }.joinToString(", ")}")
  runCommand(repoDir, "bazelisk", "build", *args)
}

fun resolveBazelPluginSignaturesDir(): Path {
  val workspaceDir = resolveIntelliJWorkspaceDir()
  val signaturesDir = workspaceDir.resolve(PLUGIN_SIGNATURES_RELATIVE_PATH)
  require(signaturesDir.exists()) { "Signatures directory not found: $signaturesDir" }
  return signaturesDir
}

fun resolveIntelliJWorkspaceDir(): Path {
  val envDir = System.getenv("BUILD_WORKSPACE_DIRECTORY") ?: error("BUILD_WORKSPACE_DIRECTORY is not set.")
  return Path(envDir)
}

fun runCommand(workDir: Path, vararg command: String) {
  println("## Executing: ${command.joinToString(" ")}")
  val process = ProcessBuilder(*command)
    .directory(workDir.toFile())
    .inheritIO()
    .start()
  val exitCode = process.waitFor()
  if (exitCode != 0) {
    error("'${command.joinToString(" ")}' failed with exit code $exitCode")
  }
}

@OptIn(ExperimentalPathApi::class)
fun cleanupIfTempClone(repoDir: Path, tempDirPrefix: String) {
  val systemTempDir = Path(System.getProperty("java.io.tmpdir"))
  if (!repoDir.startsWith(systemTempDir)) return
  if (!repoDir.fileName.toString().startsWith(tempDirPrefix)) return
  println()
  println("# Cleaning up temporary clone: $repoDir")
  repoDir.deleteRecursively()
}

fun String.compareAsVersions(other: String): Int {
  val left = split('.').map { it.toIntOrNull() ?: 0 }
  val right = other.split('.').map { it.toIntOrNull() ?: 0 }
  val maxLen = maxOf(left.size, right.size)
  for (i in 0 until maxLen) {
    val cmp = (left.getOrElse(i) { 0 }).compareTo(right.getOrElse(i) { 0 })
    if (cmp != 0) return cmp
  }
  return 0
}
