load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "logger",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//server/logger:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/testing",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
    ],
)
