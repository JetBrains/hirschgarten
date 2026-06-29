package org.jetbrains.bazel.tools.generator

import com.google.devtools.build.docgen.builtin.PatchedBuiltinProtos
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.system.exitProcess

private const val DEFAULT_BAZEL_REPO_URL = "https://github.com/bazelbuild/bazel.git"
private const val BAZEL_BUILD_TARGET = "//src/main/java/com/google/devtools/build/lib:gen_api_proto"
private const val BINARY_PROTO_RELATIVE_PATH = "bazel-bin/src/main/java/com/google/devtools/build/lib/builtin.pb"
private const val PATCH_PREFIX = "api-exporter-enhancements@"
private const val PATCH_SUFFIX = ".patch"
private val AVAILABLE_PATCH_VERSIONS = listOf("7.5.0", "8.0.0", "9.0.0")

/**
 * Generates Bazel builtin function signature data for the Starlark plugin.
 *
 * ## Pipeline
 *
 * 1. Clone (or reuse a local checkout of) the [Bazel repository](https://github.com/bazelbuild/bazel)
 *    at a chosen tag. When no `--repo-url` is given, the default
 *    repository URL is used and cloned into a temporary directory.
 * 2. Reset the working tree (`git reset --hard`) and apply the versioned
 *    `api-exporter-enhancements@{version}.patch`. The patch extends the upstream `ApiExporter` and
 *    `builtin.proto` with extra `ApiContext` enum values (`MODULE`, `VENDOR`, `REPO`) and
 *    extra `Param` fields (`is_positional`, `is_named`).
 * 3. Build the patched Bazel target `//src/main/java/com/google/devtools/build/lib:gen_api_proto`
 *    via `bazelisk`, which produces a binary proto (`builtin.pb`) containing all Starlark
 *    builtin signatures.
 * 4. Parse the binary proto and convert to `builtins@{tag}.json` in
 *    `intellij.bazel.core/resources/bazelSignatures/builtins/`.
 *    If the resulting JSON is identical to the previous highest-version file,
 *    the write is skipped.
 *
 * The `patched_builtin.proto` and generated `PatchedBuiltinProtos.java` are checked in
 * under `builtins/proto/` and `builtins/gen/` respectively, and only need manual
 * regeneration when the upstream proto schema changes.
 *
 * ## Usage
 *
 * From the monorepo root:
 * ```
 * bazel run //plugins/bazel/tools/bazel_signatures/builtins:generator \
 *     -- --version <version-tag> \
 *        [--repo-url <local-path-or-git-url>]
 * ```
 **/
fun main(args: Array<String>) {
  val arguments = parseArguments(args.toList())

  println()

  when {
    arguments.repoUrl.startsWith("https://") || arguments.repoUrl.endsWith(".git") -> {
      println("Using git URL: ${arguments.repoUrl}")
      println("The repository will be cloned into a temporary directory.")
    }
    else -> {
      println("WARNING: This will run 'git reset --hard' on '${arguments.repoUrl}' at tag ${arguments.version}.")
      println("All uncommitted changes will be lost, and a patch will be applied.")
    }
  }

  print("Continue? [y/N] ")
  val answer = readlnOrNull()?.trim()?.lowercase()
  if (answer != "y") {
    println("Aborted.")
    exitProcess(0)
  }
  val tempDirPrefix = "bazel-repo-"
  val repoDir = resolveRepo(arguments.repoUrl, tempDirPrefix)
  try {
    gitResetHard(repoDir)
    gitCheckout(repoDir, "tags/${arguments.version}")

    val patchPath = resolvePatchForVersion(arguments.version)
    gitApplyPatch(repoDir, patchPath)
    appendBuildFlags(repoDir)
    bazeliskBuild(repoDir, BAZEL_BUILD_TARGET)

    val binaryProtoPath = repoDir.resolve(BINARY_PROTO_RELATIVE_PATH)
    require(binaryProtoPath.exists()) { "Build output not found: $binaryProtoPath" }

    println("# Converting binary proto to JSON")
    val builtins = binaryProtoPath
      .inputStream()
      .use { PatchedBuiltinProtos.Builtins.parseFrom(it) }
    val functions = builtins.globalList
      .filter { it.hasCallable() }
      .mapNotNull { it.toFunctionSignature() }
      .plus(LOAD_FUNCTION)
    val builtinsDir = resolveBazelPluginSignaturesDir().resolve("builtins")
    builtinsDir.createDirectories()
    writeSignaturesJson(functions, builtinsDir.resolve("builtins@${arguments.version}.json"))

    println()
    println("Done.")
  }
  finally {
    cleanupIfTempClone(repoDir, tempDirPrefix)
  }
}

private data class Arguments(
  val repoUrl: String,
  val version: String,
)

