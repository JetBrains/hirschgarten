load("@rules_java//java:java_test.bzl", "java_test")

# A single java_test owns every source under src/. Because one test target owns
# the whole directory, the entire src/ source root is marked as test.

java_test(
    name = "test",
    srcs = [
        "src/JavaTest.java",
        "src/TestHelper.java",
    ],
    test_class = "testRunner.JavaTest",
    deps = ["@maven//:junit_junit"],
)
