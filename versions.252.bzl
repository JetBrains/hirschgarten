"""Release versions of plugins. The file is swaped with versions.bzl on CI during release"""

INTELLIJ_BAZEL_VERSION = "2025.2.12"

PLATFORM_VERSION = "252"

# make sure to always choose the lowest among different benchmark build numbers
SINCE_VERSION = "252.23892"
UNTIL_VERSION = "252.*"

BENCHMARK_BUILD_NUMBER = "252.23892.409"
PY_BENCHMARK_BUILD_NUMBER = "252.23892.515"
GO_BENCHMARK_BUILD_NUMBER = "252.23892.449"
