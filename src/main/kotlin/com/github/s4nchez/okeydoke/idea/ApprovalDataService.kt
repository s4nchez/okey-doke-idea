package com.github.s4nchez.okeydoke.idea

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ApprovalDataService(private val project: Project) {

    private val approvals = mutableListOf<ApprovalData>()

    fun currentSelection() = approvals.toList()

    fun findTestsPendingApproval(event: AnActionEvent): List<ApprovalData> {
        val selection = when (event.place) {
            "TestTreeViewPopup" -> selectionFromTestTreeView(project, event)
            "ProjectViewPopup" -> selectionFromProjectView(project, event)
            "EditorPopup" -> selectionFromEditor(project, event)
            else -> emptyList()
        }
        return selection.also {
            approvals.clear()
            approvals.addAll(it)
        }.also { toApprove ->
            if (toApprove.isEmpty())
                println("No pending approvals found")
            else
                println("Pending approvals: \n${toApprove.joinToString("\n") { "- ${it.actual}" }}")
        }


    }

}