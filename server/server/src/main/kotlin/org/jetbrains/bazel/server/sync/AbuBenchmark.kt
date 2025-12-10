package org.jetbrains.bazel.server.sync

data class AbuBenchmark(
  val name: String,
  val seconds: Double,
  val result: Any?,
) {
  companion object {
    fun toCsv(benchmarks: List<AbuBenchmark>): String =
      benchmarks.joinToString(prefix = "Scenario, Seconds\n", separator = "\n") {
        "${it.name}, ${it.seconds}"
      }

    val dataloreFileSets: Map<String, List<String>> =
      mapOf(
        "dataloreSingleFile" to dataloreSingleFile,
        "dataloreSmallFolder" to dataloreSmallFolder,
        "dataloreNear10" to dataloreNear10,
        "dataloreRandom20" to dataloreRandom20,
      ).mapValues { entry -> entry.value.map { "$DATALORE_PATH$it" } }

    val ultimateFileSets: Map<String, List<String>> =
      mapOf(
        "ultimateSingleFile" to ultimateSingleFile,
        "ultimateSmallFolder" to ultimateSmallFolder,
        "ultimateNear20" to ultimateNear20,
        "ultimateRandom50" to ultimateRandom50,
      ).mapValues { entry -> entry.value.map { "$ULTIMATE_PATH$it" } }

  }
}

private const val DATALORE_PATH = "/Users/mkocot/Documents/work/_READONLY/Datalore/"
private const val ULTIMATE_PATH = "/Users/mkocot/Documents/work/_READONLY/ultimate_test/"

private val dataloreSingleFile = listOf( // one file
  "core/server/src/main/kotlin/jetbrains/datalore/core/server/sql/DataloreSchema.kt",
)

private val ultimateSingleFile = listOf( // one file
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/check/ApiChangeSearcher.kt",
)

private val dataloreSmallFolder = listOf( // same folder
  "core/server/src/test/kotlin/jetbrains/datalore/core/server/jooq/dao/JooqDataloreSettingsDaoTest.kt",
  "core/server/src/test/kotlin/jetbrains/datalore/core/server/jooq/dao/JooqPersistentTaskDaoTest.kt",
  "core/server/src/test/kotlin/jetbrains/datalore/core/server/jooq/dao/JooqSuspiciousCodePatternDaoTest.kt",
)

private val ultimateSmallFolder = listOf( // same folder
  "plugins/asp/src/com/jetbrains/asp/AspFileSyntaxHighlighter.java",
  "plugins/asp/src/com/jetbrains/asp/AspParserDefinition.java",
  "plugins/asp/src/com/jetbrains/asp/AspCommenter.java",
)

private val dataloreNear10 = listOf( // different folders, one BUILD file
  "core/common/src/main/kotlin/jetbrains/datalore/core/common/CodePatternLocation.java",
  "core/server/src/main/kotlin/jetbrains/datalore/core/server/dao/DataloreSettingsDao.kt",
  "core/server/src/main/kotlin/jetbrains/datalore/core/server/jooq/dao/JooqSuspiciousCodePatternDao.kt",
  "core/server/src/main/kotlin/jetbrains/datalore/core/server/security/JwtServiceSpringConfiguration.kt",
  "core/server/src/main/kotlin/jetbrains/datalore/core/server/sql/DataloreSchema.kt",
  "core/server/src/test/kotlin/jetbrains/datalore/core/server/jooq/dao/JooqDataloreSettingsDaoTest.kt",
  "core/server/src/test/kotlin/jetbrains/datalore/core/server/sql/BootstrapSQLTest.kt",
  "core/server/src/testFixtures/kotlin/jetbrains/datalore/core/server/jooq/CoreGenerateJooq.kt",
  "core/server/src/testFixtures/kotlin/jetbrains/datalore/core/server/sql/spring/TestSQLDatasourceApplicationContextInitializer.kt",
  "core/server/src/testFixtures/kotlin/jetbrains/datalore/core/server/sql/DumpMigrations.kt",
)

private val ultimateNear20 = listOf( // different folders, one BUILD file
  "plugins/api-watcher/testData/searches/kotlinFile.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/usages/searches/modules/PluginModuleSearch.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/usages/problems/AuthException.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/usages/problems/UnsuccessfulResponseException.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/apis/ApiAccessFlagsOwner.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/apis/ApiPluginId.kt",
  "plugins/api-watcher/test/com/jetbrains/apiwatcher/tests/mocks/MockRequester.kt",
  "plugins/api-watcher/testData/conflicts/method/parameterTypesChanged/after.java",
  "plugins/api-watcher/test/com/jetbrains/apiwatcher/tests/misc/KotlinK1RevisionUtilTest.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/usages/requester/Requester.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/conflicts/potential/classes/ClassAccessPrivilegeWeakened.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/conflicts/potential/classes/MethodBecameAbstract.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/check/ApiChangeSearcher.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/presentation/panels/statistics/UsageStatisticsPanel.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/apis/ApiFile.kt",
  "plugins/api-watcher/testData/conflicts/method/interface/newAbstractMethodImplementedInSubSubclass/after.java",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/presentation/bytecode/highlight/HighlightTextifier.kt",
  "plugins/api-watcher/testData/descriptors/allKotlinStructures.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/conflicts/potential/PotentialConflict.kt",
  "plugins/api-watcher/testData/conflicts/field/becameFinal/before.java",
)

