load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "commons",
    srcs = glob(["*.kt"]),
    visibility = ["//server/commons:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_logging_log4j_log4j_api",
    ],
)
