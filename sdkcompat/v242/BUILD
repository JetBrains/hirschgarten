load("@rules_intellij//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "v242",
    srcs = select_for_plugin_api({
        "intellij-2024.2": glob([
            "org/jetbrains/bsp/sdkcompat/android/**",
        ]),
        "intellij-2024.3": ["Dummy.kt"],
    }),
    visibility = ["//sdkcompat:__pkg__"],
    deps = [
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
    ],
)
