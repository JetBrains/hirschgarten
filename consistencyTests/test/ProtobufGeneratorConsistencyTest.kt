package com.intellij.bazel.test

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.PathManager
import com.intellij.tool.HttpClient
import com.intellij.util.SystemProperties
import com.intellij.util.system.CpuArch
import com.intellij.util.system.LowLevelLocalMachineAccess
import com.intellij.util.system.OS
import org.apache.http.client.methods.HttpGet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.EnumSet
import java.util.zip.ZipInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText

// protobuf definition generation/validation test
// in principle similar to `AbstractAllIntellijEntitiesGenerationTest` test
internal class ProtobufGeneratorConsistencyTest {

  companion object {
    private const val UPDATE_PROPERTY_KEY = "bazel.protobuf.update.generated"
    private const val GRPC_VERSION = "1.60.2"
    private const val FALLBACK_PROTOC_VERSION = "24.2"
  }

  val workDir: Path = PathManager.getHomeDir()
    .resolve("plugins/bazel")

  data class ProtobufOutput(
    val relativePath: String,
    val relativeOutput: String = "protobuf/src/main/gen",
    val includeGrpc: Boolean = false,
    val relativeIncludePath: String? = null,
  )

  fun protobufOutputs(workDir: Path): List<ProtobufOutput> {
    // shared 3rd party protos
    val thirdPartyBlacklist = setOf(
      // we don't want to generate google API stuff
      "annotations.proto", "client.proto", "field_behavior.proto", "http.proto", "launch_stage.proto",

      // in case of some any proto definition from here is needed,
      // just remove it from the blacklist
      "package_metrics.proto", "package_load_metric.proto", "test_status.proto", "command_server.proto", "execution_graph.proto",
      "bazel_graph.proto", "spawn.proto", "bazel_flags.proto", "builtin.proto", "android_deploy_info.proto",
      "java_compilation.proto", "analysis_v2.proto"
    )
    val thirdPartyProtos = Files.walk(workDir.resolve("protobuf/src/proto"))
      .filter { it.extension == "proto" }
      .filter { it.name !in thirdPartyBlacklist }
      .map { proto ->
        ProtobufOutput(
          relativePath = workDir.relativize(proto).toString(),
          includeGrpc = true,
          relativeIncludePath = "protobuf",
        )
      }
      .toList()

    // custom protos
    return thirdPartyProtos
  }

  @OptIn(ExperimentalPathApi::class)
  @Test
  fun `check protobuf generated`() {
    val protoc = downloadProtoc()
    for (output in protobufOutputs(workDir)) {
      val (tmpDir, existingOutputDir) = runProtoc(protoc, workDir, output)
      try {
        Files.walk(tmpDir).filter { Files.isRegularFile(it) }.forEach { generated ->
          val existing = existingOutputDir.resolve(tmpDir.relativize(generated))
          val message = "Set -D$UPDATE_PROPERTY_KEY=true to update protos directly"
          check(existing.exists()) {
            "Generated file not committed: ${tmpDir.relativize(generated)}. " +
            message
          }
          assertEquals(
            existing.readText(), generated.readText(),
            message,
          )
        }
      }
      finally {
        tmpDir.deleteRecursively()
      }
    }
  }

  @OptIn(ExperimentalPathApi::class)
  @Test
  fun `update protobuf generated`() {
    if (!SystemProperties.getBooleanProperty(UPDATE_PROPERTY_KEY, false)) {
      assumeFalse(true, "Set -D$UPDATE_PROPERTY_KEY=true to write generated files")
    }

    val protoc = downloadProtoc()
    for (output in protobufOutputs(workDir)) {
      val (tmpDir, existingOutputDir) = runProtoc(protoc, workDir, output)
      try {
        Files.walk(tmpDir).filter { Files.isRegularFile(it) }.forEach { generated ->
          val target = existingOutputDir.resolve(tmpDir.relativize(generated))
          Files.createDirectories(target.parent)
          Files.copy(generated, target, StandardCopyOption.REPLACE_EXISTING)
        }
      }
      finally {
        tmpDir.deleteRecursively()
      }
    }
  }

  private fun runProtoc(protoc: Path, workDir: Path, output: ProtobufOutput): Pair<Path, Path> {
    val protoFile = workDir.resolve(output.relativePath.replace('\\', '/'))
    val includeDir = if (output.relativeIncludePath != null) {
      workDir.resolve(output.relativeIncludePath)
    }
    else {
      protoFile.parent
    }
    val protoInput = includeDir.relativize(protoFile).toString()
    val existingOutputDir = workDir.resolve(output.relativeOutput)
    val tmpDir = Files.createTempDirectory("protobuf_gen")
    val builtinIncludeDir = protoc.resolveSibling("${protoc.fileName}-include")

    val params = mutableListOf("-I$includeDir", "-I$builtinIncludeDir", "--java_out=$tmpDir")
    if (output.includeGrpc) {
      val grpcPlugin = downloadGrpcJavaPlugin()
      params += "--plugin=protoc-gen-grpc-java=$grpcPlugin"
      params += "--grpc-java_out=$tmpDir"
    }
    params += protoInput

    val process = GeneralCommandLine(protoc.toString())
      .withParameters(params)
      .createProcess()
    val stderr = process.errorStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    check(exitCode == 0) { "protoc failed with $exitCode:\n$stderr" }

    return tmpDir to existingOutputDir
  }

