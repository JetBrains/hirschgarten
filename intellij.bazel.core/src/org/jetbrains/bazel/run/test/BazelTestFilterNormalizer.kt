package org.jetbrains.bazel.run.test

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import java.util.regex.Pattern

private const val SPECS2_DISCOVERED_SUITE = "io.bazel.rulesscala.specs2.Specs2DiscoveredTestSuite"
private const val SPECS2_DISCOVERY_LABEL_SUFFIX = "//src/java/io/bazel/rulesscala/specs2:specs2_test_discovery"

internal fun normalizeBazelTestFilter(
  project: Project,
  target: BuildTarget,
  rawFilter: String?,
  testExecutableArguments: List<String>,
): String? =
  normalizeBazelTestFilter(
    target = target,
    rawFilter = rawFilter,
    testExecutableArguments = testExecutableArguments,
    useJetBrainsTestRunner = project.useJetBrainsTestRunner(),
  )

@VisibleForTesting
internal fun normalizeBazelTestFilter(
  target: BuildTarget,
  rawFilter: String?,
  testExecutableArguments: List<String>,
  useJetBrainsTestRunner: Boolean,
): String? {
  if (rawFilter == null) return null
  if (useJetBrainsTestRunner) return rawFilter
  if (testExecutableArguments.isNotEmpty()) return rawFilter
  if (!target.isRulesScalaSpecs2DiscoveredSuite()) return rawFilter
  if (!rawFilter.looksLikeJvmClassName()) return rawFilter

  return "^${Pattern.quote(rawFilter)}(#.*)?$"
}

private fun BuildTarget.isRulesScalaSpecs2DiscoveredSuite(): Boolean =
  data.filterIsInstance<ScalaBuildTarget>().any {
    it.testSuiteClass == SPECS2_DISCOVERED_SUITE ||
      it.testSuiteLabel?.endsWith(SPECS2_DISCOVERY_LABEL_SUFFIX) == true
  }

private fun String.looksLikeJvmClassName(): Boolean =
  JVM_CLASS_NAME_PATTERN.matches(this)

// Deliberately excludes `$` nested-class names until their Specs2/rules_scala
// runtime shape is verified; see docs/dev/scala_specs2_test_filter_fix_design_v2.md.
private val JVM_CLASS_NAME_PATTERN = Regex("[A-Za-z_][A-Za-z\\d_]*(\\.[A-Za-z_][A-Za-z\\d_]*)*")
