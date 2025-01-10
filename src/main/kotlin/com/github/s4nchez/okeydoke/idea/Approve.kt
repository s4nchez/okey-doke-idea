package com.github.s4nchez.okeydoke.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.status.StatusBarUtil

class Approve : AnAction() {



    override fun actionPerformed(event: AnActionEvent) {
        val service = event.project!!.service<ApprovalDataService>()
        val pendingTests = service.currentSelection()

        CommandProcessor.getInstance().executeCommand(
            event.project,
            { runWriteAction { pendingTests.forEach { file -> file.actual?.approve(this) } } },
            "Approve ${pendingTests.mapNotNull { it.actual?.name }.joinToString(", ")}",
            "Okeydoke Plugin"
        )

        updateStatusBar(event.project, pendingTests)
    }

    override fun update(event: AnActionEvent) {
        if(event.project == null) return
        val service = event.project!!.service<ApprovalDataService>()

        event.presentation.apply {
            isVisible = event.place in setOf("TestTreeViewPopup", "ProjectViewPopup", "EditorPopup")
            isEnabled = service.findTestsPendingApproval(event).isNotEmpty()
        }
    }


    private fun updateStatusBar(project: Project?, approvals: List<ApprovalData>) {
        if (project == null) return
        val message = StringBuilder("Approved ${approvals.size} test")
        if (approvals.size > 1) message.append("s")
        StatusBarUtil.setStatusBarInfo(project, message.append(".").toString())
    }
}
