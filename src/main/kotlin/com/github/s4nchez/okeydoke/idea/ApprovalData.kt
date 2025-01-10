package com.github.s4nchez.okeydoke.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

private val actualExtension = ".actual"
private val approvedExtension = ".approved"

data class ApprovalData(val approved: VirtualFile?, val actual: VirtualFile?)

fun VirtualFile?.isOkeydokeFile(): Boolean = if (this == null) false else
    name.contains(actualExtension) || name.contains(approvedExtension)

 fun Project.findApprovalTests(filter: (VirtualFile) -> Boolean): List<ApprovalData> {
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

private fun Project.findFilesByName(predicate: (String) -> Boolean): List<VirtualFile> =
    FilenameIndex.getAllFilenames(this)
        .filter(predicate)
        .flatMap { FilenameIndex.getVirtualFilesByName(it, GlobalSearchScope.allScope(this)).toList() }

fun VirtualFile.approve(approve: AnAction) {
    val requestor = approve
    val actualContent = contentsToByteArray()
    this.delete(requestor)

    val approvedFileName = name.replace(actualExtension, approvedExtension)
    val approvedFile = parent.findChild(approvedFileName) ?: parent.createChildData(requestor, approvedFileName)
    // Explicitly create new file (instead of e.g. renaming .actual file)
    // because it will trigger VCS listener which will add (or show dialog to add) .approved file to VCS.
    approvedFile.setBinaryContent(actualContent)
}