### auto-generated section `iml intellij.bazel.kotlin.sync` start
exports_files([
  "intellij.bazel.kotlin.sync.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.kotlin.sync` end

### auto-generated section `build intellij.bazel.kotlin.sync` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-kotlin-sync_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-kotlin-sync",
  module_name = "intellij.bazel.kotlin.sync",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-kotlin-sync_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.importer:importer",
    "//kotlin/intellij.bazel.kotlin.common:bazel-kotlin-common",
    "//java/intellij.bazel.java.sync:bazel-java-sync",
  ]
)
### auto-generated section `build intellij.bazel.kotlin.sync` end