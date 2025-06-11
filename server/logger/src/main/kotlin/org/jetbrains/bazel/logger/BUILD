load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
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
