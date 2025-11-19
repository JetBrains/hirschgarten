load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "server_resources",
    resources = ["//server/resources/aspects"],
    resource_strip_prefix = "server/resources",
)

kt_jvm_library(
    name = "server",
    srcs = glob(
        [
            "server/src/main/kotlin/**/*.kt",
            "server/src/main/kotlin/**/*.java",
            "server/src/main/kotlin/**/*.form",
            "logger/src/main/kotlin/**/*.kt",
            "logger/src/main/kotlin/**/*.java",
            "logger/src/main/kotlin/**/*.form",
            "install/src/main/kotlin/**/*.kt",
            "install/src/main/kotlin/**/*.java",
            "install/src/main/kotlin/**/*.form",
            "commons/src/main/kotlin/**/*.kt",
            "commons/src/main/kotlin/**/*.java",
            "commons/src/main/kotlin/**/*.form",
            "bazelrunner/src/main/kotlin/**/*.kt",
            "bazelrunner/src/main/kotlin/**/*.java",
            "bazelrunner/src/main/kotlin/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.server",
    visibility = ["//visibility:public"],
    deps = [
        ":server_resources",
        "//commons",
        "//protobuf",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml",
        "@maven//:com_fasterxml_jackson_module_jackson_module_kotlin",
    ],
)
