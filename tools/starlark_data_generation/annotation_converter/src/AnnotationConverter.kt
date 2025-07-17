import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.ArrayInitializerExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MemberValuePair
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NormalAnnotationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.TextBlockLiteralExpr
import common.Environment
import common.GlobalFunction
import common.Param
import common.serializeFunctionsTo
import java.io.File
import kotlin.system.exitProcess

object AnnotationConverter {
  private val allFunctions: MutableList<GlobalFunction> = mutableListOf()

  private fun stringOrStringConst(expr: Expression, stringConsts: Map<String, String>): String {
    return if (expr is NameExpr) {
      stringConsts[expr.nameAsString]!!
    } else {
      convertDocString(expr)!!
    }
  }

  private fun extractParam(paramExpr: NormalAnnotationExpr, stringConsts: Map<String, String>): Param {
    var name: String? = null
    var docString: String? = null
    var positional: Boolean = true
    var default: String? = null
    var named: Boolean = false
    for (it in paramExpr.childNodes) {
      if (it is MemberValuePair) {
        when (it.name.identifier) {
          "name" -> name = stringOrStringConst(it.value, stringConsts)
          "doc" -> docString = stringOrStringConst(it.value, stringConsts)
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

  private fun processParams(paramsExpr: Expression, stringConsts: Map<String, String>): List<Param> {
    val paramsList = paramsExpr.toArrayInitializerExpr().get().values
    val params = mutableListOf<Param>()
    for (paramExpr in paramsList) {
      params.add(extractParam(paramExpr as NormalAnnotationExpr, stringConsts))
    }
    return params
  }

  private fun convertDocString(doc: Expression): String? {
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

      is TextBlockLiteralExpr -> {
        doc.toTextBlockLiteralExpr().get().value.trimIndent().replace("\n", " ")
      }

      else -> null
    }
  }

  private fun processAnnotation(annotationExpr: AnnotationExpr, environment: List<Environment>, stringConsts: Map<String, String>) {
    var functionName = ""
    var docString: String? = null
    val params = mutableListOf<Param>()
    for (it in annotationExpr.childNodes) {
      if (it is MemberValuePair) {
        if (it.name.identifier == "name") {
          functionName = it.value.toString().substring(1, it.value.toString().length - 1)
        } else if (it.name.identifier == "doc") {
          docString = convertDocString(it.value)
        } else if (it.name.identifier == "parameters") {
          params.addAll(processParams(it.value, stringConsts))
        }
      }
    }

    val functionInfo = GlobalFunction(functionName, docString, environment, params)
    allFunctions.add(functionInfo)
  }

  private fun getEnvironments(annotationExpr: AnnotationExpr): List<Environment> {
    val environments = mutableListOf<Environment>()
    for (it in annotationExpr.childNodes) {
      if (it is MemberValuePair) {
        if (it.name.identifier == "environment") {
          if (it.value is ArrayInitializerExpr) {
            val list = it.value.toArrayInitializerExpr().get().values
            for (value in list) {
              val fieldAccess = value.asFieldAccessExpr()
              val name = fieldAccess.name.identifier
              environments.add(Environment.valueOf(name))
            }
          } else {
            val name = it.value.asFieldAccessExpr().name.identifier
            environments.add(Environment.valueOf(name))
          }
        }
      }
    }
    return environments
  }

  private fun String.containsOnlyUppercaseAndUnderscores(): Boolean {
    return all { it.isUpperCase() || it == '_' }
  }

  private fun getStringConsts(cu: CompilationUnit): Map<String, String> {
    val stringConsts = mutableMapOf<String, String>()
    cu.findAll(VariableDeclarator::class.java).forEach {
      val name = it.name.identifier
      if (name.containsOnlyUppercaseAndUnderscores()) {
        val value = convertDocString(it.initializer.get())
        if (value != null) {
          stringConsts[name] = value
        }
      }
    }
    return stringConsts
  }

  private fun processCompilationUnit(cu: CompilationUnit) {
    val globalMethodsExpr = cu.findAll(AnnotationExpr::class.java).filter { it.nameAsString == "GlobalMethods" }
    val environments = getEnvironments(globalMethodsExpr.first()!!)
    val stringConsts = getStringConsts(cu)
    cu.findAll(MethodDeclaration::class.java).forEach {
      it.annotations.forEach {
          annotationExpr -> if (annotationExpr.nameAsString == "StarlarkMethod") {
            processAnnotation(annotationExpr, environments, stringConsts)
        }
      }
    }
  }


  @JvmStatic
  fun main(args: Array<String>) {
    if (args.isEmpty()) {
      println("No input files")
      exitProcess(1)
    }
    for (inputFilePath in args) {
      val file = File(inputFilePath)
      if (!file.exists()) {
        println("File not found: $inputFilePath")
        exitProcess(1)
      }

      val parser = JavaParser()
      val parseResult = parser.parse(file)
      processCompilationUnit(parseResult.result.get())
    }

    println(serializeFunctionsTo(allFunctions))
  }
}
