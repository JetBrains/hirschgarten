load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "provider",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/model:model",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser:parser",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/generator:generator",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol:protocol",
        "@maven//:org_slf4j_slf4j_api",
    ],
)