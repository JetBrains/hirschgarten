load("@aspect_bazel_lib//lib:output_files.bzl", "output_files")
load("@aspect_bazel_lib//lib:write_source_files.bzl", "write_source_files")

output_files(
    name = "bsp_target_info_proto_jar",
    paths = ["server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto/libbsp_target_info_proto-speed.jar"],
    target = "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
)

output_files(
    name = "googleapis_build_proto_jar",
    paths = ["../googleapis+/google/devtools/build/v1/libbuild_proto-speed.jar"],
    target = "@googleapis//google/devtools/build/v1:build_java_proto",
)

output_files(
    name = "googleapis_build_grpc_jar",
    paths = ["../googleapis+/google/devtools/build/v1/libbuild_java_grpc.jar"],
    target = "@googleapis//google/devtools/build/v1:build_java_grpc",
)

output_files(
    name = "bazel_build_proto_jar",
    paths = ["third_party/bazel/src/main/protobuf/libbuild_proto-speed.jar"],
    target = "//third_party/bazel/src/main/protobuf:build_java_proto",
)

output_files(
    name = "build_event_stream_proto_jar",
    paths = ["third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto/libbuild_event_stream_proto-speed.jar"],
    target = "//third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto:build_event_stream_java_proto",
)

output_files(
    name = "deps_proto_jar",
    paths = ["third_party/bazel/src/main/protobuf/libdeps_proto-speed.jar"],
    target = "//third_party/bazel/src/main/protobuf:deps_java_proto",
)

write_source_files(
    name = "check_in_proto_jar",
    files = {
        "bsp_target_info_proto.jar": ":bsp_target_info_proto_jar",
        "googleapis_build_proto.jar": ":googleapis_build_proto_jar",
        "googleapis_build_grpc.jar": ":googleapis_build_grpc_jar",
        "build_event_stream_proto.jar": ":build_event_stream_proto_jar",
        "deps_proto.jar": ":deps_proto_jar",
        "bazel_build_proto.jar": ":bazel_build_proto_jar",
    },
)
