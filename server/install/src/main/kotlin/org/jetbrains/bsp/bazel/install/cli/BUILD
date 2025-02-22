load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "cli",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install:__subpackages__",
        "//server/install/src/test/kotlin/org/jetbrains/bazel/install/cli:__pkg__",
    ],
    deps = [
        "//server/commons",
        "@maven//:commons_cli_commons_cli",
    ],
)
