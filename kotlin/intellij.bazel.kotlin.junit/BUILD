### auto-generated section `iml intellij.bazel.kotlin.junit` start
exports_files([
  "intellij.bazel.kotlin.junit.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.kotlin.junit` end

### auto-generated section `build intellij.bazel.kotlin.junit` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-kotlin-junit_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-kotlin-junit",
  module_name = "intellij.bazel.kotlin.junit",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-kotlin-junit_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//java/intellij.bazel.java.junit:bazel-java-junit",
  ]
)
### auto-generated section `build intellij.bazel.kotlin.junit` end