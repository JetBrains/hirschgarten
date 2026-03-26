### auto-generated section `iml intellij.bazel.coverage` start
exports_files([
  "intellij.bazel.coverage.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.coverage` end

### auto-generated section `build intellij.bazel.coverage` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-coverage_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-coverage",
  module_name = "intellij.bazel.coverage",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-coverage_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.coverage` end