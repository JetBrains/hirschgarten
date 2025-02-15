load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "install",
    srcs = glob(["*.kt"]),
    resource_strip_prefix = "plugin-bazel",
    resources = ["//plugin-bazel/aspects"],
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
        "//plugin-bazel/executioncontext/installationcontext",
        "//plugin-bazel/executioncontext/projectview:generator",
        "//plugin-bazel/executioncontext/projectview:parser",
        "//plugin-bazel/install/src/main/kotlin/org/jetbrains/bsp/bazel/install/cli",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/utils:fileutils",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@maven//:commons_cli_commons_cli",
    ],
)
