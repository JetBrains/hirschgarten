### auto-generated section `iml intellij.bazel.golang.common` start
exports_files([
  "intellij.bazel.golang.common.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.golang.common` end

### auto-generated section `build intellij.bazel.golang.common` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-golang-common_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-golang-common",
  module_name = "intellij.bazel.golang.common",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-golang-common_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.importer:importer",
  ]
)
### auto-generated section `build intellij.bazel.golang.common` end