### auto-generated section `iml intellij.bazel.python.backend` start
exports_files([
  "intellij.bazel.python.backend.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.python.backend` end

### auto-generated section `build intellij.bazel.python.backend` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-python-backend_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-python-backend",
  module_name = "intellij.bazel.python.backend",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":bazel-python-backend_resources"],
  deps = [
    "//intellij.bazel.core:core",
    "//intellij.bazel.commons:commons",
    "//intellij.bazel.importer:importer",
    "//python/intellij.bazel.python.common:bazel-python-common",
  ]
)
### auto-generated section `build intellij.bazel.python.backend` end
