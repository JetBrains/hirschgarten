## Run Qodana locally

### Pull container for 2024.2
In order to be able to pull container from qodana registry, go to https://jetbrains.team/p/sa/packages/container/containers/qodana-jvm?v=sha256%3A317b7742b0b738dfc1a996900035c72f1a5b0757ffa2ad5287d5390598f0a87f , click "Connect" in top right corner and follow the instructions

`docker pull registry.jetbrains.team/p/sa/containers/qodana-jvm:2024.2-nightly`

### Create jetbrains/qodana_bazel container based on tools/qodana/Dockerfile

`docker build -t "jetbrains/qodana_bazel" tools/qodana`

### (optional) Create docker volume for persisting bazel cache
Creating shared volume for bazel cache will greatly improve build speed

`docker volume create bazel_cache`

In order to use it, add `-v bazel_cache:/root/.cache/bazel \` to run.qodana.sh script

### Provide $QODANA_TOKEN value
Obtain qodana token from https://qodana.cloud/teams/3rwQe and add it to env variables

`export QODANA_TOKEN=<token value>`

### Run qodana
Modify --cpus, --memory and --memory-swap params according to your capacities and run command:

`tools/qodana/run_qodana.sh`

In order to display report instead of saving it, add `-p 8080:8080` parameter to `docker run` invocation and change `--save-report` parameter to `--show-report` 
