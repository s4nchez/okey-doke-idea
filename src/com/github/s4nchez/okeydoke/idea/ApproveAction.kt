package com.github.s4nchez.okeydoke.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ApproveAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println("Approve them all!")
    }

}
