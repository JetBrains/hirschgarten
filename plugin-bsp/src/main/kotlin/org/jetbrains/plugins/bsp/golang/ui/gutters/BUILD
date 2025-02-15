load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "gutters",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bsp:__subpackages__"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/gutters",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/go",
    ],
)
