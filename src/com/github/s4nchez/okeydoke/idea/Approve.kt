package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationContext.getFromContext
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.StatusBarUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex

class Approve : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val context = event.configContext
        val pendingTests = findTestsPendingApprovalAt(context.psiElement())
        pendingTests.forEach { file -> file.actual?.approve() }
        updateStatusBar(context, pendingTests)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.apply {
            val context = event.configContext
            text = "Approve Tests"
            isVisible = context.isJUnit()
            isEnabled = findTestsPendingApprovalAt(context.psiElement()).isNotEmpty()
        }
    }

    private fun findTestsPendingApprovalAt(psiElement: PsiElement?) = findApprovalTestsAt(psiElement).filter { it.actual != null }

    private fun findApprovalTestsAt(psiElement: PsiElement?): Collection<ApprovalTest> {
        if (psiElement == null) return emptyList()

        val psiMethod = psiElement.currentTestMethod()
        if (psiMethod != null) {
            return psiElement.project.findApprovalTestsAt { file -> file.path.contains(psiMethod.containingClass?.pathPrefix() + "." + psiMethod.name) }
        }

        val psiClass = psiElement.currentTestClass()
        if (psiClass != null) {
            return psiElement.project.findApprovalTestsAt { file -> file.path.contains(psiClass.pathPrefix()) }
        }

        val psiPackage = psiElement.currentPackage()
        if (psiPackage != null) {
            return psiElement.project.findApprovalTestsAt { file -> file.path.contains(psiPackage.qualifiedName.replace(".", "/")) }
        }

        return emptyList()
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

    private fun Project.findApprovalTestsAt(filter: (VirtualFile) -> Boolean): Collection<ApprovalTest> =
        FilenameIndex.getAllFilesByExt(this, "approved")
            .filter(filter)
            .map { ApprovalTest(it, it.dotActualFile()) }

    private fun VirtualFile.dotActualFile(): VirtualFile? = parent.children.find { it.name == name.replace(Regex("approved$"), "actual") }

    private val AnActionEvent.configContext: ConfigurationContext get() = getFromContext(this.dataContext)


    private data class ApprovalTest(val approved: VirtualFile, val actual: VirtualFile?)
}
