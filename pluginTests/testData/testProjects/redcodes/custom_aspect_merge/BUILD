load(":defs.bzl", "message_library", "schema", "stub_library")

schema(name = "my_schema")

message_library(
    name = "my_schema_message",
    deps = [":my_schema"],
    visibility = ["//visibility:public"],
)

stub_library(
    name = "my_schema_stub",
    deps = [":my_schema"],
    visibility = ["//visibility:public"],
)
