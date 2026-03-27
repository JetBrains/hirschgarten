### auto-generated section `iml intellij.bazel.terminal` start
exports_files([
  "intellij.bazel.terminal.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.terminal` end

### auto-generated section `build intellij.bazel.terminal` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-terminal_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-terminal",
  module_name = "intellij.bazel.terminal",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-terminal_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.projectview:projectview",
  ]
)
### auto-generated section `build intellij.bazel.terminal` end