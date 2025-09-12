load("@rules_java//java:defs.bzl", "java_library", "java_test")

java_test(
    name = "BuildArtifactCacheTest",
    size = "small",
    srcs = ["BuildArtifactCacheTest.java"],
    deps = [
        ":test_utils",
        "//og/shared/src/main/java/com/google/idea/blaze/common/artifact",
        "//og/shared/src/test/java/com/google/idea/blaze/common:test_utils",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
    ],
)

java_library(
    name = "test_utils",
    srcs = [
        "MockArtifact.java",
        "TestArtifactFetcher.java",
        "TestOutputArtifact.java",
    ],
    deps = [
        "//og/shared/src/main/java/com/google/idea/blaze/common",
        "//og/shared/src/main/java/com/google/idea/blaze/common/artifact",
        "//og/shared/src/main/java/com/google/idea/blaze/exception",
        "//third_party/java/auto_value",
        "@com_google_guava_guava//jar",
        "@jsr305_annotations//jar",
    ],
)
