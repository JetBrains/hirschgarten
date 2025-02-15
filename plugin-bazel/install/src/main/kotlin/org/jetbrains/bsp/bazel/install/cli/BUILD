load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "cli",
    srcs = glob(["*.kt"]),
    visibility = [
        "//plugin-bazel/install/src/main/kotlin/org/jetbrains/bsp/bazel/install:__subpackages__",
        "//plugin-bazel/install/src/test/kotlin/org/jetbrains/bsp/bazel/install/cli:__pkg__",
    ],
    deps = [
        "//plugin-bazel/commons",
        "@maven//:commons_cli_commons_cli",
    ],
)
