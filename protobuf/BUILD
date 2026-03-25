load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_shell//shell:sh_binary.bzl", "sh_binary")

kt_jvm_library(
    name = "protobuf",
    srcs = glob(
        [
            "src/main/gen/**/*.kt",
            "src/main/gen/**/*.java",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.protobuf",
    visibility = ["//visibility:public"],
    deps = [
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
