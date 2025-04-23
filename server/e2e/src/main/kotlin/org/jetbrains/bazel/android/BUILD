load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "android",
    srcs = glob(["*.kt"]),
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
        "@maven//:commons_io_commons_io",
    ],
)
