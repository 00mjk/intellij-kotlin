package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public interface ValueParameterDescriptor extends VariableDescriptor, Annotated {
    /**
     * Returns the 0-based index of the value parameter in the parameter list of its containing function.
     *
     * @return the parameter index
     */
    int getIndex();
    boolean hasDefaultValue();
    boolean isRef();
    @Nullable JetType getVarargElementType();

    @Override
    @NotNull
    JetType getOutType();

    @Override
    ValueParameterDescriptor getOriginal();

    @NotNull
    ValueParameterDescriptor copy(DeclarationDescriptor newOwner);

}
