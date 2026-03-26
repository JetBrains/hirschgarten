### auto-generated section `iml intellij.bazel.resource` start
exports_files([
  "intellij.bazel.resource.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.resource` end

### auto-generated section `build intellij.bazel.resource` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "resource_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "resource",
  module_name = "intellij.bazel.resource",
  visibility = ["//visibility:public"],
  srcs = glob([], allow_empty = True),
  resources = [":resource_resources"]
)
### auto-generated section `build intellij.bazel.resource` end