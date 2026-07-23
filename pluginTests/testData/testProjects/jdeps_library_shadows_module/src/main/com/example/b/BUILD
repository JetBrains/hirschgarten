load("@rules_java//java:java_library.bzl", "java_library")

# In project scope (a root in .bazelproject) -> imported as a source MODULE.
# Depends on M (not directly on A); M re-exports A, so B compiles against A's class.
# B's .jdeps therefore references A's header jar.
java_library(
    name = "B",
    srcs = ["B.java"],
    visibility = ["//visibility:public"],
    deps = ["//src/main/com/example/mid:M"],
)
