load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "annotation_converter",
    main_class = "AnnotationConverter",
    deps = [
        "@maven//:com_github_javaparser_javaparser_core",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//tools/starlark_data_generation/common:commons",
    ],
    srcs = glob([
        "src/**/*.kt"
    ]),
)

java_proto_library(
    name = "annotation_converter_java_proto",
    visibility = ["//visibility:public"],
    deps = [],
)
