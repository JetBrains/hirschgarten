load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
  name = "commons",
  module_name = "intellij.bazel.commons",
  visibility = ["//visibility:public"],
  srcs = glob(["src/main/kotlin/**/*.kt", "src/main/kotlin/**/*.java", "src/main/kotlin/**/*.form"], allow_empty = True),
  deps = [
    "//protobuf",
    "//rules_intellij/intellij_platform_sdk:plugin_api",
  ]
)
