package org.jetbrains.bazel.test.framework

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.google.protobuf.util.JsonFormat
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.DigestUtil
import com.intellij.util.io.bytesToHex
import com.intellij.util.io.createDirectories
import com.intellij.util.io.createParentDirectories
import com.intellij.util.io.outputStream
import org.jetbrains.bazel.bazelrunner.BazelCommandExecutionDescriptor
import org.jetbrains.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.DefaultBazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.WeakHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class BazelInvocationCache(
  private val workspaceRoot: Path,
  private val bspInfo: BspInfo,
  private val aspectsResolver: InternalAspectsResolver,
  private val bazelInfoResolver: BazelInfoResolver,
) : BazelProcessLauncher {
  companion object {
    private const val INVOCATION_CACHE_DIR: String = "bazel_invocation_cache"
    const val INVOCATION_CACHE_DIR_PATH: String = "${Constants.DOT_BAZELBSP_DIR_NAME}/$INVOCATION_CACHE_DIR"
    private const val STDOUT_FILE = "stdout.out"
    private const val BEP_EVENTS_FILE = "bep_events.out"
    private const val OUTPUT_BASE_DIR = "output_base"
    private const val CACHED_OUTPUT_BASE_DIR = "cached_output_base"
    private const val HASH_SYMBOLS = 8
    private const val WORKSPACE_TEMPLATE = "__bazel_invocation_cache_workspace__"
    private const val OUTPUT_BASE_TEMPLATE = "__bazel_invocation_cache_output_base__"
    private const val MAX_FILE_SIZE_TO_CACHE_BYTES = 100_000

    private val LOG = logger<BazelInvocationCache>()
  }

  private lateinit var originalBazelInfo: BazelInfo

  private val invocationCacheDir: Path
    get() = bspInfo.bazelBspDir.resolve(INVOCATION_CACHE_DIR)

  private val buildNewCache = invocationCacheDir.notExists()

  private val outputBaseDir: Path
    get() = invocationCacheDir.resolve(OUTPUT_BASE_DIR)

  private val hashCommandMap = WeakHashMap<BazelCommandExecutionDescriptor, String>()

  override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
    return if (buildNewCache) {
      launchAndCacheProcess(executionDescriptor)
    }
    else {
      launchProcessFromCache(executionDescriptor)
    }
  }

  private fun launchAndCacheProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
    val cacheDir = getCacheDirectory(executionDescriptor).createDirectories()

    val defaultBazelProcessLauncher = DefaultBazelProcessLauncher(workspaceRoot)
    val bepOutputFile = Files.createTempFile(cacheDir, "bazel-bep-output", null)
    val process = defaultBazelProcessLauncher.launchProcess(redirectEventFile(executionDescriptor, bepOutputFile))

    val stdout = process.inputStream.readAllBytes()
    val statusCode = process.waitFor()
    check(statusCode == 0) {
      "$executionDescriptor failed with status code $statusCode, " +
      "last lines of stdout: ${String(stdout).lines().takeLast(20).joinToString("\n")}"
    }

    if (executionDescriptor.command.contains("info")) {
      originalBazelInfo = bazelInfoResolver.parseBazelInfo(String(stdout).lines(), stderrLines = emptyList())
    }
    val absolutePathReplacements = mapOf(
      originalBazelInfo.outputBase.invariantSeparatorsPathString to OUTPUT_BASE_TEMPLATE,
      originalBazelInfo.workspaceRoot.invariantSeparatorsPathString to WORKSPACE_TEMPLATE,
    )

    cacheDir.resolve(STDOUT_FILE).writeText(fixAbsolutePaths(String(stdout), absolutePathReplacements))

    cacheBepOutput(executionDescriptor, bepOutputFile, absolutePathReplacements)
    bepOutputFile.deleteExisting()

    return CachedProcess(
      stderr = byteArrayOf(),
      stdout = stdout,
      statusCode = statusCode,
    )
  }

  private fun redirectEventFile(
    executionDescriptor: BazelCommandExecutionDescriptor,
    bepOutputFile: Path,
  ): BazelCommandExecutionDescriptor {
    val descriptorWithRedirectedEventFile = executionDescriptor.copy(
      command = executionDescriptor.command.map { arg ->
        if (arg.startsWith(buildEventBinaryFileArg)) {
          BazelFlag.buildEventBinaryFile(bepOutputFile.toString())
        }
        else {
          arg
        }
      },
    )
    return descriptorWithRedirectedEventFile
  }

  private fun BazelInvocationCache.cacheBepOutput(
    executionDescriptor: BazelCommandExecutionDescriptor,
    eventFile: Path,
    absolutePathReplacements: Map<String, String>,
  ) {
    val originalEventFile = executionDescriptor.buildEventBinaryFile() ?: return
    originalEventFile.writeBytes(eventFile.readBytes())

    val cachedEventFile = getCacheDirectory(executionDescriptor).resolve(BEP_EVENTS_FILE)

    cachedEventFile.outputStream().buffered().use { outputStream ->
      eventFile.readBuildEvents().forEach { buildEvent ->
        cacheBepOutput(executionDescriptor, buildEvent)
        val fixedBuildEvent = fixAbsolutePaths(buildEvent, absolutePathReplacements)
        fixedBuildEvent.writeDelimitedTo(outputStream)
      }
    }
  }

  /**
   * Cache only the files that [org.jetbrains.bazel.server.bep.BepServer] uses
   */
  private fun cacheBepOutput(command: BazelCommandExecutionDescriptor, buildEvent: BuildEvent) {
    val filesToCache: List<BuildEventStreamProtos.File> = buildList {
      if (buildEvent.hasTestResult()) {
        addAll(buildEvent.testResult.testActionOutputList)
      }
      if (buildEvent.hasNamedSetOfFiles()) {
        addAll(buildEvent.namedSetOfFiles.filesList)
      }
      if (buildEvent.hasAction()) {
        add(buildEvent.action.stderr)
      }
    }
    filesToCache
      .filter { file -> file.fileCase == BuildEventStreamProtos.File.FileCase.URI }
      .forEach { file ->
        cacheOutputFile(command, URI.create(file.uri).toPath())
      }
  }

  private fun cacheOutputFile(command: BazelCommandExecutionDescriptor, file: Path) {
    if (file.startsWith(workspaceRoot)) return
    if (!shouldCacheOutputFile(file)) {
      LOG.warn("Skipping caching output file $file")
      return
    }
    val relativePath = file.relativeTo(originalBazelInfo.outputBase)
    val destination = getCacheDirectory(command).resolve(CACHED_OUTPUT_BASE_DIR).resolve(relativePath)
    file.copyTo(destination.createParentDirectories(), overwrite = true)
  }

  private fun shouldCacheOutputFile(file: Path): Boolean {
    if (file.name.endsWith(Constants.ASPECT_OUTPUT_EXTENSION)) return true
    return file.fileSize() < MAX_FILE_SIZE_TO_CACHE_BYTES
  }

  private fun launchProcessFromCache(executionDescriptor: BazelCommandExecutionDescriptor): Process {
    val cacheDir = getCacheDirectory(executionDescriptor)
    if (!cacheDir.exists()) deleteCacheDirectoryAndThrow("Can't find cache dir for ${executionDescriptor.command}, expected $cacheDir")

    val cachedOutputBase = cacheDir.resolve(CACHED_OUTPUT_BASE_DIR)
    if (cachedOutputBase.exists()) {
      copyCachedOutputBase(cachedOutputBase)
    }

    val absolutePathReplacements = mapOf(
      OUTPUT_BASE_TEMPLATE to outputBaseDir.invariantSeparatorsPathString,
      WORKSPACE_TEMPLATE to workspaceRoot.invariantSeparatorsPathString,
    )

    val cachedBepOutput = cacheDir.resolve(BEP_EVENTS_FILE)
    if (cachedBepOutput.exists()) {
      copyCachedBepOutput(executionDescriptor, cachedBepOutput, absolutePathReplacements)
    }

    val stdoutFile = cacheDir.resolve(STDOUT_FILE)
    if (stdoutFile.notExists()) deleteCacheDirectoryAndThrow("Can't find stdout for command $executionDescriptor")
    val stdout = stdoutFile.readText()
    val stdoutFixed = fixAbsolutePaths(stdout, absolutePathReplacements)

    return CachedProcess(
      stdout = stdoutFixed.toByteArray(),
      stderr = byteArrayOf(),
      statusCode = 0,
    )
  }

  private class CachedProcess(
    private val stdout: ByteArray,
    private val stderr: ByteArray,
    private val statusCode: Int,
  ) : Process() {
    override fun getOutputStream(): OutputStream? = null
    override fun getInputStream(): InputStream = stdout.inputStream()
    override fun getErrorStream(): InputStream = stderr.inputStream()
    override fun waitFor(): Int = exitValue()
    override fun exitValue(): Int = statusCode
    override fun destroy() {}
  }

  private fun copyCachedOutputBase(cachedOutputBase: Path) {
    val visitor = object : SimpleFileVisitor<Path>() {
      override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        val destination = outputBaseDir.resolve(file.relativeTo(cachedOutputBase))
        file.copyTo(destination.createParentDirectories(), overwrite = true)
        return FileVisitResult.CONTINUE
      }
    }

    Files.walkFileTree(cachedOutputBase, visitor)
  }

  private fun copyCachedBepOutput(
    executionDescriptor: BazelCommandExecutionDescriptor,
    cachedBepOutput: Path,
    absolutePathReplacements: Map<String, String>,
  ) {
    val actualBepOutput = requireNotNull(executionDescriptor.buildEventBinaryFile())
    actualBepOutput.outputStream().buffered().use { outputStream ->
      cachedBepOutput.readBuildEvents().forEach { buildEvent ->
        val fixedBuildEvent = fixAbsolutePaths(buildEvent, absolutePathReplacements)
        fixedBuildEvent.writeDelimitedTo(outputStream)
      }
    }
  }

  private fun Path.readBuildEvents(): List<BuildEvent> {
    val parser = BuildEvent.parser()
    val events = mutableListOf<BuildEvent>()
    this@readBuildEvents.inputStream().buffered().use { inputStream ->
      while (inputStream.available() > 0) {
        events += parser.parsePartialDelimitedFrom(inputStream)
      }
    }
    return events
  }

  private fun BazelCommandExecutionDescriptor.buildEventBinaryFile(): Path? = command
    .firstOrNull { it.startsWith(buildEventBinaryFileArg) }
    ?.substringAfter(buildEventBinaryFileArg)
    ?.let { Path(it) }

  private fun fixAbsolutePaths(buildEvent: BuildEvent, replacements: Map<String, String>): BuildEvent {
    val jsonString = JsonFormat.printer().print(buildEvent)
    val fixed = fixAbsolutePaths(jsonString, replacements)
    val builder = BuildEvent.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(fixed, builder)
    return builder.buildPartial()
  }

  /**
   * The cached output we get may have been run on another machine,
   * where both the Bazel output base and the workspace root have different absolute paths.
   * Because the output of BEP and of `bazel info` (unfortunately) include absolute paths, we have to substitute them here.
   */
  private fun fixAbsolutePaths(bazelOutput: String, replacements: Map<String, String>): String {
    var output = bazelOutput
    for ((old, new) in replacements) {
      output = output.replace(old, new)
    }
    return output
  }

  private fun getCacheDirectory(command: BazelCommandExecutionDescriptor): Path =
    invocationCacheDir.resolve(hashCommandLazy(command))

  /**
   * Hashing command involves a call to [hashSubtree], so it's best to avoid hashing it several times
   */
  private fun hashCommandLazy(command: BazelCommandExecutionDescriptor): String = synchronized(hashCommandMap) {
    hashCommandMap.getOrPut(command) { doHashCommand(command) }
  }

  private fun doHashCommand(command: BazelCommandExecutionDescriptor): String {
    val digest = DigestUtil.sha256()
    digest.hashCommand(command)
    return bytesToHex(digest.digest()).take(HASH_SYMBOLS)
  }

  private val buildEventBinaryFileArg: String
    get() = BazelFlag.buildEventBinaryFile("")

  private fun MessageDigest.hashCommand(command: BazelCommandExecutionDescriptor) {
    val targetPatternFileArg = BazelFlag.targetPatternFile("")
    val aspectArg = BazelFlag.aspect("")

    hashSubtree(bspInfo.bspProjectRoot)

    command.command
      .drop(1)  // Bazel path is machine/system-dependent, so ignore it
      .forEach { arg ->
        when {
          arg.startsWith(targetPatternFileArg) -> {
            hashString(targetPatternFileArg)
            hashFileContentsIgnoringLineEndings(Path(arg.substringAfter(targetPatternFileArg)))
          }

          arg.startsWith(buildEventBinaryFileArg) ->
            hashString(buildEventBinaryFileArg)

          arg.startsWith(aspectArg) -> {
            hashString(aspectArg)
            hashSubtree(aspectsResolver.aspectsPath)
          }

          else -> hashString(arg)
        }
      }

    command.environment.toSortedMap().forEach { (key, value) ->
      hashString(key)
      hashString(value)
    }
  }

  private fun MessageDigest.hashString(string: String) {
    update(string.toByteArray())
  }

  private fun MessageDigest.hashSubtree(root: Path, currentDir: Path = root) {
    require(currentDir.isDirectory())
    if (currentDir.isSymbolicLink()) return
    if (currentDir.name.startsWith(".")) return

    for (child in currentDir.listDirectoryEntries().sorted()) {
      when {
        child.isDirectory() -> hashSubtree(root, child)
        child.isRegularFile() -> {
          val name = child.name
          if ((name.startsWith(".") && !name.startsWith(".bazel")) ||
              name == "MODULE.bazel.lock" ||
              name == "kotlin-stdlib.jar") {
            continue
          }
          hashString(child.relativeTo(root).invariantSeparatorsPathString)
          hashFileContentsIgnoringLineEndings(child)
        }
      }
    }
  }

  // Ignore line endings here because Git can change them depending on the OS
  private fun MessageDigest.hashFileContentsIgnoringLineEndings(file: Path) {
    val lines = runCatching { file.readLines() }.getOrNull()
    if (lines != null) {
      lines.forEach { line ->
        update(line.toByteArray())
      }
    }
    else {
      // Couldn't read lines from file, probably it's just a binary file
      hashFileContents(file)
    }
  }

  private fun MessageDigest.hashFileContents(file: Path) {
    file.inputStream().use { inputStream ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      while (true) {
        val bytesRead = inputStream.read(buffer)
        if (bytesRead <= 0) break
        update(buffer, 0, bytesRead)
      }
    }
  }

  @OptIn(ExperimentalPathApi::class)
  private fun deleteCacheDirectoryAndThrow(message: String) {
    invocationCacheDir.deleteRecursively()
    throw OutdatedCacheException(message)
  }

  class OutdatedCacheException(message: String) : RuntimeException(
    "$message. Please rerun the test to build a new cache directory.",
  )
}

object BazelInvocationCacheProvider : BazelProcessLauncherProvider {
  override fun createBazelProcessLauncher(
    workspaceRoot: Path,
    bspInfo: BspInfo,
    aspectsResolver: InternalAspectsResolver,
    bazelInfoResolver: BazelInfoResolver,
  ): BazelProcessLauncher = BazelInvocationCache(workspaceRoot, bspInfo, aspectsResolver, bazelInfoResolver)
}
