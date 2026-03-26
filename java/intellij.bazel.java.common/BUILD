### auto-generated section `iml intellij.bazel.java.common` start
exports_files([
  "intellij.bazel.java.common.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.java.common` end

### auto-generated section `build intellij.bazel.java.common` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-java-common_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-java-common",
  module_name = "intellij.bazel.java.common",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-java-common_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.importer:importer",
    "//intellij.bazel.connector:connector",
    "//intellij.bazel.projectview:projectview",
    "//java/intellij.bazel.java.sync:bazel-java-sync",
  ]
)
### auto-generated section `build intellij.bazel.java.common` end