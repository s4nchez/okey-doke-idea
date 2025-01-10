package com.github.s4nchez.okeydoke.idea

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.Project

fun selectionFromProjectView(project: Project, event: AnActionEvent): List<ApprovalData> {
    val selection = PlatformCoreDataKeys.SELECTED_ITEMS.getData(event.dataContext) as? Array<*> ?: return emptyList()

    return selection
        .mapNotNull { selectedNode ->
            when (selectedNode) {
                is PsiFileNode -> if (selectedNode.virtualFile.isOkeydokeFile()) {
                    project.findApprovalTests { file ->
                        file.nameWithoutExtension == selectedNode.virtualFile?.nameWithoutExtension
                    }.distinct()
                } else emptyList()
                is PsiDirectoryNode -> selectedNode.virtualFile?.let { selectedDirectory ->
                    project.findApprovalTests { file -> file.path.contains(selectedDirectory.path) }
                } ?: emptyList()
                else -> emptyList()
            }
        }.flatten()
}
