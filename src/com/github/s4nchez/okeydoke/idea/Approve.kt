package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationContext.getFromContext
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.junit.JUnitUtil
import com.intellij.execution.junit.JavaRuntimeConfigurationProducerBase
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.StatusBarUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class Approve : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val context = event.configContext
        val pendingTests = context.psiElement().findTestsPendingApproval()
        pendingTests.forEach { file -> file.actual?.approve() }
        updateStatusBar(context, pendingTests)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.apply {
            val context = event.configContext
            text = "Approve Tests"
            isVisible = context.isJUnit()
            isEnabled = context.psiElement().findTestsPendingApproval().isNotEmpty()
        }
    }

    private fun PsiElement?.findTestsPendingApproval() = findApprovalTests().filter { it.actual != null }

    private fun PsiElement?.findApprovalTests(): Collection<ApprovalTest> {
        if (this == null) return emptyList()

        val selectedMethod = getTestMethod()
        if (selectedMethod != null) {
            return project.findApprovalTests { file -> file.path.contains(selectedMethod.containingClass?.pathPrefix() + "." + selectedMethod.name) }
        }

        val selectedClass = getTestClass()
        if (selectedClass != null) {
            return project.findApprovalTests { file -> file.path.contains(selectedClass.pathPrefix()) }
        }

        val selectedPackage = JavaRuntimeConfigurationProducerBase.checkPackage(this)
        if (selectedPackage != null) {
            return project.findApprovalTests { file -> file.path.contains(selectedPackage.qualifiedName.replace(".", "/")) }
        }

        return emptyList()
    }

    private fun PsiElement.getTestMethod(): PsiMethod? {
        val javaMethod = JUnitUtil.getTestMethod(this)
        if (javaMethod != null) return javaMethod

        val ktFunction = this.getParentOfType<KtNamedFunction>(false) ?: return null
        val owner = getParentOfType(ktFunction, KtFunction::class.java, KtClass::class.java)

        if (owner is KtClass) {
            val delegate = owner.toLightClass() ?: return null
            val ktMethod = delegate.methods.firstOrNull { it.navigationElement == ktFunction } ?: return null
            if (JUnitUtil.getTestMethod(ktMethod) != null) return ktMethod
        }
        return null
    }

    private fun PsiElement.getTestClass(): PsiClass? {
        val javaClass = JUnitUtil.getTestClass(this)
        if (javaClass != null) return javaClass

        val containingFile = containingFile as? KtFile ?: return null
        var ktClass = getParentOfType<KtClass>(false)
        if (!ktClass.isJUnitTestClass()) {
            ktClass = getTestClassInFile(containingFile)
        }
        return ktClass?.toLightClass()
    }

    private fun ConfigurationContext.psiElement() = location?.psiElement

    private fun VirtualFile.approve() {
        runWriteAction {
            val approvalTestFileName = name.replace(Regex("actual$"), "approved")
            parent.findChild(approvalTestFileName)?.delete(this@Approve)
            rename(this@Approve, approvalTestFileName)
        }
    }

    private fun updateStatusBar(context: ConfigurationContext, pendingTests: List<ApprovalTest>) {
        val message = StringBuilder("Approved ${pendingTests.size} test")
        if (pendingTests.size > 1) message.append("s")
        StatusBarUtil.setStatusBarInfo(context.project, message.append(".").toString())
    }

    private fun ConfigurationContext.isJUnit(): Boolean {
        val runnerConfig = configuration
        return runnerConfig != null && runnerConfig.type == findConfigurationType("JUnit")
    }

    private fun PsiClass.pathPrefix(): String = StringUtil.getPackageName(qualifiedName ?: "").replace(".", "/") + "/" + name

    private fun Project.findApprovalTests(filter: (VirtualFile) -> Boolean): Collection<ApprovalTest> =
        FilenameIndex.getAllFilesByExt(this, "approved")
            .filter(filter)
            .map { ApprovalTest(it, it.dotActualFile()) }

    private fun VirtualFile.dotActualFile(): VirtualFile? = parent.children.find { it.name == name.replace(Regex("approved$"), "actual") }

    private fun KtClass?.isJUnitTestClass() =
        this?.toLightClass()?.let { JUnitUtil.isTestClass(it, false, true) } ?: false

    private fun getTestClassInFile(ktFile: KtFile) =
        ktFile.declarations.filterIsInstance<KtClass>().singleOrNull { it.isJUnitTestClass() }

    private val AnActionEvent.configContext: ConfigurationContext get() = getFromContext(this.dataContext)


    private data class ApprovalTest(val approved: VirtualFile, val actual: VirtualFile?)
}
