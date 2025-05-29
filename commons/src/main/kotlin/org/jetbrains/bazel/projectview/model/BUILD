load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
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
