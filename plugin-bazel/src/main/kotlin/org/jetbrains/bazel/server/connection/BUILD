load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "connection",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/client",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/settings/bazel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/ids",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//protocol",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
