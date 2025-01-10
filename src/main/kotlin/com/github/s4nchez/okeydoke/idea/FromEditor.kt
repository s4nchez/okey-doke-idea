package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.actions.ConfigurationContext.getFromContext
import com.intellij.openapi.actionSystem.ActionPlaces.UNKNOWN
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

fun selectionFromEditor(project: Project, event: AnActionEvent): List<ApprovalData> {
    val psiElement = getFromContext(event.dataContext, UNKNOWN).location?.psiElement ?: return emptyList()

    val psiMethod = psiElement.currentTestMethod()
    if (psiMethod != null) {
        return project.findApprovalTests { file ->
            file.path.contains(psiMethod.containingClass?.pathPrefix() + "." + psiMethod.name)
        }
    }

    val psiClass = psiElement.currentTestClass()
    if (psiClass != null) {
        return project.findApprovalTests { file -> file.path.contains(psiClass.pathPrefix()) }
    }

    val psiPackage = psiElement.currentPackage()
    if (psiPackage != null) {
        return project.findApprovalTests { file -> file.path.contains(psiPackage.qualifiedName.replace(".", "/")) }
    }

    return emptyList()
}
