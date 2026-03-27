### auto-generated section `iml intellij.bazel.protoedit` start
exports_files([
  "intellij.bazel.protoedit.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.protoedit` end

### auto-generated section `build intellij.bazel.protoedit` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-protoedit_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-protoedit",
  module_name = "intellij.bazel.protoedit",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-protoedit_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.importer:importer",
  ]
)
### auto-generated section `build intellij.bazel.protoedit` end