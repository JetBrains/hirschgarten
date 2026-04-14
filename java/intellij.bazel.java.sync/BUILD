### auto-generated section `iml intellij.bazel.java.sync` start
exports_files([
  "intellij.bazel.java.sync.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.java.sync` end

### auto-generated section `build intellij.bazel.java.sync` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-java-sync_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-java-sync",
  module_name = "intellij.bazel.java.sync",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-java-sync_resources"],
  deps = [
    "//intellij.bazel.commons:commons",
    "//protobuf",
    "//intellij.bazel.importer:importer",
    "//intellij.bazel.projectview:projectview",
  ]
)
### auto-generated section `build intellij.bazel.java.sync` end