load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "logger",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//server/logger:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
    ],
)
