load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "commons",
    srcs = glob(["*.kt"]),
    visibility = ["//server/commons:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "@maven//:com_google_guava_guava",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
