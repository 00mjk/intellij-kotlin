package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;

/**
* @author abreslav
*/
public class UnresolvedReferenceDiagnostic extends DiagnosticWithPsiElement<JetReferenceExpression> {

    public UnresolvedReferenceDiagnostic(JetReferenceExpression referenceExpression) {
        super(Errors.UNRESOLVED_REFERENCE, ERROR, "Unresolved reference", referenceExpression);
    }

    @NotNull
    @Override
    public String getMessage() {
        return super.getMessage() + ": " + getPsiElement().getText();
    }
}
