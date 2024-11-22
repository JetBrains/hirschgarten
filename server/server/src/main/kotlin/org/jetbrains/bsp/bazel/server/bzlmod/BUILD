load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "bzlmod",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "//server/bazelrunner",
    ],
)
