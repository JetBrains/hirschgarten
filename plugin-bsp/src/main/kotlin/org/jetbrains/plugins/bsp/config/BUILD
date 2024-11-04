load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "config",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bsp:__subpackages__"],
    deps = [
        "//plugin-bsp/src:intellij-bsp-resources",
        "//protocol",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
