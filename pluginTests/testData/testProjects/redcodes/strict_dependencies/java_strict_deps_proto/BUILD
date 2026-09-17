load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_java//java:java_library.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_proto_library(
    name = "worker_protocol_java_proto",
    deps = ["@bazel_worker_api//:worker_protocol_proto"],
)

java_library(
    name = "work_request_handlers",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
        ],
        allow_empty = True,
    ),
    deps = [
        "//:worker_protocol_java_proto",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
    ],
)
