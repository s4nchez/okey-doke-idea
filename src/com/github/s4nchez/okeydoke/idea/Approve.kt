package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationContext.getFromContext
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex

class Approve : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val context = getFromContext(e.dataContext)
        context.psiElement().findApprovalTests().forEach { file -> file.actual?.approve() }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Approve Tests"
        val context = getFromContext(e.dataContext)
        e.presentation.isVisible = context.isJUnit()
        e.presentation.isEnabled = context.psiElement().findApprovalTests()
            .filter { t -> t.actual != null }
            .isNotEmpty()
    }

    fun PsiElement?.findApprovalTests(): Collection<ApprovalTest> {
        return when (this) {
            is PsiDirectory -> {
                val psiPackage = JavaDirectoryService.getInstance().getPackage(this)
                if (psiPackage != null) {
                    return project.findApprovalTests { f -> f.path.contains(psiPackage.qualifiedName.replace(".", "/")) }
                }
                listOf()
            }
            is PsiClass -> {
                return project.findApprovalTests { file -> file.path.contains(pathPrefix()) }
            }
            is PsiMethod -> {
                return project.findApprovalTests { file -> file.path.contains(containingClass?.pathPrefix() + "." + name) }
            }
            is PsiIdentifier -> {
                context.findApprovalTests()
            }
            else -> listOf()
        }
    }

    private fun ConfigurationContext.psiElement() = location?.psiElement

    private fun VirtualFile.approve() {
        runWriteAction {
            val approvalTestFileName = name.replace(Regex("actual$"), "approved")
            parent.findChild(approvalTestFileName)?.delete(this@Approve)
            rename(this@Approve, approvalTestFileName)
        }
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
}

data class ApprovalTest(val approved: VirtualFile, val actual: VirtualFile?)