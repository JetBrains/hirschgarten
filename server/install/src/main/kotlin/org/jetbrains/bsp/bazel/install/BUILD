load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "install",
    srcs = glob(["*.kt"]),
    resources = ["//server/aspects"],
    visibility = [
        "//server/bspcli:__subpackages__",
        "//server/e2e:__subpackages__",
        "//server/install:__subpackages__",
    ],
    deps = [
        "//server/commons",
        "//server/executioncontext/installationcontext",
        "//server/executioncontext/projectview:generator",
        "//server/executioncontext/projectview:parser",
        "//server/install/src/main/kotlin/org/jetbrains/bsp/bazel/install/cli",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@maven//:commons_cli_commons_cli",
    ],
)
