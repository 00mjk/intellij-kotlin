package org.jetbrains.jet.asJava;

import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
public final class JetFileUtil {

    private JetFileUtil() {}

    public static List<JetFile> collectJetFiles(final Project project, @NotNull final GlobalSearchScope scope) {
        final List<JetFile> answer = new ArrayList<JetFile>();

        final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        List<VirtualFile> contentRoots = Arrays.asList(ProjectRootManager.getInstance(project).getContentRoots());
        final PsiManager manager = PsiManager.getInstance(project);

        CompilerPathsEx.visitFiles(contentRoots, new CompilerPathsEx.FileVisitor() {
            @Override
            protected void acceptFile(VirtualFile file, String fileRoot, String filePath) {
                final FileType fileType = fileTypeManager.getFileTypeByFile(file);
                if (fileType != JetFileType.INSTANCE) return;

                if (scope.accept(file)) {
                    final PsiFile psiFile = manager.findFile(file);
                    if (psiFile instanceof JetFile) {
                        answer.add((JetFile) psiFile);
                    }
                }
            }
        });

        return answer;
    }
}
