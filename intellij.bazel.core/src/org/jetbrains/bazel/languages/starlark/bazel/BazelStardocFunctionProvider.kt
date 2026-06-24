package org.jetbrains.bazel.languages.starlark.bazel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.project.Project

/**
 * Provides external Bazel rule signatures for Starlark code completion and documentation.
 *
 * Reads `*.json` stardoc resources (generated offline by
 * `//plugins/bazel/tools/bazel_signatures/stardoc:generator`) and
 * deserializes the JSON into [BazelGlobalFunction] objects via Gson.
 *
 * See the KDoc on the generator's `main` function for the full generation pipeline and
 * usage instructions.
 *
 * ### Last regeneration commands
 * ```bash
 * # rules_kotlin (v2.4.0)
 * bazel run //plugins/bazel/tools/bazel_signatures/stardoc:generator -- \
 *     --repo-url https://github.com/bazelbuild/rules_kotlin.git --ref v2.4.0 \
 *     --bzl-file //kotlin:core.bzl --dep //kotlin/internal \
 *     --bzl-file //kotlin:jvm.bzl --dep //kotlin/internal \
 *     --bzl-file //kotlin:lint.bzl --dep //kotlin/internal --dep //kotlin/internal/lint \
 *     --bzl-file //kotlin:repositories.doc.bzl --dep //kotlin/internal \
 *     --output rules_kotlin
 *
 * # rules_go (v0.61.1)
 * bazel run //plugins/bazel/tools/bazel_signatures/stardoc:generator -- \
 *     --repo-url https://github.com/bazelbuild/rules_go.git --ref v0.61.1 \
 *     --bzl-file //go/private/rules:library.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:binary.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:test.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:source.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:cross.bzl --dep //go:def \
 *     --bzl-file //go/private/rules:transition.bzl --dep //go:def \
 *     --bzl-file //go/private/tools:path.bzl --dep //go:def \
 *     --output rules_go
 * ```
 */
internal class BazelStardocFunctionProvider : StarlarkGlobalFunctionProvider {

  private val stardocToLoad = listOf(
    "/bazelSignatures/rules_kotlin@v2.4.0.json",
    "/bazelSignatures/rules_go@v0.61.1.json",
  )

  private val cachedFunctions by lazy { loadStardocFunctions() }

  override fun functions(project: Project): List<BazelGlobalFunction> = cachedFunctions

  private fun loadStardocFunctions(): List<BazelGlobalFunction> {
    val cls = this::class.java
    val gson = Gson()
    return stardocToLoad.flatMap { path ->
      cls.getResourceAsStream(path)
        ?.use { it.bufferedReader().readText() }
        ?.let { json -> gson.fromJson(json, bazelGlobalFunctionListTypeToken) }
        .orEmpty()
    }
  }

  private val bazelGlobalFunctionListTypeToken = object : TypeToken<List<BazelGlobalFunction>>() {}
}
