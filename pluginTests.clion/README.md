### Prerequisites to run CLion tests locally:

Because running CLion tests require .NET framework and Rider backed, they are located in a separate module.

1) Install .NET framework:

https://dotnet.microsoft.com/en-us/download/dotnet/10.0

2) Compile Rider backend:
``` shell
./dotnet/Rider/compileBackend_Radler.cmd
```

### Running CLion tests from the command line

The `pluginTests.clion_test` target is a `test_suite` of all tests run in CI. To run the entire test suite:
```bash
bazel test //plugins/bazel/pluginTests.clion:pluginTests.clion_test --test_env=RESHARPER_HOST_BIN="$(pwd)/dotnet/Bin.RiderBackend"
```

Each test file has its own test target. The name of the target is the name of the file without `.kt`. To run the tests of one file:
```bash
bazel test //plugins/bazel/pluginTests.clion:CcCoptsTest --test_env=RESHARPER_HOST_BIN="$(pwd)/dotnet/Bin.RiderBackend"
```

To debug the tests of one file use `run` and pass `-- --debug`. A `test_suite` does not support `run`:
```bash
RESHARPER_HOST_BIN="$(pwd)/dotnet/Bin.RiderBackend" bazel run //plugins/bazel/pluginTests.clion:CcCoptsTest -- --debug
```

To filter tests add:
```bash
--test_env=JB_TEST_FILTER="fully qualified class name[:test name]"
```

### Layout

The CLion tests run only with Bazel. The JPS module `intellij.bazel.pluginTests.clion` lets you edit them in a JPS project.
The generator does not write the targets of this module. `BUILD.bazel` skips its `build` and `test` sections.
When you change a dependency, change it in `intellij.bazel.pluginTests.clion.iml` and in `test_defs.bzl`.

- `tests/` holds one Kotlin file for each test target. Put a new test file there, and add a `cc_test` call for it to `BUILD.bazel`.
- `framework/` holds the code that all the tests share. It compiles into the `pluginTests.clion_test_lib` library.
- `test_defs.bzl` holds the `cc_test` macro and the dependency lists of all the tests.
- `extension.bzl` holds the `registry` module extension. It downloads the archives of real projects.
- `registry.MODULE.bazel` declares these projects. The root `MODULE.bazel` includes it.

To add a test for a real project, add a `github` or `archive` tag and a `bazel mod tidy` to update the `use_repo`.

On Windows msys2 (BAZEL_SH, needs to be set explicitly because some scripts in our repo overwrite it otherwise) and clang-cl (BAZEL_LLVM) are required and the following additional environment variables need to be set (copy this to your project view file):
```
  --test_env=BAZEL_SH=C:\msys64\usr\bin\bash.exe
  --test_env=BAZEL_LLVM 
  --test_env=USERPROFILE
  --test_env=LOCALAPPDATA
  --test_env=APPDATA
  --test_env=ProgramFiles
  --test_env=ProgramFiles(x86)
  --test_env=ProgramData
```