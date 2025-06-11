load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "utils",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/params",
    ],
)
