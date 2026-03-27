### auto-generated section `build intellij.bazel.protobuf` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "protobuf_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "protobuf",
  module_name = "intellij.bazel.protobuf",
  visibility = ["//visibility:public"],
  srcs = glob(["src/main/gen/**/*.kt", "src/main/gen/**/*.java"], allow_empty = True),
  resources = [":protobuf_resources"],
  deps = []
)
### auto-generated section `build intellij.bazel.protobuf` end