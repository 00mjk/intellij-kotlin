package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.Variance;

/**
 * @author abreslav
 */
public interface ConstraintSystem {
    void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance);

    void addSubtypingConstraint(@NotNull SubtypingConstraint constraint);

    @NotNull
    ConstraintSystemSolution solve();
}