private fun parseArguments(rawArgs: List<String>): Arguments {
  var repoUrl: String? = null
  var version: String? = null

  var index = 0
  while (index < rawArgs.size) {
    val key = rawArgs[index++]
    require(index < rawArgs.size) { "Missing value for $key" }
    val value = rawArgs[index++]
    when (key) {
      "--repo-url" -> repoUrl = value
      "--version" -> version = value
      else -> error("Unknown argument: $key. Usage: --version <version> [--repo-url <path-or-url>]")
    }
  }

  requireNotNull(version) { "--version is required" }

  return Arguments(
    repoUrl = repoUrl ?: DEFAULT_BAZEL_REPO_URL,
    version = version,
  )
}

private fun resolvePatchForVersion(version: String): String {
  val selectedVersion = AVAILABLE_PATCH_VERSIONS
    .filter { it.compareAsVersions(version) <= 0 }
    .maxWithOrNull { a, b -> a.compareAsVersions(b) }
    ?: error("No compatible patch found for version $version. Available: $AVAILABLE_PATCH_VERSIONS")
  val path = "/patches/$PATCH_PREFIX${selectedVersion}$PATCH_SUFFIX"
  println("# Using patch: $path (for version $version)")
  return path
}

private fun appendBuildFlags(repoDir: java.nio.file.Path) {
  val bazelrc = repoDir.resolve(".bazelrc")
  val flags = """
    build --repo_env=BAZEL_USE_CPP_ONLY_TOOLCHAIN=1
    build --crosstool_top=@bazel_tools//tools/cpp:toolchain
    build --host_crosstool_top=@bazel_tools//tools/cpp:toolchain
    """.trimIndent()
  bazelrc.appendText(flags)
  println("# Appended build flags to .bazelrc")
}

private fun PatchedBuiltinProtos.Value.toFunctionSignature(): FunctionSignature? {
  val environment = apiContext.toEnvironmentStrings() ?: return null
  return FunctionSignature(
    name = name,
    doc = doc.cleanupStardoc(),
    environment = environment,
    params = callable.paramList.map { it.toParamSignature() },
    returnType = callable.returnType.takeIf { it.isNotEmpty() && it != "NoneType" },
  )
}

private fun PatchedBuiltinProtos.Param.toParamSignature(): ParamSignature {
  val newName = when {
    isStarStarArg && !name.startsWith("**") -> "**${name}"
    isStarArg && !name.startsWith("*") -> "*${name}"
    else -> name
  }
  return ParamSignature(
    name = newName,
    doc = doc.cleanupStardoc() ?: KNOWN_PARAM_DOCS[newName],
    defaultValue = defaultValue.ifEmpty { null },
    named = isNamed,
    positional = isPositional,
    required = isMandatory,
  )
}

private fun PatchedBuiltinProtos.ApiContext.toEnvironmentStrings(): List<String>? = when (this) {
  PatchedBuiltinProtos.ApiContext.ALL -> listOf("BZL", "BUILD", "MODULE", "REPO", "VENDOR")
  PatchedBuiltinProtos.ApiContext.BZL -> listOf("BZL")
  PatchedBuiltinProtos.ApiContext.BUILD -> listOf("BUILD")
  PatchedBuiltinProtos.ApiContext.MODULE -> listOf("MODULE")
  PatchedBuiltinProtos.ApiContext.VENDOR -> listOf("VENDOR")
  PatchedBuiltinProtos.ApiContext.REPO -> listOf("REPO")
  PatchedBuiltinProtos.ApiContext.UNRECOGNIZED -> null
}

private val LOAD_FUNCTION = FunctionSignature(
  name = "load",
  doc = "Loads specified symbols from an external .bzl file. " +
    "This function allows importing functions, rules, and other symbols defined in Starlark extension files.",
  environment = listOf("BUILD", "BZL"),
  params = listOf(
    ParamSignature(
      name = "label",
      doc = "Label reference to the .bzl file containing the symbols to import (e.g. \"@repo//path/to/file.bzl\")",
      named = false,
      positional = true,
      required = true,
      defaultValue = null,
    ),
    ParamSignature(
      name = "*symbols",
      doc = "Symbol names to import from the extension file.",
      named = false,
      positional = true,
      required = false,
      defaultValue = null,
    ),
    ParamSignature(
      name = "**aliases",
      doc = "Symbol imports with aliases. The key is the local alias and the value is the original symbol name.",
      named = false,
      positional = false,
      required = false,
      defaultValue = null,
    ),
  ),
  returnType = null,
)

/**
 * Documentation for well-known parameters that Bazel's proto export omits.
 * The `name` attribute is implicit in every rule but has no doc string in the proto.
 */
private val KNOWN_PARAM_DOCS = mapOf(
  "name" to "A unique name for this target.",
)
