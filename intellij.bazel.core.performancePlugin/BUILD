### auto-generated section `iml intellij.bazel.core.performancePlugin` start
exports_files([
  "intellij.bazel.core.performancePlugin.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.core.performancePlugin` end

### auto-generated section `build intellij.bazel.core.performancePlugin` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "core-performancePlugin_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "core-performancePlugin",
  module_name = "intellij.bazel.core.performancePlugin",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":core-performancePlugin_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.connector:connector",
    "//intellij.bazel.bazelisk:bazelisk",
    "//intellij.bazel.importer:importer",
    "//intellij.bazel.projectview:projectview",
    "//intellij.bazel.commons:commons",
    "//protobuf",
  ]
)
### auto-generated section `build intellij.bazel.core.performancePlugin` end