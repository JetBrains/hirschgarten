load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "info",
    srcs = glob(
        [
            "*.kt",
            "*.java",
        ],
        allow_empty = True,
    ),
    visibility = ["//visibility:public"],
    exports = ["//commons/src/main/kotlin/org/jetbrains/bazel/label"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
    ],
)
