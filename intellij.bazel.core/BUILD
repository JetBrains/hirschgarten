### auto-generated section `iml intellij.bazel.core` start
exports_files([
  "intellij.bazel.core.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.core` end

### auto-generated section `build intellij.bazel.core` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "core_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "core",
  module_name = "intellij.bazel.core",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":core_resources"],
  deps = [
    "//intellij.bazel.connector:connector",
    "//intellij.bazel.bazelisk:bazelisk",
    "//intellij.bazel.importer:importer",
    "//intellij.bazel.projectview:projectview",
    "//intellij.bazel.commons:commons",
    "//protobuf",
  ]
)
### auto-generated section `build intellij.bazel.core` end