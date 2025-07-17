import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MemberValuePair
import com.github.javaparser.ast.expr.NormalAnnotationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import common.GlobalFunction
import common.Param
import common.serializeFunctionsTo
import java.io.File
import kotlin.system.exitProcess

object AnnotationConverter {
  private val allFunctions: MutableList<GlobalFunction> = mutableListOf()

  private fun extractParam(paramExpr: NormalAnnotationExpr): Param {
    var name: String? = null
    var docString: String? = null
    var positional: Boolean = true
    var default: String? = null
    var named: Boolean = false
    for (it in paramExpr.childNodes) {
      if (it is MemberValuePair) {
        when (it.name.identifier) {
          "name" -> name = it.value.toString().substring(1, it.value.toString().length - 1)
          "doc" -> docString = convertDocString(it.value)
          "named" -> named = it.value.toString() == "true"
          "positional" -> positional = it.value.toString() == "true"
          "default" -> default = it.value.toString().substring(1, it.value.toString().length - 1)
        }
      }
    }
    return Param(
      name = name!!,
      doc = docString,
      positional = positional,
      named = named,
      defaultValue = default ?: "",
      required = default == null,
    )
  }

  private fun processParams(paramsExpr: Expression): List<Param> {
    val paramsList = paramsExpr.toArrayInitializerExpr().get().values
    val params = mutableListOf<Param>()
    for (paramExpr in paramsList) {
      params.add(extractParam(paramExpr as NormalAnnotationExpr))
    }
    return params
  }

  private fun convertDocString(doc: Expression): String {
    return when (doc) {
      is BinaryExpr -> {
        var left = doc.left
        var res = ""
        while (left is BinaryExpr) {
          res = left.right.toStringLiteralExpr().get().value + res
          left = left.left
        }
        return left.toStringLiteralExpr().get().value + res
      }

      is StringLiteralExpr -> {
        doc.toStringLiteralExpr().get().value
      }

      else -> {
        doc.toTextBlockLiteralExpr().get().value.trimIndent().replace("\n", " ")
      }
    }
  }

  private fun processAnnotation(annotationExpr: AnnotationExpr) {
    var functionName = ""
    var docString = ""
    val params = mutableListOf<Param>()
    for (it in annotationExpr.childNodes) {
      if (it is MemberValuePair) {
        if (it.name.identifier == "name") {
          functionName = it.value.toString().substring(1, it.value.toString().length - 1)
        } else if (it.name.identifier == "doc") {
          docString = convertDocString(it.value)
        } else if (it.name.identifier == "parameters") {
          params.addAll(processParams(it.value))
        }
      }
    }

    val functionInfo = GlobalFunction(functionName, docString, params)
    allFunctions.add(functionInfo)
  }

  private fun processCompilationUnit(cu: CompilationUnit) {
    cu.findAll(MethodDeclaration::class.java).forEach {
      it.annotations.forEach {
          annotationExpr -> if (annotationExpr.nameAsString == "StarlarkMethod") processAnnotation(annotationExpr)
      }
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    //if (args.isEmpty()) {
    //  println("No input files")
    //  exitProcess(1)
    //}
    //val inputFilePath = args.first()
    val inputFilePath = "/Users/solar/dev/ultimate/plugins/bazel/tools/starlark_data_generation/annotation_converter/inputs/ModuleFileGlobals.java"
    val file = File(inputFilePath)
    if (!file.exists()) {
      println("File not found: $inputFilePath")
      exitProcess(1)
    }

    val parser = JavaParser()
    val parseResult = parser.parse(file)
    if (parseResult.result.isPresent) {
      processCompilationUnit(parseResult.result.get())
    } else {
      println("Error parsing file $inputFilePath")
    }
    println(serializeFunctionsTo(allFunctions))
  }
}
