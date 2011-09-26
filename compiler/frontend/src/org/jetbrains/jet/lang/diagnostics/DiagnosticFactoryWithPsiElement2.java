package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public abstract class DiagnosticFactoryWithPsiElement2<T extends PsiElement, A, B> extends DiagnosticFactoryWithMessageFormat {

    protected DiagnosticFactoryWithPsiElement2(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    protected DiagnosticFactoryWithPsiElement2(Severity severity, String message) {
        super(severity, message);
    }

    protected String makeMessage(@NotNull A a, @NotNull B b) {
        return messageFormat.format(new Object[] {makeMessageForA(a), makeMessageForB(b)});
    }

    protected String makeMessageForA(@NotNull A a) {
        return renderer.render(a);
    }

    protected String makeMessageForB(@NotNull B b) {
        return renderer.render(b);
    }

    @NotNull
    public Diagnostic on(@NotNull T element, @NotNull A a, @NotNull B b) {
        return on(element, element.getNode(), a, b);
    }
    
    @NotNull
    public Diagnostic on(@NotNull T element, @NotNull ASTNode node, @NotNull A a, @NotNull B b) {
        return new DiagnosticWithPsiElementImpl<T>(this, severity, makeMessage(a, b), element, node.getTextRange());
    }
}
