### auto-generated section `iml intellij.bazel.bytecodeViewer` start
exports_files([
  "intellij.bazel.bytecodeViewer.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.bytecodeViewer` end

### auto-generated section `build intellij.bazel.bytecodeViewer` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-bytecodeViewer_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-bytecodeViewer",
  module_name = "intellij.bazel.bytecodeViewer",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-bytecodeViewer_resources"],
  deps = [
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.core:core",
  ]
)
### auto-generated section `build intellij.bazel.bytecodeViewer` end