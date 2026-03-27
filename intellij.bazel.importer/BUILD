### auto-generated section `iml intellij.bazel.importer` start
exports_files([
  "intellij.bazel.importer.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.importer` end

### auto-generated section `build intellij.bazel.importer` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "importer_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "importer",
  module_name = "intellij.bazel.importer",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form", "gen/**/*.kt", "gen/**/*.java"], allow_empty = True),
  resources = [":importer_resources"],
  deps = [
    "//intellij.bazel.commons:commons",
    "//protobuf",
    "//intellij.bazel.connector:connector",
    "//intellij.bazel.projectview:projectview",
  ]
)
### auto-generated section `build intellij.bazel.importer` end