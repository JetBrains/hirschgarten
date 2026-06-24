package org.jetbrains.bazel.tools.generator

import com.google.devtools.build.lib.starlarkdocextract.StardocOutputProtos
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

private const val EXTRACT_PACKAGE = "ij_stardoc_gen"

/**
 * Generates a merged stardoc JSON file for an external Bazel rule repository.
 *
 * ## Pipeline
 *
 * 1. Clone (or reuse a local checkout of) the target rule repository at a chosen tag.
 * 2. Create `starlark_doc_extract` BUILD targets inside the cloned repo,
 *    one per `--bzl-file` entry, each with its own `bzl_library` deps.
 * 3. Build all targets via a single `bazelisk` invocation for parallelism.
 * 4. Parse each binary proto, convert rules to `FunctionSignature` (merging
 *    `StardocCommonParams`), and merge all results into a single map
 *    (rules take priority over macros; later entries override earlier ones).
 * 5. Write the merged JSON into `intellij.bazel.core/resources/bazelSignatures/`.
 *    If the resulting JSON is identical to the previous highest-version file,
 *    the write is skipped.
 *
 * Each `--bzl-file` starts a new entry; subsequent `--dep` flags attach to the
 * most recent `--bzl-file`. Global flags (`--repo-url`, `--ref`, `--exclude`,
 * `--output`) are position-independent.
 *
 * `--repo-url` also accepts a local path.
 * `--exclude` drops specific rules/functions from the output by name (repeatable).
 * Rules/functions without a doc string are always excluded automatically.
 *
 * ## Usage
 *
 * From the monorepo root:
 * ```
 * bazel run //plugins/bazel/tools/bazel_signatures/stardoc:generator \
 *     -- --repo-url <git-url> \
 *        --ref <tag|commit|branch> \
 *        --bzl-file <label> [--dep <bzl_library label>]... \
 *        [--bzl-file <label> [--dep <bzl_library label>]...]... \
 *        [--exclude <name>]... \
 *        --output <base-name>
 * ```
 *
 */
fun main(args: Array<String>) {
  val arguments = parseArguments(args.toList())

  println()
  println("Stardoc Signatures Generator")
  println("Repository: ${arguments.repoPath}")
  println("Ref: ${arguments.ref}")
  for ((i, entry) in arguments.bzlEntries.withIndex()) {
    println("Bzl file #${i + 1}: ${entry.bzlFile}")
    println("  Deps: ${entry.deps.ifEmpty { listOf("(none)") }}")
  }
  println("Excludes: ${arguments.excludes.ifEmpty { listOf("(none)") }}")
  println("Output: ${arguments.output}")

  val tempDirPrefix = "stardoc-repo-"
  val repoDir = resolveRepo(arguments.repoPath, tempDirPrefix)
  try {
    gitCheckout(repoDir, arguments.ref)

    println()
    println("# Creating starlark_doc_extract targets")
    val buildDir = repoDir.resolve(EXTRACT_PACKAGE)
    buildDir.createDirectories()
    val buildContent = buildString {
      for ((i, entry) in arguments.bzlEntries.withIndex()) {
        val depsStr = entry.deps.joinToString(", ") { "\"$it\"" }
        appendLine("starlark_doc_extract(")
        appendLine("    name = \"extract_$i\",")
        appendLine("    src = \"${entry.bzlFile}\",")
        if (entry.deps.isNotEmpty()) {
          appendLine("    deps = [$depsStr],")
        }
        appendLine(")")
        appendLine()
      }
    }
    buildDir.resolve("BUILD.bazel").writeText(buildContent)

    val targetLabels = arguments.bzlEntries.indices.map { "//$EXTRACT_PACKAGE:extract_$it" }
    bazeliskBuild(repoDir, "--check_visibility=false", *targetLabels.toTypedArray())

    println()
    println("# Converting binary protos to JSON")
    val excludeSet = arguments.excludes.toSet()
    val mergedFunctions = mutableMapOf<String, FunctionSignature>()
    val mergedRules = mutableMapOf<String, FunctionSignature>()
    for (i in arguments.bzlEntries.indices) {
      val protoPath = repoDir.resolve("bazel-bin/$EXTRACT_PACKAGE/extract_$i.binaryproto")
      require(protoPath.exists()) { "Build output not found: $protoPath" }
      val moduleInfo = protoPath.inputStream().use { StardocOutputProtos.ModuleInfo.parseFrom(it) }
      val macrosAsFunctions = moduleInfo.funcInfoList
        .mapNotNull { it.toFunctionSignature() }
        .filter { it.name !in excludeSet }
        .associateBy { it.name }
      val rulesAsFunctions = moduleInfo.ruleInfoList
        .mapNotNull { it.toFunctionSignature() }
        .filter { it.name !in excludeSet }
        .associateBy { it.name }
      mergedFunctions.putAll(macrosAsFunctions)
      mergedRules.putAll(rulesAsFunctions)
    }

    val signaturesDir = resolveBazelPluginSignaturesDir()
    val jsonFileName = "${arguments.output}@${arguments.ref}.json"
    val merged = mergedRules + mergedFunctions
    writeSignaturesJson(merged.values.toList(), signaturesDir.resolve(jsonFileName))

    println()
    println("Done.")
  } finally {
    cleanupIfTempClone(repoDir, tempDirPrefix)
  }
}

