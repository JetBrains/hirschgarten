load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

# nasz parser
kt_jvm_library(
    name = "parser",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
            "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/lexer",
            "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview/elements",
    ],
)
