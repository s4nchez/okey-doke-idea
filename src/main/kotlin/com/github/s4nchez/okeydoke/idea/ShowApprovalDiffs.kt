package com.github.s4nchez.okeydoke.idea

import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestFactoryImpl
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class ShowApprovalDiffs: AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val service = event.project!!.service<ApprovalDataService>()

        val chain = service.currentSelection().map {
            DiffRequestFactoryImpl().createFromFiles(event.project, it.approved, it.actual)
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