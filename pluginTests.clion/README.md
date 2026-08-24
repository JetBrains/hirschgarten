### Prerequisites to run CLion tests locally:

Because running CLion tests require .NET framework and Rider backed, they are located in a separate module.

1) Install .NET framework:

https://dotnet.microsoft.com/en-us/download/dotnet/10.0

2) Compile Rider backend:
``` shell
cd dotnet && Rider/compileBackend_Radler.cmd
```

3) [TEMPORARY] because of https://youtrack.jetbrains.com/issue/MRI-3220

Modify the `BUILD.bazel` file 
``` bazel
jps_test(
    name = "pluginTests.clion_test",
    runtime_deps = [":pluginTests.clion_test_lib"],
    env = {
        "RESHARPER_HOST_BIN": "/Users/...../ultimate/dotnet/Bin.RiderBackend",
    },
)

```

Please note that this change will be overwritten on each jps-to-bazel call 

### Running CLion tests from the command line

Instead of following step 3, it is also possible to run the CLion test from the command line after compiling the Rider Backend without modifying the `BUILD` file:
```bash
bazel test //plugins/bazel/pluginTests.clion:pluginTests.clion_test --test_env=RESHARPER_HOST_BIN="$(pwd)/dotnet/Bin.RiderBackend"
```

To debug the tests use `run` and pass `-- --debug`:
```bash
bazel run //plugins/bazel/pluginTests.clion:pluginTests.clion_test --test_env=RESHARPER_HOST_BIN="$(pwd)/dotnet/Bin.RiderBackend" -- --debug
```

To filter tests add:
```bash
--test_env=JB_IDE_SM_RUN=true --test_env=JB_TEST_FILTER="fully qualified class name[:test name]"
```
