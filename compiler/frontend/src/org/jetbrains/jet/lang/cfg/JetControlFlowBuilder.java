package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

import java.util.List;

/**
 * @author abreslav
 */
public interface JetControlFlowBuilder {
    void read(@NotNull JetElement element);
    void readUnit(@NotNull JetExpression expression);

    // General label management
    @NotNull
    Label createUnboundLabel();

    void bindLabel(@NotNull Label label);
    void allowDead();
    void stopAllowDead();

    // Jumps
    void jump(@NotNull Label label);
    void jumpOnFalse(@NotNull Label label);
    void jumpOnTrue(@NotNull Label label);
    void nondeterministicJump(Label label); // Maybe, jump to label
    void nondeterministicJump(List<Label> label);
    void jumpToError(JetThrowExpression expression);
    void jumpToError(JetExpression nothingExpression);

    // Entry/exit points
    Label getEntryPoint(@NotNull JetElement labelElement);
    Label getExitPoint(@NotNull JetElement labelElement);

    // Loops
    LoopInfo enterLoop(@NotNull JetExpression expression, @Nullable Label loopExitPoint, @Nullable Label conditionEntryPoint);

    void exitLoop(@NotNull JetExpression expression);
    @Nullable
    JetElement getCurrentLoop();

    // Finally
    void enterTryFinally(@NotNull GenerationTrigger trigger);
    void exitTryFinally();

    // Subroutines
    void enterSubroutine(@NotNull JetDeclaration subroutine);

    void exitSubroutine(@NotNull JetDeclaration subroutine);

    @NotNull
    JetElement getCurrentSubroutine();
    @Nullable
    JetElement getReturnSubroutine();
    void returnValue(@NotNull JetExpression returnExpression, @NotNull JetElement subroutine);

    void returnNoValue(@NotNull JetElement returnExpression, @NotNull JetElement subroutine);

    void write(@NotNull JetElement assignment, @NotNull JetElement lValue);
    
    void declare(@NotNull JetParameter parameter);
    void declare(@NotNull JetProperty property);

    // Other
    void unsupported(JetElement element);
}
