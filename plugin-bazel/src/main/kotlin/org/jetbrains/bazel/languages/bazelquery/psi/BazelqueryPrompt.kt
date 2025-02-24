package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets


class BazelqueryPrompt(node: ASTNode) : BazelqueryBaseElement(node) {
    override fun acceptVisitor(visitor: BazelqueryElementVisitor) = visitor.visitPrompt(this)
//    val queryVal: BazelqueryQueryVal? = this.findChildByType(BazelqueryElementTypes.QUERY_VAL)
    //val command: String? = this.findChildByType<PsiElement>(BazelqueryElementTypes.COMMAND)?.text ?: ""

  val queryVals: Array<BazelqueryQueryVal>
    get() = this.findChildrenByClass(BazelqueryQueryVal::class.java)


  val word: String = this.findChildByType<PsiElement>(BazelqueryTokenSets.WORDS)?.text ?: ""


}
