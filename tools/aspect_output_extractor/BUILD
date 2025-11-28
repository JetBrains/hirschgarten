"""
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "aspect_output_extractor",
    srcs = ["AspectOutputExtractor.kt"],
    main_class = "org.jetbrains.bazel.server.AspectOutputExtractor",
    resource_strip_prefix = "server/resources/aspects",
    resources = ["//server/resources/aspects"],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
"""