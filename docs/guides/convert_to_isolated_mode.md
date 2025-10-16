# Convert BUILD files to isolated mode

Why: We keep two sets of BUILD files — `~BUILD` for the open‑source layout and `BUILD.bazel` for the internal monorepo. To work with the isolated (open‑source‑style) codebase (e.g., to test against SDK/compat), run the script below to remove `BUILD.bazel` files and rename `~BUILD` → `BUILD`.

Requirements: Python 3, clean working directory (recommended).

Usage (macOS/Linux):
- The folder argument must be the full path to the `plugins/bazel` directory in your repo.

1) Set the absolute path

   REPO=/absolute/path/to/your/repo
   TARGET="$REPO/plugins/bazel"

2) Dry run (shows what would change)

   python3 plugins/bazel/tools/infra_scripts/fix_builds.py "$TARGET" -n

3) Do the conversion

   python3 plugins/bazel/tools/infra_scripts/fix_builds.py "$TARGET"

4) Overwrite existing BUILD files if needed

   python3 plugins/bazel/tools/infra_scripts/fix_builds.py "$TARGET" -f

Notes:
- You can run the script from anywhere; just pass the full path in the folder argument.
- The script prints a summary of deleted and renamed files.
- To revert, use your VCS (e.g., `git restore -SW .` or `git checkout -- .`).