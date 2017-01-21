package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtilCore.iterateChildrenRecursively
import com.intellij.openapi.vfs.VirtualFile

class ApproveAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.module().contentRoots().forEach { root ->
            iterateChildrenRecursively(root, null, { file ->
                if (file.isApprovalTestFile()) {
                    runWriteAction { file.approve() }
                }
                true
            })
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Approve all '${e.module().name}' tests"
    }

    private fun Module.contentRoots(): Array<out VirtualFile> = ModuleRootManager.getInstance(this).contentRoots

    private fun AnActionEvent.module() = ConfigurationContext.getFromContext(dataContext).module

    private fun VirtualFile.isApprovalTestFile() = extension == "actual"

    private fun VirtualFile.approve() {
        val approvalTestFileName = name.replace(Regex("actual$"), "approved")
        parent.findChild(approvalTestFileName)?.delete(this@ApproveAction)
        rename(this@ApproveAction, approvalTestFileName)
    }
}
