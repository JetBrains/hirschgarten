load("@rules_java//java:java_library.bzl", "java_library")

# A plain (non-test, non-testonly) library. Its sources are marked as test only
# because the project view's test_sources glob matches this src/ directory (see
# the workspace-root .bazelproject). Because a single target owns src/ and all of
# its files match the glob, the whole src/ source root is a test source root.

java_library(
    name = "sources",
    srcs = [
        "src/FirstSource.java",
        "src/SecondSource.java",
    ],
)
