package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.testframework.TestTreeView
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.base.util.projectScope

fun selectionFromTestTreeView(project: Project, event: AnActionEvent): List<ApprovalData> {
    return (TestTreeView.MODEL_DATA_KEY.getData(event.dataContext)?.treeView
        ?.let { it.selectionPaths?.mapNotNull { testPath -> it.getSelectedTest(testPath) } ?: emptyList() }
        ?.mapNotNull { selectedTest ->
            val locationUrl = selectedTest.locationUrl ?: return@mapNotNull null
            if (selectedTest is SMTestProxy) {
                event.project?.let { project ->
                    val protocol = VirtualFileManager.extractProtocol(locationUrl)!!
                    val path =
                        VirtualFileManager.extractPath(locationUrl).removeSuffix("(Approver)")
                    selectedTest.locator.getLocation(protocol, path, project, project.projectScope())
                }
            } else null
        }?.flatten()
        ?.mapNotNull { it.psiElement }
        ?.flatMap { element ->
            when (element) {
                is KtLightClass -> project.findApprovalTests { file -> file.path.contains(element.pathPrefix()) }
                is KtLightMethod -> project.findApprovalTests { file ->
                    file.path.contains(element.containingClass.pathPrefix() + "." + element.name)
                }
                else -> emptyList()
            }
        }
        ?: emptyList())

}
