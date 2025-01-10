package com.github.s4nchez.okeydoke.idea

import com.intellij.diff.*
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class ShowApprovalDiffs: AnAction() {

    private val myContentFactory = DiffContentFactoryEx.getInstanceEx()

    override fun actionPerformed(event: AnActionEvent) {
        val approvalDataService = event.project!!.service<ApprovalDataService>()
        val diffRequestFactory = service<DiffRequestFactory>()

        val chain = approvalDataService.currentSelection().map {
            diffRequestFactory.createFromFiles(event.project, it.approved, it.actual)
        }.let { SimpleDiffRequestChain(it) }

        DiffManager.getInstance().showDiff(event.project!!, chain, DiffDialogHints.DEFAULT)
    }

    override fun update(event: AnActionEvent) {
        val service = event.project!!.service<ApprovalDataService>()
        event.presentation.apply {
            isVisible = event.place in setOf("TestTreeViewPopup", "ProjectViewPopup", "EditorPopup")
            isEnabled = service.currentSelection().isNotEmpty()
        }
    }
}