private data class BzlEntry(val bzlFile: String, val deps: List<String>)

private data class Arguments(
  val repoPath: String,
  val ref: String,
  val bzlEntries: List<BzlEntry>,
  val excludes: List<String>,
  val output: String,
)

private fun parseArguments(rawArgs: List<String>): Arguments {
  var repoPath: String? = null
  var ref: String? = null
  val bzlEntries = mutableListOf<BzlEntry>()
  var currentBzlFile: String? = null
  var currentDeps = mutableListOf<String>()
  val excludes = mutableListOf<String>()
  var output: String? = null

  fun flushEntry() {
    val bzl = currentBzlFile ?: return
    bzlEntries.add(BzlEntry(bzl, currentDeps.toList()))
    currentBzlFile = null
    currentDeps = mutableListOf()
  }

  var index = 0
  while (index < rawArgs.size) {
    val key = rawArgs[index++]
    require(index < rawArgs.size) { "Missing value for $key" }
    val value = rawArgs[index++]
    when (key) {
      "--repo-url" -> repoPath = value
      "--ref" -> ref = value
      "--bzl-file" -> {
        flushEntry()
        currentBzlFile = value
      }
      "--dep" -> {
        requireNotNull(currentBzlFile) { "--dep must follow a --bzl-file" }
        currentDeps.add(value)
      }
      "--exclude" -> excludes.add(value)
      "--output" -> output = value
      else -> error(
        "Unknown argument: $key. Usage: --repo-url <url> --bzl-file <label> [--dep <label>]... [--bzl-file <label> [--dep <label>]...]... --exclude <name> --output <name> [--ref <ref>]"
      )
    }
  }
  flushEntry()

  requireNotNull(repoPath) { "--repo-url is required" }
  requireNotNull(ref) { "--ref is required" }
  require(bzlEntries.isNotEmpty()) { "At least one --bzl-file is required" }
  requireNotNull(output) { "--output is required" }

  return Arguments(
    repoPath = repoPath,
    ref = ref,
    bzlEntries = bzlEntries,
    excludes = excludes,
    output = output,
  )
}

private fun StardocOutputProtos.RuleInfo.toFunctionSignature(): FunctionSignature? {
  if (docString.isNullOrBlank()) return null
  // Stardoc protos may prefix rule names with a module namespace (e.g. "versions.use_repository"); strip it.
  val name = ruleName.substringAfterLast('.')
  val attributes = attributeList
    .map { it.toParamSignature() }
    .mergeWith(RuleCommonParams.common)
    .let {
      when {
        name.endsWith("_test") -> it.mergeWith(RuleCommonParams.test)
        name.endsWith("_binary") -> it.mergeWith(RuleCommonParams.binary)
        else -> it
      }
    }
  return FunctionSignature(
    name = name,
    doc = docString.cleanupStardoc(),
    environment = listOf("BUILD", "BZL"),
    params = attributes,
    returnType = null,
  )
}

private fun StardocOutputProtos.AttributeInfo.toParamSignature() = ParamSignature(
  name = name,
  doc = docString.cleanupStardoc(),
  required = mandatory,
  defaultValue = defaultValue.ifEmpty { null },
  positional = false,
  named = true,
)

private fun StardocOutputProtos.StarlarkFunctionInfo.toFunctionSignature(): FunctionSignature? {
  if (docString.isNullOrBlank()) return null
  val name = functionName
  val explicitParams = parameterList
    .filter { it.role != StardocOutputProtos.FunctionParamRole.PARAM_ROLE_VARARGS &&
              it.role != StardocOutputProtos.FunctionParamRole.PARAM_ROLE_KWARGS }
  val params = explicitParams
    .map { it.toParamSignature() }
    .mergeWith(RuleCommonParams.common)
    .let {
      when {
        name.endsWith("_test") -> it.mergeWith(RuleCommonParams.test)
        name.endsWith("_binary") -> it.mergeWith(RuleCommonParams.binary)
        else -> it
      }
    }
  return FunctionSignature(
    name = name,
    doc = docString.cleanupStardoc(),
    environment = listOf("BUILD", "BZL"),
    params = params,
    returnType = null,
  )
}

private fun StardocOutputProtos.FunctionParamInfo.toParamSignature() = ParamSignature(
  name = name,
  doc = docString.cleanupStardoc(),
  required = mandatory,
  defaultValue = defaultValue.ifEmpty { null },
  positional = role == StardocOutputProtos.FunctionParamRole.PARAM_ROLE_ORDINARY ||
               role == StardocOutputProtos.FunctionParamRole.PARAM_ROLE_POSITIONAL_ONLY,
  named = role != StardocOutputProtos.FunctionParamRole.PARAM_ROLE_POSITIONAL_ONLY,
)

private fun List<ParamSignature>.mergeWith(other: List<ParamSignature>): List<ParamSignature> {
  val otherByName = other.associateBy { it.name }
  val res = map { param ->
    val corresponding = otherByName[param.name]
    if (corresponding != null && param.doc == null) param.copy(doc = corresponding.doc) else param
  }
  val existingNames = mapTo(mutableSetOf()) { it.name }
  return res + other.filter { it.name !in existingNames }
}
