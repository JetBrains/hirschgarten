package org.jetbrains.bazel.commons.phased

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target

val Target.name: String
  get() = rule.name

val Target.kind: String
  get() = rule.ruleClass

private const val TAGS_NAME = "tags"
val Target.tags: List<String>
  get() = getListAttribute(TAGS_NAME)

private const val SRCS_NAME = "srcs"
val Target.srcs: List<String>
  get() = getListAttribute(SRCS_NAME)

private const val RESOURCES_NAME = "resources"
val Target.resources: List<String>
  get() = getListAttribute(RESOURCES_NAME)

private val compileDeps = listOf("deps", "jars", "exports", "associates", "proc_macro_deps")
private val runtimeDeps = listOf("runtime_deps")
val Target.interestingDeps: List<String>
  get() = (compileDeps + runtimeDeps).flatMap { getListAttribute(it) }

fun Target.getListAttribute(name: String): List<String> =
  rule.attributeList
    .firstOrNull { it.name == name }
    ?.stringListValueList
    .orEmpty()

private const val BAZEL_MANUAL_TAG = "manual"
val Target.isManual: Boolean
  get() = BAZEL_MANUAL_TAG in tags

private const val BAZEL_NO_IDE_TAG = "no-ide"
val Target.isNoIde: Boolean
  get() = BAZEL_NO_IDE_TAG in tags

// TODO: https://youtrack.jetbrains.com/issue/BAZEL-1556
val Target.isBinary: Boolean
  get() = kind.endsWith("_binary") || kind == "intellij_plugin_debug_target"

// TODO: https://youtrack.jetbrains.com/issue/BAZEL-1556
val Target.isTest: Boolean
  get() = kind.endsWith("_test")
