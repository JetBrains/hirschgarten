load("@rules_java//java:java_library.bzl", "java_library")
load(":defs.bzl", "gen_java_library", "schema")

schema(
    name = "my_schema",
)

gen_java_library(
    name = "my_schema_java",
    deps = [":my_schema"],
)

java_library(
    name = "app",
    srcs = ["App.java"],
    deps = [":my_schema_java"],
)
