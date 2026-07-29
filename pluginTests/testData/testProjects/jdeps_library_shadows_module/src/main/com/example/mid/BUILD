load("@rules_java//java:java_library.bzl", "java_library")

# NOT a root in .bazelproject and only reachable as a transitive dep of //src/main/com/example/b:B.
# With import_depth: 0 it is therefore demoted to a library rather than a source module.
# It re-exports A so that B can compile against A's class while only depending on M.
java_library(
    name = "M",
    visibility = ["//visibility:public"],
    exports = ["//src/main/com/example/a:A"],
)
