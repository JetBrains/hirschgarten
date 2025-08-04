load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "projectview",
    srcs = glob(["**/*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/bazelrc",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
    ],
)
