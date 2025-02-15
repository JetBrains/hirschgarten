load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "connection",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/install/src/main/kotlin/org/jetbrains/bsp/bazel/install:install",
        "//plugin-bazel/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/flow/open/wizard",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/chunking",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/client",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console/ids",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
