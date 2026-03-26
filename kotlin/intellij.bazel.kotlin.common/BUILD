### auto-generated section `iml intellij.bazel.kotlin.common` start
exports_files([
  "intellij.bazel.kotlin.common.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.kotlin.common` end

### auto-generated section `build intellij.bazel.kotlin.common` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-kotlin-common_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-kotlin-common",
  module_name = "intellij.bazel.kotlin.common",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-kotlin-common_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//java/intellij.bazel.java.common:bazel-java-common",
    "//intellij.bazel.importer:importer",
  ]
)
### auto-generated section `build intellij.bazel.kotlin.common` end