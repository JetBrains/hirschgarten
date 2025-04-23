load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "install",
    srcs = glob(["*.kt"]),
    resource_strip_prefix = "server/resources",
    resources = ["//server/resources/aspects"],
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/generator",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install/cli",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/utils:fileutils",
    ],
)
