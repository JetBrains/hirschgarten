### auto-generated section `iml intellij.bazel.projectview` start
exports_files([
  "intellij.bazel.projectview.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.projectview` end

### auto-generated section `build intellij.bazel.projectview` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "projectview_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "projectview",
  module_name = "intellij.bazel.projectview",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":projectview_resources"],
  deps = [
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.projectview` end