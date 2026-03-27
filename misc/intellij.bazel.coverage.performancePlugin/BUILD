### auto-generated section `iml intellij.bazel.coverage.performancePlugin` start
exports_files([
  "intellij.bazel.coverage.performancePlugin.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.coverage.performancePlugin` end

### auto-generated section `build intellij.bazel.coverage.performancePlugin` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-coverage-performancePlugin_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-coverage-performancePlugin",
  module_name = "intellij.bazel.coverage.performancePlugin",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-coverage-performancePlugin_resources"],
  deps = [
    "//misc/intellij.bazel.coverage:bazel-coverage",
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.coverage.performancePlugin` end