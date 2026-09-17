load(":defs.bzl", "aggregator", "schema")

schema(name = "my_schema")

aggregator(
    name = "my_schema_java",
    deps = [":my_schema"],
    visibility = ["//visibility:public"],
)
