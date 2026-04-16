### auto-generated section `iml intellij.bazel.java.profiler` start
exports_files([
  "intellij.bazel.java.profiler.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.java.profiler` end

### auto-generated section `build intellij.bazel.java.profiler` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-java-profiler_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-java-profiler",
  module_name = "intellij.bazel.java.profiler",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-java-profiler_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//java/intellij.bazel.java.common:bazel-java-common",
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.java.profiler` end