  @OptIn(LowLevelLocalMachineAccess::class)
  private fun downloadProtoc(): Path {
    val os = when (OS.CURRENT) {
      OS.Windows -> "win"
      OS.macOS -> "osx"
      OS.Linux -> "linux"
      else -> error("unsupported OS")
    }

    val arch = when (CpuArch.CURRENT) {
      CpuArch.X86_64 -> "x86_64"
      CpuArch.ARM64 -> "aarch_64"
      else -> error("unsupported CPU arch")
    }

    val version = tryExtractPlatformProtocVersion() ?: FALLBACK_PROTOC_VERSION
    val specifier = if (OS.CURRENT == OS.Windows) {
      // this version of protobuf only has x86_64 package (excluding 32bit one)
      "win64"
    }
    else {
      "$os-$arch"
    }

    val pkgName = "protoc-$version-$specifier.zip"

    val cacheDir = PathManager.getSystemDir().resolve("protoc-cache")
    val executable = cacheDir.resolve("protoc-$version-$specifier")
    val includeDir = cacheDir.resolve("protoc-$version-$specifier-include")

    if (executable.exists() && Files.isDirectory(includeDir)) {
      return executable
    }

    Files.createDirectories(cacheDir)

    val zipFile = cacheDir.resolve(pkgName)

    val downloadUrl = "https://cache-redirector.jetbrains.com/" +
                      "github.com/protocolbuffers/protobuf/releases/download/v$version/$pkgName"

    if (!HttpClient.download(HttpGet(downloadUrl), zipFile, retries = 1)) {
      error("failed to download protoc")
    }

    val binaryEntry = if (OS.CURRENT == OS.Windows) {
      "bin/protoc.exe"
    }
    else {
      "bin/protoc"
    }

    ZipInputStream(Files.newInputStream(zipFile)).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        when {
          entry.name == binaryEntry -> {
            Files.copy(zip, executable, StandardCopyOption.REPLACE_EXISTING)
          }

          // copy builtin proto types
          entry.name.startsWith("include/") && !entry.isDirectory -> {
            val target = includeDir.resolve(entry.name.removePrefix("include/"))
            Files.createDirectories(target.parent)
            Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING)
          }
        }
        entry = zip.nextEntry
      }
    }
    Files.deleteIfExists(zipFile)

    if (OS.CURRENT == OS.Linux || OS.CURRENT == OS.macOS) {
      Files.setPosixFilePermissions(
        executable,
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE),
      )
    }

    return executable
  }

  @OptIn(LowLevelLocalMachineAccess::class)
  private fun downloadGrpcJavaPlugin(): Path {
    val os = when (OS.CURRENT) {
      OS.Windows -> "windows"
      OS.macOS -> "osx"
      OS.Linux -> "linux"
      else -> error("unsupported OS")
    }
    val arch = when (CpuArch.CURRENT) {
      CpuArch.X86_64 -> "x86_64"
      CpuArch.ARM64 -> "aarch_64"
      else -> error("unsupported CPU arch")
    }

    val classifier = "$os-$arch"
    val artifactName = "protoc-gen-grpc-java-$GRPC_VERSION-$classifier.exe"
    val cacheDir = PathManager.getSystemDir().resolve("protoc-cache")
    val executable = cacheDir.resolve("grpc-java-$GRPC_VERSION-$classifier")

    if (executable.exists()) {
      return executable
    }

    Files.createDirectories(cacheDir)

    val downloadUrl = "https://cache-redirector.jetbrains.com/" +
                      "repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/$GRPC_VERSION/$artifactName"

    if (!HttpClient.download(HttpGet(downloadUrl), executable, retries = 1)) {
      error("failed to download grpc-java plugin")
    }

    if (OS.CURRENT == OS.Linux || OS.CURRENT == OS.macOS) {
      Files.setPosixFilePermissions(
        executable,
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE),
      )
    }

    return executable
  }

  private fun tryExtractPlatformProtocVersion(): String? {
    val protocScript = PathManager.getHomeDir()
      .resolve("community/build/protobuf/getprotoc.sh")
    if (!protocScript.exists()) {
      return null
    }
    return """PROTOC_VERSION=\$\{PROTOC_VERSION:-(\d+\.\d+)\}""".toRegex()
      .find(protocScript.readText())
      ?.groupValues?.get(1)
  }


}
