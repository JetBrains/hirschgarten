# New Bazel plugin + Android

1. Install the new Bazel plugin by following instructions for the [landing page](https://lp.jetbrains.com/new-bazel-plugin/).

2. Also install the [Android plugin](https://plugins.jetbrains.com/plugin/22989-android), [Android Design Tools plugin](https://plugins.jetbrains.com/plugin/22990-android-design-tools), [Jetpack Compose plugin](https://plugins.jetbrains.com/plugin/18409-jetpack-compose).

3. Install the Android SDK and set the [ANDROID_HOME](https://developer.android.com/tools/variables) environment variable.

4. Open any random project inside IntelliJ IDEA (e.g. a “Hello world” app, doesn’t matter).

5. Double-press Shift to bring up the search menu, enter “Registry” and click on it. Then find the `bsp.android.support` registry flag and enable it:

   ![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXdQrxXjR1VPAP0keLZbShYad5ovELhG87DI0C0JKA3nNBFNc42nMtcUwAmaCFWUexagOD3JwBZ3Ngz4CezVz6EqJrxaXr9M8ECxzGxO0_TfCQB5JFrjp_jj73gssOO1cFOZ3HUi_fPhJ6qhe_BKmNsEw3gH?key=XPzNUqa6vGttBIp8MvrAKg)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXeRNitrNatdvOn0PM9BiCTgYrOjqaSpZjhvnHfAWGntlaXip3gylwbYeIb9VKOPEUaZdbGSWFIFR5VjAeltOoMOj0j6tMuYeLk8ILDxQOwERWzJ5FD8JABjGsjI7SJxYGVa18wjjQFkNpkwXznzXgKHLtc8?key=XPzNUqa6vGttBIp8MvrAKg)

6. Create the following `projectview.bazelproject` file in the root of your Bazel Android project (for a Java-only project, omit `rules_kotlin`):
   ```
   targets:
     //...

   enabled_rules:
     rules_android
     rules_kotlin
   ```

7. Bazel 5 and 6 are not supported for now. Make sure they are not overridden in `.bazelversion`.

8. Then open the project in IntelliJ IDEA and wait for it to import!

9. Press “Build & Resync” afterwards to make sure all the dependent libraries are unzipped by Bazel.

```
```
