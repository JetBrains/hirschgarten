### auto-generated section `iml intellij.bazel.python.common` start
exports_files([
  "intellij.bazel.python.common.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.python.common` end

### auto-generated section `build intellij.bazel.python.common` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-python-common_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-python-common",
  module_name = "intellij.bazel.python.common",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-python-common_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.importer:importer",
  ]
)
### auto-generated section `build intellij.bazel.python.common` end