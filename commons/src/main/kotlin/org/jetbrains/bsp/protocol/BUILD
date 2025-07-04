load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "protocol",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bazel/info",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
    ],
)
