load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules_intellij/build_defs:build_defs.bzl", "intellij_module")

kt_jvm_library(
  name = "k2_resources",
  resources = glob(["resources/**/*"]),
  resource_strip_prefix = "k2/resources",
)

kt_jvm_library(
  name = "k2",
  module_name = "intellij.bazel.kotlin.k2",
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  deps = [
    ":k2_resources",
    "//plugin-bazel:plugin_neverlink",
  ]
)

intellij_module(
  name = "intellij.bazel.kotlin.k2",
  visibility = ["//visibility:public"],
  deps = ["//k2"],
)
