load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("//rules/kotlin:macros.bzl", "kt_binary")

kt_binary(
    name = "stardoc-proto-reader",
    srcs = glob([
        "src/**/*.kt",
    ]),
    main_class = "ProtobufReader",
    deps = [
        ":stardoc_output_java_proto",
        "@maven//:com_google_code_gson_gson",
    ],
)

java_proto_library(
    name = "stardoc_output_java_proto",
    visibility = ["//visibility:public"],
    deps = [
        "@stardoc//stardoc/proto:stardoc_output_proto",
    ],
)
