### auto-generated section `build intellij.bazel.bazelisk` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazelisk_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazelisk",
  module_name = "intellij.bazel.bazelisk",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazelisk_resources"],
  deps = [
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.bazelisk` end