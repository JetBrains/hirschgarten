### auto-generated section `iml intellij.bazel.devkit` start
exports_files([
  "intellij.bazel.devkit.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.devkit` end

### auto-generated section `build intellij.bazel.devkit` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-devkit_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-devkit",
  module_name = "intellij.bazel.devkit",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-devkit_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//protobuf",
  ]
)
### auto-generated section `build intellij.bazel.devkit` end