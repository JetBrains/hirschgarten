# Bazel feature matrix

|     | Support level                                |
|-----|----------------------------------------------|
| ✅   | fully supported                              |
| 🚩  | available behind feature flag / with caveats |
| 🛠️ | work in progress                             |
| ❌   | currently not supported                      |


| **language**                   | **import** | **editor** | **build** | **diagnostics** | **run** | **debug** | **test** |
| ------------------------------ | :--------- | ---------- | --------- | --------------- | ------- | --------- | -------- |
| **JVM**                        |            |            |           |                 |         |           |          |
| Java                           | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅        |
| Kotlin                         | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅        |
| Scala                          | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅        |
| **[Python](guides/python.md)** | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅        |
| **[Go](guides/go.md)**         | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅        |
| **Starlark**                   |            | ✅          |           | ✅               |         | ✅         |          |

| **testing framework** | **run** | **debug** | **test** | **gutter** | **reports** | **filter** |
| --------------------- | ------- | --------- | -------- | ---------- | ----------- | ---------- |
| JUnit4                | ✅       | ✅         | ✅        | ✅          | ✅           | ✅️        |
| JUnit5                | ✅       | ✅         | ✅        | ✅          | ✅           | ✅️        |


(🚩) To enable support, follow the linked guides. Features behind flags are under development and may not be fully functional. 
(*) Scala support is currently only available with nightly versions of the Scala plugin for IntelliJ IDEA.