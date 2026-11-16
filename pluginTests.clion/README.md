### Prerequisites to run CLion tests locally:

Because running CLion tests require .NET framework and Rider backed, they are located in a separate module.

1) Install .NET framework:

https://dotnet.microsoft.com/en-us/download/dotnet/10.0

2) Compile Rider backend:
``` shell
cd dotnet && Rider/compileBackend_Radler.cmd
```

3) Set `RESHARPER_HOST_BIN` to the Rider backend build. Run this command from the repository root:
``` shell
export RESHARPER_HOST_BIN="$(pwd)/dotnet/Bin.RiderBackend"
```

The test target inherits this variable from the shell, so no change of the `BUILD.bazel` file is necessary.

### Running CLion tests from the command line

```bash
bazel test //plugins/bazel/pluginTests.clion:pluginTests.clion_test
```

Instead of step 3, you can also pass the variable to one command:
```bash
bazel test //plugins/bazel/pluginTests.clion:pluginTests.clion_test --test_env=RESHARPER_HOST_BIN="$(pwd)/dotnet/Bin.RiderBackend"
```

Each test file has its own test target. The name of the target is the name of the file without `.kt`.
The `pluginTests.clion_test` target is a `test_suite` of all these targets. To run the tests of one file:
```bash
bazel test //plugins/bazel/pluginTests.clion:CcCoptsTest
```

To debug the tests of one file use `run` and pass `-- --debug`. A `test_suite` does not support `run`:
```bash
bazel run //plugins/bazel/pluginTests.clion:CcCoptsTest -- --debug
```

### Layout

The CLion tests run only with Bazel. The JPS module `intellij.bazel.pluginTests.clion` lets you edit them in a JPS project.
The generator does not write the targets of this module. `BUILD.bazel` skips its `build` and `test` sections.
When you change a dependency, change it in `intellij.bazel.pluginTests.clion.iml` and in `test_defs.bzl`.

- `tests/` holds one Kotlin file for each test target. Put a new test file there, and add a `cc_test` call for it to `BUILD.bazel`.
- `framework/` holds the code that all the tests share. It compiles into the `pluginTests.clion_test_lib` library.
- `test_defs.bzl` holds the `cc_test` macro and the dependency lists of all the tests.

The directories have no package directories. Each Kotlin file keeps its `package` declaration.

To filter tests add:
```bash
--test_env=JB_IDE_SM_RUN=true --test_env=JB_TEST_FILTER="fully qualified class name[:test name]"
```

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