package org.jetbrains.bazel.sync.workspace.mapper.phased

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Attribute
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Rule
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target

fun createMockTarget(
  name: String,
  kind: String,
  deps: List<String> = emptyList(),
  exports: List<String> = emptyList(),
  runtimeDeps: List<String> = emptyList(),
  srcs: List<String> = emptyList(),
  resources: List<String> = emptyList(),
  tags: List<String> = emptyList(),
): Target {
  val rule =
    Rule
      .newBuilder()
      .setName(name)
      .setRuleClass(kind)
      .addAttribute(("deps" to deps).toListAttribute())
      .addAttribute(("exports" to exports).toListAttribute())
      .addAttribute(("runtime_deps" to runtimeDeps).toListAttribute())
      .addAttribute(("srcs" to srcs).toListAttribute())
      .addAttribute(("resources" to resources).toListAttribute())
      .addAttribute(("tags" to tags).toListAttribute())
      .build()

  return Target
    .newBuilder()
    .setType(Target.Discriminator.RULE)
    .setRule(rule)
    .build()
}

fun Pair<String, List<String>>.toListAttribute(): Attribute =
  Attribute
    .newBuilder()
    .setName(first)
    .setType(Attribute.Discriminator.STRING_LIST)
    .addAllStringListValue(second)
    .build()
