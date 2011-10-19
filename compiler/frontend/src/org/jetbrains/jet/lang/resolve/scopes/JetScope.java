package org.jetbrains.jet.lang.resolve.scopes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public interface JetScope {
    JetScope EMPTY = new JetScopeImpl() {
        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            throw new UnsupportedOperationException("Don't take containing declaration of the EMPTY scope");
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    @Nullable
    ClassifierDescriptor getClassifier(@NotNull String name);

    @Nullable
    NamespaceDescriptor getNamespace(@NotNull String name);

    @Nullable
    VariableDescriptor getVariable(@NotNull String name);

    @NotNull
    Set<FunctionDescriptor> getFunctions(@NotNull String name);

    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @NotNull
    Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName);

    /**
     * @param fieldName includes the "$"
     * @return the property declaring this field, if any
     */
    @Nullable
    PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName);

    @NotNull
    Collection<DeclarationDescriptor> getAllDescriptors();

    /**
     * @return EFFECTIVE implicit receiver at this point (may be corresponding to an outer scope)
     */
    @NotNull
    ReceiverDescriptor getImplicitReceiver();

    /**
     * Adds receivers to the list in order of locality, so that the closest (the most local) receiver goes first
     * @param result
     */
    void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result);
}