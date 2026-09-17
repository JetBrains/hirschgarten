load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "hello_proto",
    srcs = ["hello.proto"],
)

# triggers bazel_java_proto_aspect over `:hello_proto`, producing a JVM shadow
# alongside the plain proto_library node. The `JavaLanguagePlugin` collapse heuristic
# must pick the JVM shadow for `App.java` to resolve the generated Hello class
java_proto_library(
    name = "hello_java_proto",
    deps = [":hello_proto"],
)
