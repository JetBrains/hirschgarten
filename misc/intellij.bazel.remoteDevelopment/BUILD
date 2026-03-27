### auto-generated section `iml intellij.bazel.remoteDevelopment` start
exports_files([
  "intellij.bazel.remoteDevelopment.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.remoteDevelopment` end

### auto-generated section `build intellij.bazel.remoteDevelopment` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-remoteDevelopment_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-remoteDevelopment",
  module_name = "intellij.bazel.remoteDevelopment",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-remoteDevelopment_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.remoteDevelopment` end