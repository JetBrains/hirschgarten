### auto-generated section `iml intellij.bazel.python.common.performancePlugin` start
exports_files([
  "intellij.bazel.python.common.performancePlugin.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.python.common.performancePlugin` end

### auto-generated section `build intellij.bazel.python.common.performancePlugin` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-python-common-performancePlugin_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-python-common-performancePlugin",
  module_name = "intellij.bazel.python.common.performancePlugin",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-python-common-performancePlugin_resources"],
  deps = [
    "//python/intellij.bazel.python.common:bazel-python-common",
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.python.common.performancePlugin` end