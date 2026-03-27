### auto-generated section `iml intellij.bazel.connector` start
exports_files([
  "intellij.bazel.connector.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.connector` end

### auto-generated section `build intellij.bazel.connector` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "connector_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "connector",
  module_name = "intellij.bazel.connector",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":connector_resources"],
  deps = [
    "//intellij.bazel.commons:commons",
    "//protobuf",
  ],
  deps_to_bundle = [
    "@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml",
  ],
)
### auto-generated section `build intellij.bazel.connector` end