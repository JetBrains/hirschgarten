load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "client",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/taskEvents",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/ids",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
