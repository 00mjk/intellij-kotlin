package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class AbstractScopeAdapter implements JetScope {
    @NotNull
    protected abstract JetScope getWorkerScope();

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return getWorkerScope().getImplicitReceiver();
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        getWorkerScope().getImplicitReceiversHierarchy(result);
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return getWorkerScope().getFunctionGroup(name);
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return getWorkerScope().getNamespace(name);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return getWorkerScope().getClassifier(name);
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        return getWorkerScope().getVariable(name);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return getWorkerScope().getContainingDeclaration();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        return getWorkerScope().getDeclarationsByLabel(labelName);
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        return getWorkerScope().getPropertyByFieldReference(fieldName);
    }

    @Override
    public DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis() {
        return getWorkerScope().getDeclarationDescriptorForUnqualifiedThis();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return getWorkerScope().getAllDescriptors();
    }
}
