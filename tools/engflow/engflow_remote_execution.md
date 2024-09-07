# Remote execution locally

## Run remote execution

Config for engflow is stored in [.engflow.bazelrc](.engflow.bazelrc)  
Generate mTLS certificate and key on [our project's page](https://jasper.cluster.engflow.com/gettingstarted)  
Place them in [tools/mtls](tools/mtls) folder  
Add `--config=engflow` to any Bazel command you run  
```
bazel build --config=engflow //...
```

## Update Docker image used for remote execution
### Requirements
* Unix system
* Docker installed on a system
* Credentials for publishing on [our registry](https://jetbrains.team/p/bazel/packages/container/docker). Visit it and click "Connect" button in the upper right corner to request login and password


### Build and publish Docker image
The image for remote execution is built from [this Dockerfile](docker/Dockerfile)  
To build the image run the script [build-and-push.sh](docker/build-and-push.sh) after making changes to Dockerfile

### Update platforms after changing Docker image
The platforms for remote execution are generated via `rbe_configs_gen` in [bazel-toolchains](https://github.com/bazelbuild/bazel-toolchains?tab=readme-ov-file#rbe_configs_gen---cli-tool-to-generate-configs) by running the following command from root of the project:

```
rbe_configs_gen --output_config_path n --output_config_path tools/engflow/platforms/linux_x86 --exec_os=linux --target_os=linux --output_src_root=$(pwd) --toolchain_container=registry.jetbrains.team/p/bazel/docker/hirschgarten-engflow:latest
```

After running this script make sure to delete `tools/engflow/platforms/linux_x86/cc/WORKSPACE` and `tools/engflow/platforms/linux_x86/cc/REPO.bazel` by running:

```rm tools/engflow/platforms/linux_x86/cc/WORKSPACE tools/engflow/platforms/linux_x86/cc/REPO.bazel```