load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_test.bzl", "java_test")

# Several targets share the src/ directory and mix test and production sources,
# so the src/ source root must not be marked as test even though it contains a
# test source. SharedSource.java is used by both the test and the binary, so it
# must not be marked as test either.

java_library(
    name = "production_library",
    srcs = ["src/ProductionLibrary.java"],
)

java_test(
    name = "test",
    srcs = [
        "src/JavaTest.java",
        "src/SharedSource.java",
    ],
    test_class = "mixedSources.JavaTest",
    deps = ["@maven//:junit_junit"],
)

java_binary(
    name = "binary",
    srcs = [
        "src/Main.java",
        "src/SharedSource.java",
    ],
    main_class = "mixedSources.Main",
    deps = [":production_library"],
)
