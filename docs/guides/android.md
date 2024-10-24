# New Bazel plugin + Android

1. Install IntelliJ IDEA 2024.3 EAP.
2. Open IDEA, click on "Plugins", click the settings icon on the top right, click "Manage Plugin Repositories..."
   <img src="../files/REPOSITORIES.png" width="600">
3. Hit the "+" button, add `https://plugins.jetbrains.com/plugins/nightly/20329`.
4. Hit the "+" button, add `https://plugins.jetbrains.com/plugins/nightly/22977`.
   <img src="../files/REPOS.png" width="600">
5. Search "Bazel" in the Marketplace and install the nightly version of the plugin.
   <img src="../files/BAZELPLUGIN.png" width="600">
6. Also install the [Android plugin](https://plugins.jetbrains.com/plugin/22989-android), [Android Design Tools plugin](https://plugins.jetbrains.com/plugin/22990-android-design-tools), [Jetpack Compose plugin](https://plugins.jetbrains.com/plugin/18409-jetpack-compose).

7. Install the Android SDK and set the [ANDROID_HOME](https://developer.android.com/tools/variables) environment variable.

8. Open any random project inside IntelliJ IDEA (e.g. a “Hello world” app, doesn’t matter).

9. Double-press Shift to bring up the search menu, enter “Registry” and click on it. Then find the `bsp.android.support` registry flag and enable it:

   ![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXdQrxXjR1VPAP0keLZbShYad5ovELhG87DI0C0JKA3nNBFNc42nMtcUwAmaCFWUexagOD3JwBZ3Ngz4CezVz6EqJrxaXr9M8ECxzGxO0_TfCQB5JFrjp_jj73gssOO1cFOZ3HUi_fPhJ6qhe_BKmNsEw3gH?key=XPzNUqa6vGttBIp8MvrAKg)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXeRNitrNatdvOn0PM9BiCTgYrOjqaSpZjhvnHfAWGntlaXip3gylwbYeIb9VKOPEUaZdbGSWFIFR5VjAeltOoMOj0j6tMuYeLk8ILDxQOwERWzJ5FD8JABjGsjI7SJxYGVa18wjjQFkNpkwXznzXgKHLtc8?key=XPzNUqa6vGttBIp8MvrAKg)

10. Make sure you have the following flags in your project's `.bazelrc`:
   ```
   common --experimental_google_legacy_api
   common --experimental_enable_android_migration_apis
   ```
   in order to prevent errors like `AndroidManifestInfo is experimental and thus unavailable with the current flags`.
11. Add the following line:
    ```
    enable_native_android_rules: true
    ```
    to your projectview file **if** you are still using the native (built-in) Android rules instead of `rules_android`.
7. Bazel 5 and 6 are not supported for now. Make sure they are not overridden in `.bazelversion`.

8. Then open the project in IntelliJ IDEA and wait for it to import!

9. Press “Build & Resync” afterwards to make sure all the dependent libraries are unzipped by Bazel.
