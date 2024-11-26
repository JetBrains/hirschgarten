package org.jetbrains.bsp.bazel.server.sync.firstStep.mappings

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.BuildTargetTag
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bsp.bazel.server.model.Language

private const val TAGS_NAME = "tags"
private const val BAZEL_MANUAL_TAG = "manual"
private const val BAZEL_NO_IDE_TAG = "no-ide"

const val SRCS_NAME = "srcs"
const val DEPS_NAME = "deps"
const val EXPORTS_NAME = "exports"

fun Target.toBspBuildTarget(): BuildTarget =
  BuildTarget(
    BuildTargetIdentifier(rule.name),
    inferTags(),
    inferLanguages().map { it.id }.toList(),
    getAllInterestingDeps().map { BuildTargetIdentifier(it) }.toList(),
    inferCapabilities(),
  )

private fun Target.inferTags(): List<String> {
  val typeTag = inferTypeTagFromTargetKind()
  val manualTag = if (isManual()) BuildTargetTag.MANUAL else null

  return listOfNotNull(typeTag, manualTag)
}

private fun Target.inferTypeTagFromTargetKind(): String =
  when {
    isBinary() -> BuildTargetTag.APPLICATION
    isTest() -> BuildTargetTag.TEST
    else -> BuildTargetTag.LIBRARY
  }

private fun Target.inferLanguages(): Set<Language> {
  val languagesForTarget = Language.allOfKind(rule.ruleClass)
  val languagesForSources = getListAttribute(SRCS_NAME).flatMap { Language.allOfSource(it) }.toHashSet()
  return languagesForTarget + languagesForSources
}

private fun Target.getAllInterestingDeps(): List<String> = getListAttribute(DEPS_NAME) + getListAttribute(EXPORTS_NAME)

private fun Target.inferCapabilities(): BuildTargetCapabilities =
  BuildTargetCapabilities().apply {
    canCompile = !isManual()
    canRun = isBinary()
    canTest = isTest()
  }

private fun Target.isBinary(): Boolean = rule.ruleClass.endsWith("_binary") || rule.ruleClass == "intellij_plugin_debug_target"

private fun Target.isTest(): Boolean = rule.ruleClass.endsWith("_test")

fun Target.isManual(): Boolean = BAZEL_MANUAL_TAG in getListAttribute(TAGS_NAME)

fun Target.isNoIde(): Boolean = BAZEL_NO_IDE_TAG in getListAttribute(TAGS_NAME)

fun Target.isSupported(): Boolean = Language.allOfKind(rule.ruleClass).isNotEmpty()

fun Target.getListAttribute(name: String): List<String> =
  rule.attributeList
    .firstOrNull { it.name == name }
    ?.stringListValueList
    .orEmpty()
