load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "bzlmod",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//plugin-bazel/server:__subpackages__"],
    deps = [
        "//plugin-bazel/bazelrunner",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
    ],
)