private val dataloreRandom20 = listOf( // files scattered everywhere
  "notebook/client/src/main/java/jetbrains/datalore/notebook/client/editor/cellCodeEditor/actions/DeleteCellAction.kt",
  "ocelot/src/testFixtures/kotlin/jetbrains/datalore/ocelot/ot/persistence/protocol/client/EntityRootIdIndex.java",
  "notebook/client/src/main/java/jetbrains/datalore/notebook/client/pages/report/notebook/cellTree/CellTree.kt",
  "ocelot/src/main/kotlin/jetbrains/ocelot/ot/model/protocol/client/root/ClientRootEvent.java",
  "computation/runs/common/src/main/kotlin/jetbrains/datalore/computation/runs/common/RunParameterDefinition.kt",
  "ocelot/src/test/kotlin/jetbrains/ocelot/ot/model/ot/undo/TestViewStateProvider.java",
  "transport/server/src/main/kotlin/jetbrains/datalore/transport/server/AccessCheckingByKeyChannel.kt",
  "notebook/client/src/main/java/jetbrains/datalore/notebook/client/pages/teamAdmin/services/TeamAdminServiceModule.kt",
  "notebook/client/src/main/java/jetbrains/datalore/notebook/client/components/common/EmailFormat.kt",
  "e2e-tests/playwright-tests/src/main/java/jetbrains/datalore/playwright/core/pages/datalore/reports/NotFoundPage.java",
  "vfs/server/api/src/main/kotlin/jetbrains/datalore/vfs/server/api/sql/jooq/generated/tables/records/FutureSharingRecord.kt",
  "user-management/server/api/src/main/kotlin/jetbrains/datalore/userManagement/server/api/jooq/generated/tables/records/DisposableDomainsRecord.kt",
  "vfs/resources/databases/server/impl/src/main/kotlin/jetbrains/datalore/vfs/resources/databases/server/impl/jooq/JooqDatabasesDao.kt",
  "notebook/server/src/main/java/jetbrains/datalore/notebook/server/devServlet/servlets/BazelServlet.java",
  "core/server/src/main/kotlin/jetbrains/datalore/core/server/security/JwtServiceSpringConfiguration.kt",
  "base/common/src/main/kotlin/jetbrains/datalore/base/common/edt/EdtManagerFactory.java",
  "notebook/client/src/main/java/jetbrains/datalore/notebook/client/api/notebook/worksheet/GroupedMarkdownCells.kt",
  "notebook/client/src/main/java/jetbrains/datalore/notebook/client/editor/cellCodeEditor/viewState/EditorViewStateProviderModule.java",
  "ot2/core/src/main/kotlin/jetbrains/datalore/ot2/core/transport/OtChannel.kt",
  "billing/server/impl/src/test/kotlin/jetbrains/datalore/billing/server/impl/plans/team/AdminTeamsControllerTest.kt",
)

