### auto-generated section `iml intellij.bazel.kotlin.projectWizard` start
exports_files([
  "intellij.bazel.kotlin.projectWizard.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.kotlin.projectWizard` end

### auto-generated section `build intellij.bazel.kotlin.projectWizard` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-kotlin-projectWizard_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-kotlin-projectWizard",
  module_name = "intellij.bazel.kotlin.projectWizard",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-kotlin-projectWizard_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
  ]
)
### auto-generated section `build intellij.bazel.kotlin.projectWizard` end