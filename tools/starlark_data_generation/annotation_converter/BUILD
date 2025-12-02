"""
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "annotation_converter",
    srcs = glob([
        "src/**/*.kt",
    ]),
    main_class = "AnnotationConverter",
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/starlark",
        "//tools/starlark_data_generation/common:commons",
        "@maven//:com_github_javaparser_javaparser_core",
    ],
)

java_proto_library(
    name = "annotation_converter_java_proto",
    visibility = ["//visibility:public"],
    deps = [],
)
"""