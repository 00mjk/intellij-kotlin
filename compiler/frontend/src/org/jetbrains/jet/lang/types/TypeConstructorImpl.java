package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotatedImpl;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeConstructorImpl extends AnnotatedImpl implements TypeConstructor {
    private final List<TypeParameterDescriptor> parameters;
    private Collection<JetType> supertypes;
    private final String debugName;
    private final boolean sealed;

    @Nullable
    private final ClassifierDescriptor classifierDescriptor;

    public TypeConstructorImpl(
            @Nullable ClassifierDescriptor classifierDescriptor,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean sealed,
            @NotNull String debugName,
            @NotNull List<TypeParameterDescriptor> parameters,
            @NotNull Collection<JetType> supertypes) {
        super(annotations);
        this.classifierDescriptor = classifierDescriptor;
        this.sealed = sealed;
        this.debugName = debugName;
        this.parameters = Collections.unmodifiableList(new ArrayList<TypeParameterDescriptor>(parameters));
        this.supertypes = Collections.unmodifiableCollection(supertypes);
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getParameters() {
        return parameters;
    }

    @Override
    @NotNull
    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    @Override
    public String toString() {
        return debugName;
    }

    @Override
    public boolean isSealed() {
        return sealed;
    }

    @Override
    @Nullable
    public ClassifierDescriptor getDeclarationDescriptor() {
        return classifierDescriptor;
    }
}
