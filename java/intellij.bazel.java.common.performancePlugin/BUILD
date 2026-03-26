### auto-generated section `iml intellij.bazel.java.common.performancePlugin` start
exports_files([
  "intellij.bazel.java.common.performancePlugin.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.java.common.performancePlugin` end

### auto-generated section `build intellij.bazel.java.common.performancePlugin` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-java-common-performancePlugin_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-java-common-performancePlugin",
  module_name = "intellij.bazel.java.common.performancePlugin",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-java-common-performancePlugin_resources"],
  deps = [
    "//intellij.bazel.core.performancePlugin:core-performancePlugin",
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//java/intellij.bazel.java.common:bazel-java-common",
  ]
)
### auto-generated section `build intellij.bazel.java.common.performancePlugin` end