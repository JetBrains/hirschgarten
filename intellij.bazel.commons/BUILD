load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
  name = "commons_resources",
  visibility = ["//visibility:public"],
  resources = glob(["resources/**/*"]),
  resource_strip_prefix = "intellij.bazel.commons/resources",
)

kt_jvm_library(
  name = "commons",
  module_name = "intellij.bazel.commons",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  deps = [
    "//protobuf",
    "//rules_intellij/intellij_platform_sdk:plugin_api",
  ]
)
