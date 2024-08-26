load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "action",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:ch_epfl_scala_bsp4j",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
                "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
        "@rules_intellij//third_party/code_with_me",
        "@rules_intellij//third_party/devkit",
        "@rules_intellij//third_party/performance",
        "@rules_intellij//third_party/python",
    ],
)
