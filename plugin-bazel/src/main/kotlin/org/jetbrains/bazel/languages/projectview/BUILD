load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "projectview",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/bazelrc",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/starlark",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
    ],
)
