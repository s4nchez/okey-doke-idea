package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationContext.getFromContext
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.StatusBarUtil
import com.intellij.psi.search.FilenameIndex
import java.util.*

class Approve : AnAction() {

    private val actualExtension = ".actual"
    private val approvedExtension = ".approved"

    override fun actionPerformed(event: AnActionEvent) {
        val context = event.configContext
        val pendingTests = context.findTestsPendingApproval()

        CommandProcessor.getInstance().executeCommand(
            event.project,
            { pendingTests.forEach { file -> file.actual?.approve() } },
            "Approve ${pendingTests.mapNotNull { it.actual?.name }.joinToString(", ")}",
            "Okeydoke Plugin"
        )

        updateStatusBar(context, pendingTests)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.apply {
            val context = event.configContext
            isVisible = context.isJUnit() || context.isOkeydokeFile()
            isEnabled = context.findTestsPendingApproval().isNotEmpty()
        }
    }

    private fun ConfigurationContext.findTestsPendingApproval(): List<ApprovalData> {
        val psiElement = location?.psiElement ?: return emptyList()

        val psiMethod = psiElement.currentTestMethod()
        if (psiMethod != null) {
            return psiElement.project.findApprovalTests { file -> file.path.contains(psiMethod.containingClass?.pathPrefix() + "." + psiMethod.name) }
        }

        val psiClass = psiElement.currentTestClass()
        if (psiClass != null) {
            return psiElement.project.findApprovalTests { file -> file.path.contains(psiClass.pathPrefix()) }
        }

        if (psiElement.containingFile != null && this.isOkeydokeFile()) {
            return psiElement.project.findApprovalTests { file -> file.nameWithoutExtension == psiElement.containingFile.virtualFile.nameWithoutExtension }
        }

        val psiPackage = psiElement.currentPackage()
        if (psiPackage != null) {
            return psiElement.project.findApprovalTests { file -> file.path.contains(psiPackage.qualifiedName.replace(".", "/")) }
        }

        return emptyList()
    }

    private fun VirtualFile.approve() {
        runUndoTransparentWriteAction {
            val approvalTestFileName = name.replacePostfix(actualExtension, approvedExtension)
            parent.findChild(approvalTestFileName)?.delete(this@Approve)
            rename(this@Approve, approvalTestFileName)
        }
    }

    private fun updateStatusBar(context: ConfigurationContext, approvals: List<ApprovalData>) {
        val message = StringBuilder("Approved ${approvals.size} test")
        if (approvals.size > 1) message.append("s")
        StatusBarUtil.setStatusBarInfo(context.project, message.append(".").toString())
    }

    private fun ConfigurationContext.isOkeydokeFile(): Boolean {
        val file = psiLocation?.containingFile?.virtualFile ?: return false
        return file.name.endsWith(actualExtension) || file.name.endsWith(approvedExtension)
    }

    private fun Project.findApprovalTests(filter: (VirtualFile) -> Boolean): List<ApprovalData> {
        val approvedFiles = FilenameIndex.getAllFilesByExt(this, approvedExtension.substring(1)).filter(filter)
        val actualFiles = FilenameIndex.getAllFilesByExt(this, actualExtension.substring(1)).filter(filter)

        return outerJoin(approvedFiles, actualFiles) { approved, actual -> areFilesForTheSameTest(approved, actual) }
            .map{ (approved, actual) -> ApprovalData(approved, actual) }
            .filterNot{ it.approved != null && it.actual == null }
    }

    private fun <T> outerJoin(left: List<T>, right: List<T>, match: (T, T) -> Boolean): List<Pair<T?, T?>> {
        val mutableRight = ArrayList(right)

        val leftAndInnerResult = left.mapNotNull { leftMatch ->
            val rightMatch = mutableRight.find { match(leftMatch, it) }
            if (rightMatch == null) {
                Pair(leftMatch, null)
            } else {
                mutableRight.remove(rightMatch)
                Pair(leftMatch, rightMatch)
            }
        }
        val rightResult = mutableRight.map { Pair(null, it) }

        return leftAndInnerResult + rightResult
    }

    private fun areFilesForTheSameTest(file1: VirtualFile, file2: VirtualFile) =
        file1.path.replacePostfix(approvedExtension, "") == file2.path.replacePostfix(actualExtension, "") ||
        file1.path.replacePostfix(actualExtension, "") == file2.path.replacePostfix(approvedExtension, "")
}


private data class ApprovalData(val approved: VirtualFile?, val actual: VirtualFile?)


private fun ConfigurationContext.isJUnit(): Boolean {
    val runnerConfig = configuration
    return runnerConfig != null && runnerConfig.type == findConfigurationType("JUnit")
}

private val AnActionEvent.configContext: ConfigurationContext get() = getFromContext(this.dataContext)

private fun String.replacePostfix(postfix: String, replacement: String) =
    if (!endsWith(postfix)) this else substring(0, length - postfix.length) + replacement
