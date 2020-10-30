package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.junit.JUnitUtil
import com.intellij.execution.testframework.AbstractJavaTestConfigurationProducer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

fun PsiElement.currentPackage() =
    AbstractJavaTestConfigurationProducer.checkPackage(this)

fun PsiElement.currentTestMethod(): PsiMethod? {
    val javaMethod = JUnitUtil.getTestMethod(this)
    if (javaMethod != null) return javaMethod

    val ktFunction = this.getParentOfType<KtNamedFunction>(false) ?: return null
    val owner = PsiTreeUtil.getParentOfType(ktFunction, KtFunction::class.java, KtClass::class.java)

    if (owner is KtClass) {
        val delegate = owner.toLightClass() ?: return null
        val ktMethod = delegate.methods.firstOrNull { it.navigationElement == ktFunction } ?: return null
        if (JUnitUtil.getTestMethod(ktMethod) != null) return ktMethod
    }
    return null
}

fun PsiElement.currentTestClass(): PsiClass? {
    val javaClass = JUnitUtil.getTestClass(this)
    if (javaClass != null) return javaClass

    val containingFile = containingFile as? KtFile ?: return null
    var ktClass = getParentOfType<KtClass>(false)
    if (!ktClass.isJUnitTestClass()) {
        ktClass = getTestClassInFile(containingFile)
    }
    return ktClass?.toLightClass()
}

fun PsiClass.pathPrefix(): String = StringUtil.getPackageName(qualifiedName ?: "").replace(".", "/") + "/" + name

private fun KtClass?.isJUnitTestClass() =
    this?.toLightClass()?.let { JUnitUtil.isTestClass(it, false, true) } ?: false

private fun getTestClassInFile(ktFile: KtFile) =
    ktFile.declarations.filterIsInstance<KtClass>().singleOrNull { it.isJUnitTestClass() }
