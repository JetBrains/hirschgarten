load("@rules_java//java:java_library.bzl", "java_library")

# A single testonly java_library owns every source under src/. Because one
# test-only target owns the whole directory, the entire src/ source root is
# marked as test.

java_library(
    name = "test_library",
    testonly = True,
    srcs = [
        "src/TestFixtures.java",
        "src/TestHelpers.java",
    ],
)
