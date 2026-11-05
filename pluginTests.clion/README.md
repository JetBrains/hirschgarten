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

To debug the tests use `run` and pass `-- --debug`:
```bash
bazel run //plugins/bazel/pluginTests.clion:pluginTests.clion_test -- --debug
```

To filter tests add:
```bash
--test_env=JB_IDE_SM_RUN=true --test_env=JB_TEST_FILTER="fully qualified class name[:test name]"
```
