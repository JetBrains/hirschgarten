load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "elements",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/base",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/lexer",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/psi",
    ],
)
