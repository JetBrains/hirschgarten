load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "parser",
    srcs = glob(["*.kt"]),
    resources = ["//server/log4j_config"],
    visibility = ["//visibility:public"],
    exports = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser/sections",
        "//server/commons",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
