package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public abstract class VariableDescriptorImpl extends DeclarationDescriptorImpl implements VariableDescriptor {
    private JetType inType;
    private JetType outType;

    public VariableDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            @Nullable JetType inType,
            @Nullable JetType outType) {
        super(containingDeclaration, annotations, name);
        assert (inType != null) || (outType != null);

        this.inType = inType;
        this.outType = outType;
    }

    protected VariableDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name
    )
    {
        super(containingDeclaration, annotations, name);
    }

    @Override
    public JetType getOutType() {
        return outType;
    }

    @Override
    public JetType getInType() {
        return inType;
    }

    protected void setInType(JetType inType) {
        this.inType = inType;
    }

    protected void setOutType(JetType outType) {
        this.outType = outType;
    }

    @Override
    @NotNull
    public VariableDescriptor getOriginal() {
        return (VariableDescriptor) super.getOriginal();
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Set<? extends CallableDescriptor> getOverriddenDescriptors() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public ReceiverDescriptor getReceiverParameter() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getExpectedThisObject() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @Override
    public JetType getReturnType() {
        return getOutType();
    }
}
