load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "parser",
    srcs = glob(["*.kt"]),
    resources = ["//server/log4j_config"],
    visibility = ["//visibility:public"],
    exports = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser/splitter",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview",
        "//server/commons",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
