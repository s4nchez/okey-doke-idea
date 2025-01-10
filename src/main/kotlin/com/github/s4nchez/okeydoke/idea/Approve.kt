package com.github.s4nchez.okeydoke.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationContext.getFromContext
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.testframework.TestTreeView
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.actionSystem.ActionPlaces.UNKNOWN
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.impl.status.StatusBarUtil
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.base.util.projectScope

class Approve : AnAction() {

    private val actualExtension = ".actual"
    private val approvedExtension = ".approved"

    override fun actionPerformed(event: AnActionEvent) {
        val context = event.configContext
        val pendingTests = findTestsPendingApproval(event)

        CommandProcessor.getInstance().executeCommand(
            event.project,
            { runWriteAction { pendingTests.forEach { file -> file.actual?.approve() } } },
            "Approve ${pendingTests.mapNotNull { it.actual?.name }.joinToString(", ")}",
            "Okeydoke Plugin"
        )

        updateStatusBar(context, pendingTests)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.apply {
            val context = event.configContext
            isVisible = context.isJUnit() || context.isOkeydokeFile()
            isEnabled = findTestsPendingApproval(event).isNotEmpty()
        }
    }

    private fun findTestsPendingApproval(event: AnActionEvent): List<ApprovalData> {
        val project = event.project ?: return emptyList()

        val selection = when (event.place) {
            "TestTreeViewPopup" -> selectionFromTestResultsPane(project, event)
            "ProjectViewPopup" -> selectionFromProjectView(project, event)
            "EditorPopup" -> selectionFromEditor(project, event)
            else -> emptyList()
        }

        return selection.also { toApprove ->
            if (toApprove.isEmpty())
                println("No pending approvals found")
            else
                println("Pending approvals: \n${toApprove.joinToString("\n") { "- ${it.actual}" }}")
        }
    }

    private fun selectionFromEditor(project: Project, event: AnActionEvent): List<ApprovalData> {
        val psiElement = event.configContext.location?.psiElement ?: return emptyList()

        val psiMethod = psiElement.currentTestMethod()
        if (psiMethod != null) {
            return project.findApprovalTests { file -> file.path.contains(psiMethod.containingClass?.pathPrefix() + "." + psiMethod.name) }
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

    private fun selectionFromProjectView(project: Project, event: AnActionEvent): List<ApprovalData> {
        val selection = event.dataContext.getData("selectedItems") as? Array<*> ?: return emptyList()

        return selection
            .mapNotNull { selectedNode ->
                when (selectedNode) {
                    is PsiFileNode -> if (selectedNode.virtualFile.isOkeydokeFile()) {
                        project.findApprovalTests { file -> file.nameWithoutExtension == selectedNode.virtualFile?.nameWithoutExtension }
                    } else emptyList()
                    //is PsiDirectoryNode -> TODO support packages
                    else -> emptyList()
                }
            }.flatten()
    }

    private fun selectionFromTestResultsPane(project: Project, event: AnActionEvent): List<ApprovalData> {
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
                    is KtLightMethod -> project.findApprovalTests { file -> file.path.contains(element.containingClass.pathPrefix() + "." + element.name) }
                    else -> {
                        println("Don't recognise ${element::class}"); emptyList()
                    }
                }
            }
            ?: emptyList())

    }

    private fun VirtualFile.approve() {
        val requestor = this@Approve
        val actualContent = contentsToByteArray()
        this.delete(requestor)

        val approvedFileName = name.replace(actualExtension, approvedExtension)
        val approvedFile = parent.findChild(approvedFileName) ?: parent.createChildData(requestor, approvedFileName)
        // Explicitly create new file (instead of e.g. renaming .actual file)
        // because it will trigger VCS listener which will add (or show dialog to add) .approved file to VCS.
        approvedFile.setBinaryContent(actualContent)
    }

    private fun updateStatusBar(context: ConfigurationContext, approvals: List<ApprovalData>) {
        val message = StringBuilder("Approved ${approvals.size} test")
        if (approvals.size > 1) message.append("s")
        StatusBarUtil.setStatusBarInfo(context.project, message.append(".").toString())
    }

    private fun ConfigurationContext.isOkeydokeFile(): Boolean {
        val file = psiLocation?.containingFile?.virtualFile ?: return false
        return file.isOkeydokeFile()
    }

    private fun VirtualFile?.isOkeydokeFile(): Boolean = if (this == null) false else
        name.contains(actualExtension) || name.contains(approvedExtension)

    private fun Project.findApprovalTests(filter: (VirtualFile) -> Boolean): List<ApprovalData> {
        val approvedFiles = findFilesByName { it.contains(approvedExtension) }.filter(filter)
        val actualFiles = findFilesByName { it.contains(actualExtension) }.filter(filter)

        return outerJoin(approvedFiles, actualFiles) { approved, actual -> areFilesForTheSameTest(approved, actual) }
            .map { (approved, actual) -> ApprovalData(approved, actual) }
            .filterNot { it.approved != null && it.actual == null }
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
        file1.path.replace(approvedExtension, "") == file2.path.replace(actualExtension, "") ||
                file1.path.replace(actualExtension, "") == file2.path.replace(approvedExtension, "")
}

private data class ApprovalData(val approved: VirtualFile?, val actual: VirtualFile?)

private fun ConfigurationContext.isJUnit() =
    setOf(findConfigurationType("GradleRunConfiguration"), findConfigurationType("JUnit"))
        .filterNotNull()
        .any { isCompatibleWithOriginalRunConfiguration(it) }


private val AnActionEvent.configContext: ConfigurationContext get() = getFromContext(this.dataContext, UNKNOWN)

private fun Project.findFilesByName(predicate: (String) -> Boolean): List<VirtualFile> =
    FilenameIndex.getAllFilenames(this)
        .filter(predicate)
        .flatMap { FilenameIndex.getVirtualFilesByName(it, GlobalSearchScope.allScope(this)).toList() }
