### auto-generated section `iml intellij.bazel.java.coverage` start
exports_files([
  "intellij.bazel.java.coverage.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.java.coverage` end

### auto-generated section `build intellij.bazel.java.coverage` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-java-coverage_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-java-coverage",
  module_name = "intellij.bazel.java.coverage",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-java-coverage_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//misc/intellij.bazel.coverage:bazel-coverage",
  ]
)
### auto-generated section `build intellij.bazel.java.coverage` end