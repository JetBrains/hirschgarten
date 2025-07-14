load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "params",
    srcs = glob(["*.kt"]),
    visibility = ["//server/bazelrunner:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
    ],
)
