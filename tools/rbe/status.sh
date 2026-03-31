#!/bin/bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

cleanup_stale_pid_files
print_status_summary
