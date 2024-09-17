load("@rules_intellij//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "v243",
    srcs = select_for_plugin_api({
        "intellij-2024.2": ["Dummy.kt"],
        "intellij-2024.3": glob([
            "org/jetbrains/bsp/sdkcompat/android/**",
        ]),
    }),
    visibility = ["//sdkcompat:__pkg__"],
    deps = [
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
    ],
)
