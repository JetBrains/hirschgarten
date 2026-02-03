load("@rules_proto//proto:defs.bzl", "proto_lang_toolchain")

proto_lang_toolchain(
    name = "protoc_java_toolchain",
    command_line = "--java_out=%s",
    progress_message = "Generating Java proto_library %{label}",
    runtime = "@maven//:com_google_protobuf_protobuf_java",
    toolchain_type = "@rules_java//java/proto:toolchain_type",
)
