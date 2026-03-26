### auto-generated section `iml intellij.bazel.kotlin.k2` start
exports_files([
  "intellij.bazel.kotlin.k2.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.kotlin.k2` end

### auto-generated section `build intellij.bazel.kotlin.k2` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-kotlin-k2_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-kotlin-k2",
  module_name = "intellij.bazel.kotlin.k2",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-kotlin-k2_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//java/intellij.bazel.java.sync:bazel-java-sync",
  ]
)
### auto-generated section `build intellij.bazel.kotlin.k2` end