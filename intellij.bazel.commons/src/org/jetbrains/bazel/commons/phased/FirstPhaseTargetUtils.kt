package org.jetbrains.bazel.commons.phased

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTargetTag

val Target.name: String
  @ApiStatus.Internal
  get() = rule.name

val Target.kind: String
  @ApiStatus.Internal
  get() = rule.ruleClass

private const val TAGS_NAME = "tags"
val Target.tags: List<String>
  @ApiStatus.Internal
  get() = getListAttribute(TAGS_NAME)

private const val SRCS_NAME = "srcs"
val Target.srcs: List<String>
  @ApiStatus.Internal
  get() = getListAttribute(SRCS_NAME)

private const val RESOURCES_NAME = "resources"
val Target.resources: List<String>
  @ApiStatus.Internal
  get() = getListAttribute(RESOURCES_NAME)

private val compileDeps = listOf("deps", "jars", "exports", "associates", "proc_macro_deps")
private val runtimeDeps = listOf("runtime_deps")
val Target.interestingDeps: List<String>
  @ApiStatus.Internal
  get() = (compileDeps + runtimeDeps).flatMap { getListAttribute(it) }

private const val GENERATOR_NAME = "generator_name"
val Target.generatorName: String?
  @ApiStatus.Internal
  get() = getStringAttribute(GENERATOR_NAME)

internal fun Target.getListAttribute(name: String): List<String> =
  rule.attributeList
    .firstOrNull { it.name == name }
    ?.stringListValueList
    .orEmpty()

internal fun Target.getStringAttribute(name: String): String? =
  rule.attributeList
    .firstOrNull { it.name == name }
    ?.stringValue

val Target.isManual: Boolean
  @ApiStatus.Internal
  get() = BuildTargetTag.MANUAL in tags

val Target.isNoIde: Boolean
  @ApiStatus.Internal
  get() = BuildTargetTag.NO_IDE in tags
