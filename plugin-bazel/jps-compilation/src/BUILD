load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "jps-compilation",
    srcs = glob([
        "main/kotlin/**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