private val ultimateRandom50 = listOf( // files scattered everywhere
  "community/platform/platform-impl/src/com/intellij/ide/ManageRecentProjectsAction.java",
  "community/java/java-tests/testData/codeInsight/gotoDeclaration/RecordPatternInForEach.java",
  "community/plugins/kotlin/idea/tests/testData/codeInsight/moveUpDown/classBodyDeclarations/function/singleLambdaExpressionFunction2.kt",
  "plugins/pwa/java-pwa/testSrc/com/intellij/analysis/pwa/JavaUsedDependenciesTest.kt",
  "community/plugins/testng/src/com/theoryinpractice/testng/model/TestClassFilter.java",
  "community/plugins/kotlin/idea/tests/testData/inspections/unusedSymbol/object/companionObjectBaseMemberUsed.kt",
  "phpstorm/dql/gen/com/jetbrains/php/dql/psi/DqlNamespaceReferenceExpression.java",
  "community/plugins/kotlin/idea/tests/testData/refactoring/introduceLambdaParameter/lambdaParamOfUnit.kt",
  "community/plugins/kotlin/completion/testData/handlers/basic/staticMemberOfNotImported/AmbigiousName.kt",
  "community/tools/intellij.lambda.testFramework/src/com/intellij/lambda/testFramework/testApi/editor/HighlightersTestApi.kt",
  "language-server/lsp.test/test/com/jetbrains/ls/lsp/test/cases/requests/documentSymbols/LspKotlinDocumentSymbolsTest.kt",
  "community/plugins/xslt-debugger/src/org/intellij/plugins/xsltDebugger/ui/StructureTree.java",
  "community/plugins/kotlin/idea/tests/testData/refactoring/moveTopLevel/kotlin/callsAndCallableRefs/internalUsages/differentSourceWithImports/after/foo/Y.kt",
  "community/plugins/kotlin/idea/tests/testData/findUsages/kotlin/conventions/components/callableReferences.0.kt",
  "plugins/graph/genApi/com/intellij/openapi/graph/layout/Direction.java",
  "plugins/spring/spring-framework/spring-core/src/com/intellij/spring/model/cacheable/jam/SpringJamCacheableElement.java",
  "plugins/JavaScriptDebugger/wip/protocol-model-generator/src/DomainGenerator.kt",
  "plugins/dependency-analysis/php/src/com/intellij/dependencyAnalysis/php/PhpDependencyAnalyzer.kt",
  "plugins/JavaScriptLanguage/testSrc/com/intellij/lang/javascript/FlowJSHighlightingAndCompletionTest.java",
  "plugins/llm/activation/activation-platform/src-ij/com/intellij/ml/llm/activation/platform/ijPlatform.kt",
  "plugins/jvm-dfa-analysis/protobuf/gen/com/intellij/jvm/dfa/analysis/protobuf/ir/PtIrValueConstantOrBuilder.java",
  "plugins/jvm-dfa-analysis/java/testData/regression/BenchmarkTest02383.java",
  "plugins/llm/ds/next/src/com/intellij/ml/llm/ds/next/agents/impl/executors/dataWrangler/DataWranglerAgentHelper.kt",
  "plugins/bazel/plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/starlark/psi/expressions/StarlarkTupleExpression.kt",
  "plugins/frameworks/ktor/ktor-starter/src/com/intellij/ktor/initializr/json/ProjectSettings.kt",
  "plugins/http-client/restClient/src/com/intellij/httpClient/http/request/psi/codeStyle/HttpRequestBodyBlock.java",
  "plugins/JavaScriptLanguage/javascript-psi-impl/src/com/intellij/lang/javascript/psi/stubs/impl/JSDestructuringPropertyStub.java",
  "plugins/full-line/java/local/test/com/intellij/fullLine/java/supporters/JavaFullLineSessionTest.kt",
  "plugins/api-watcher/src/com/intellij/apiwatcher/plugin/presentation/nodes/usages/ApiElementNode.kt",
  "plugins/spring/spring/src/com/intellij/spring/model/xml/beans/LifecycleBean.java",
  "rider/src/com/jetbrains/rider/services/security/TrustedSolutionManager.kt",
  "dbe/dialects/mysqlbase/gen/com/intellij/database/dialects/mysqlbase/model/MysqlBaseTable.java",
  "CIDR/clion-makefile/src/com/jetbrains/cidr/cpp/makefile/project/resolver/preconfigure/detectors/MkPerlMakeMakerDetector.kt",
  "rider/src/com/jetbrains/rider/debugger/attach/RiderAttachUtil.kt",
  "fleet/dock/impl/srcCommonMain/fleet/dock/impl/DockIdentityImpl.kt",
  "contrib/qodana/core/tests/org/jetbrains/qodana/staticAnalysis/inspections/runner/QodanaQuickFixesApplyTest.kt",
  "dbe/dialects/oracle/sql/psi/OraPsiUtils.java",
  "fleet/frontend/srcCommonMain/fleet/frontend/smartMode/SmartModeActions.kt",
  "goland/intellij-go/impl/test/com/goide/editor/GoFillParagraphLineCommentsTest.java",
  "goland/intellij-go/impl/src/com/goide/dlv/renderer/GoFmtSprintfEvaluator.kt",
  "dbe/sql/tests/psi/impl/OraPlusParserTest.java",
  "rider/model/generated/src/com/jetbrains/rider/model/StackTraceFilterProvider.Pregenerated.kt",
  "dbe/sql/core-impl/src/dataFlow/instructions/SqlSpecialBinaryOpInstruction.kt",
  "platform/buildScripts/src/productLayout/UltimateModuleSets.kt",
  "dbe/sql/core-impl/src/psi/stubs/factories/SqlTriggerDefinitionStubElementFactory.java",
  "dbe/dialects/mongo/ex/sql/js/MongoJSFileTypeOverrider.kt",
  "remote-dev/rd-ui/src/com/jetbrains/rd/ui/bindable/views/listControl/renderers/RdButtonRenderer.kt",
  "CIDR/cidr-clangd/tests-classic/testSrc/com/jetbrains/cidr/cpp/clang/ClangTidyAnnotatorIntegrationTestCase.java",
  "rider/src/com/jetbrains/rider/projectView/views/assemblyExplorer/AssemblyExplorerRootNode.kt",
  "licenseCommon/source/com/intellij/ide/license/impl/AuthManagerHolder.java",
)
