"""Release versions of plugins. The file is swaped with versions.bzl on CI during release"""

INTELLIJ_BAZEL_VERSION = "2025.3.4.1"

PLATFORM_VERSION = "253"

# make sure to always choose the lowest among different benchmark build numbers
SINCE_VERSION = "253.29346.138"
UNTIL_VERSION = "253.*"

BENCHMARK_BUILD_NUMBER = "253.22441.33"
PY_BENCHMARK_BUILD_NUMBER = "253.20558.58"
GO_BENCHMARK_BUILD_NUMBER = "253.22441.23"
