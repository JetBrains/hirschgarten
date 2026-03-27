### auto-generated section `iml intellij.bazel.java.junit` start
exports_files([
  "intellij.bazel.java.junit.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.java.junit` end

### auto-generated section `build intellij.bazel.java.junit` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-java-junit_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-java-junit",
  module_name = "intellij.bazel.java.junit",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-java-junit_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//java/intellij.bazel.java.common:bazel-java-common",
  ]
)
### auto-generated section `build intellij.bazel.java.junit` end