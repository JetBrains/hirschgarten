load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "generator",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/generator/sections",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model",
    ],
)
