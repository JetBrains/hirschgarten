load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "connection",
    srcs = glob(["*.kt"]),
    # Do not depend on this target because it exports the Bazel BSP server dependency!
    # Do not change this to __subpackages__
    visibility = ["//plugin-bazel/src:__pkg__"],
    deps = [
        "//plugin-bazel/src:intellij-bsp",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/flow",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/settings",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel:bsp_export",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
