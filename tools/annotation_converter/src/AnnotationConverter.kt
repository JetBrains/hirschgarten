import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MemberValuePair
import com.github.javaparser.ast.expr.NormalAnnotationExpr
import java.io.File
import kotlin.system.exitProcess

object AnnotationConverter {
  private val allFunctions: MutableList<FunctionInfo> = mutableListOf()

  data class ParameterInfo(
    val name: String,
    val docString: String,
    val required: Boolean,
    val default: String,
    val positional: Boolean,
    val named: Boolean,
  )

  data class FunctionInfo(
    val name: String,
    val docString: String,
    val params: List<ParameterInfo>,
  )

  private fun extractParam(paramExpr: NormalAnnotationExpr): ParameterInfo {
    var name: String? = null
    var docString: String? = null
    var positional: Boolean? = null
    var default: String? = null
    var named: Boolean? = null
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
    return ParameterInfo(
      name!!,
      docString!!,
      default == null,
      default ?: "",
      positional!!
    , named!!)
  }

  private fun processParams(paramsExpr: Expression): List<ParameterInfo> {
    val paramsList = paramsExpr.toArrayInitializerExpr().get().values
    val params = mutableListOf<ParameterInfo>()
    for (paramExpr in paramsList) {
      params.add(extractParam(paramExpr as NormalAnnotationExpr))
    }
    return params
  }

  private fun convertDocString(doc: Expression): String {
    return if (doc is BinaryExpr) {
      var left = doc.left
      var res = ""
      while (left is BinaryExpr) {
        res = left.right.toStringLiteralExpr().get().value + res
        left = left.left
      }
      return left.toStringLiteralExpr().get().value + res
    } else {
      doc.toStringLiteralExpr().get().value
    }
  }
  
  private fun processAnnotation(annotationExpr: AnnotationExpr) {
    var functionName = ""
    var docString = ""
    val params = mutableListOf<ParameterInfo>()
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

    val functionInfo = FunctionInfo(functionName, docString, params)
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
//    if (args.isEmpty()) {
//      println("No input files")
//      exitProcess(1)
//    }
    val inputFilePath = "/Users/solar/dev/annotation_converter/inputs/ModuleFileGlobals.java"
    try {
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
    } catch (e: Exception) {
      println("Error parsing file $inputFilePath: ${e.message}")
    }
    println(allFunctions)
  }
}
