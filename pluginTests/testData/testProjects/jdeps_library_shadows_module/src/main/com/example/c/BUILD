load("@rules_java//java:java_library.bzl", "java_library")

# Demo of how a DEPENDENT of B sees A.
# C depends on B and uses A. A reaches B only through M's `exports`, and B does NOT re-export A, so A is
# not on C's classpath: in the IDE `new A()` is UNRESOLVED (matching Bazel, which fails to build C). That
# unresolved reference is what surfaces IntelliJ's "Add dependency on module 'A'" quick fix
# (OrderEntryFix -> BazelProjectModelModifier), which adds //src/main/com/example/a:A to this target's deps.
# `bazel build` of this target fails the strict-deps check, which is intentional; the workspace-model
# import does not compile it.
java_library(
    name = "C",
    srcs = ["C.java"],
    deps = [
        "//src/main/com/example/b:B",
    ],
)
