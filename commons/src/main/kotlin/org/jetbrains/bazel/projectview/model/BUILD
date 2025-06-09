load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "model",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model/sections",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model/sections",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
