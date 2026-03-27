### auto-generated section `iml intellij.bazel.devkit.monorepo` start
exports_files([
  "intellij.bazel.devkit.monorepo.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.devkit.monorepo` end

### auto-generated section `build intellij.bazel.devkit.monorepo` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

resourcegroup(
  name = "bazel-devkit-monorepo_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "bazel-devkit-monorepo",
  module_name = "intellij.bazel.devkit.monorepo",
  visibility = ["//visibility:public"],
  # Monorepo DevKit isn't published, we can't build against it
  srcs = glob([], allow_empty = True),
  resources = [":bazel-devkit-monorepo_resources"],
  deps = []
)
### auto-generated section `build intellij.bazel.devkit.monorepo` end