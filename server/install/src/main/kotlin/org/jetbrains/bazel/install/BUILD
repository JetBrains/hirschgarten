load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "install",
    srcs = glob(["*.kt"]),
    resource_strip_prefix = "server/resources",
    resources = ["//server/resources/aspects"],
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/generator",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install/cli",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/utils:fileutils",
    ],
)
