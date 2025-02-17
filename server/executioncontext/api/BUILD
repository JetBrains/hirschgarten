load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "api",
    visibility = ["//server/executioncontext:__subpackages__"],
    exports = [
        "//server/executioncontext/api/src/main/kotlin/org/jetbrains/bsp/bazel/executioncontext/api",
    ],
)
