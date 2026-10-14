# Convert BUILD files to isolated mode

Why: We keep two sets of BUILD files — `~BUILD` for the open‑source layout and `BUILD.bazel` for the internal monorepo. To work with the isolated (open‑source‑style) codebase (e.g., to test against SDK/compat), run the script below to apply the open-source mirror layout: in each directory with `~BUILD`, `BUILD` becomes the `~BUILD` content and `BUILD.bazel` is removed; directories without `~BUILD` keep `BUILD` and `BUILD.bazel`; every other `~X` file becomes `X`.

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

4) Overwrite existing target files with different content if needed

   python3 plugins/bazel/tools/infra_scripts/fix_builds.py "$TARGET" -f

Notes:
- You can run the script from anywhere; just pass the full path in the folder argument.
- The script prints a summary of deleted and replaced files.
- If a target file exists with different content, the script exits with an error and changes nothing unless you pass `-f`.
- To revert, use your VCS (e.g., `git restore -SW .` or `git checkout -- .`).