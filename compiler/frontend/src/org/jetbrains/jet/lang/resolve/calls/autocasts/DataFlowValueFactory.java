package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.JetModuleUtil;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeChecker;

import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

/**
 * @author abreslav
 */
public class DataFlowValueFactory {
    public static final DataFlowValueFactory INSTANCE = new DataFlowValueFactory();

    private DataFlowValueFactory() {}

    @NotNull
    public DataFlowValue createDataFlowValue(@NotNull JetExpression expression, @NotNull JetType type, @NotNull BindingContext bindingContext) {
        if (expression instanceof JetConstantExpression) {
            JetConstantExpression constantExpression = (JetConstantExpression) expression;
            if (constantExpression.getNode().getElementType() == JetNodeTypes.NULL) return DataFlowValue.NULL;
        }
        if (JetTypeChecker.INSTANCE.equalTypes(type, JetStandardClasses.getNullableNothingType())) return DataFlowValue.NULL; // 'null' is the only inhabitant of 'Nothing?'
        Pair<Object, Boolean> result = getIdForStableIdentifier(expression, bindingContext, false);
        return new DataFlowValue(result.first == null ? expression : result.first, type, result.second, getImmanentNullability(type));
    }

    @NotNull
    public DataFlowValue createDataFlowValue(@NotNull ThisReceiverDescriptor receiver) {
        JetType type = receiver.getType();
        return new DataFlowValue(receiver.getDeclarationDescriptor(), type, true, getImmanentNullability(type));
    }

    @NotNull
    public DataFlowValue createDataFlowValue(@NotNull VariableDescriptor variableDescriptor) {
        JetType type = variableDescriptor.getOutType();
        return new DataFlowValue(variableDescriptor, type, isStableVariable(variableDescriptor), getImmanentNullability(type));
    }

//    private Object getId(@NotNull JetExpression expression, @NotNull BindingContext bindingContext) {
//        if (expression instanceof JetThisExpression) {
//            JetThisExpression thisExpression = (JetThisExpression) expression;
//            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, thisExpression.getThisReference());
//            if (declarationDescriptor instanceof CallableDescriptor) {
//                return ((CallableDescriptor) declarationDescriptor).getReceiverParameter();
//            }
//            if (declarationDescriptor instanceof ClassDescriptor) {
//                return ((ClassDescriptor) declarationDescriptor).getImplicitReceiver();
//            }
////            throw new AssertionError("No resolution data for expression " + expression.getText());
//        }
//        else if (expression instanceof JetReferenceExpression) {
//            JetReferenceExpression referenceExpression = (JetReferenceExpression) expression;
//            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, referenceExpression);
//            if (declarationDescriptor != null) {
//                return declarationDescriptor;
//            }
////            throw new AssertionError("No resolution data for expression " + expression.getText() + DiagnosticUtils.atLocation(expression));
//        }
//        return expression;
//    }

    private Nullability getImmanentNullability(JetType type) {
        return type.isNullable() ? Nullability.UNKNOWN : Nullability.NOT_NULL;
    }

    @NotNull
    private static Pair<Object, Boolean> getIdForStableIdentifier(@NotNull JetExpression expression, @NotNull BindingContext bindingContext, boolean allowNamespaces) {
        if (expression instanceof JetParenthesizedExpression) {
            JetParenthesizedExpression parenthesizedExpression = (JetParenthesizedExpression) expression;
            JetExpression innerExpression = parenthesizedExpression.getExpression();
            if (innerExpression == null) {
                return Pair.create(null, false);
            }
            return getIdForStableIdentifier(innerExpression, bindingContext, allowNamespaces);
        }
        else if (expression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) expression;
            JetExpression selectorExpression = qualifiedExpression.getSelectorExpression();
            if (selectorExpression == null) {
                return Pair.create(null, false);
            }
            Pair<Object, Boolean> receiverId = getIdForStableIdentifier(qualifiedExpression.getReceiverExpression(), bindingContext, true);
            Pair<Object, Boolean> selectorId = getIdForStableIdentifier(selectorExpression, bindingContext, allowNamespaces);
            return receiverId.second ? selectorId : Pair.create(receiverId.first, false);
        }
        if (expression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, simpleNameExpression);
            if (declarationDescriptor instanceof VariableDescriptor) {
                return Pair.create((Object) declarationDescriptor, isStableVariable((VariableDescriptor) declarationDescriptor));
            }
            if (declarationDescriptor instanceof NamespaceDescriptor) {
                return Pair.create((Object) declarationDescriptor, allowNamespaces);
            }
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                return Pair.create((Object) classDescriptor, classDescriptor.isClassObjectAValue());
            }
        }
        else if (expression instanceof JetThisExpression) {
            JetThisExpression thisExpression = (JetThisExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, thisExpression.getThisReference());
            if (declarationDescriptor instanceof CallableDescriptor) {
                return Pair.create((Object) ((CallableDescriptor) declarationDescriptor).getReceiverParameter(), true);
            }
            if (declarationDescriptor instanceof ClassDescriptor) {
                return Pair.create((Object) ((ClassDescriptor) declarationDescriptor).getImplicitReceiver(), true);
            }
            return Pair.create(null, true);
        }
        else if (expression instanceof JetRootNamespaceExpression) {
            return Pair.create((Object) JetModuleUtil.getRootNamespaceType(expression), allowNamespaces);
        }
        return Pair.create(null, false);
    }

    @Nullable
    public static VariableDescriptor getVariableDescriptorFromSimpleName(@NotNull BindingContext bindingContext, @NotNull JetExpression expression) {
        JetExpression receiver = JetPsiUtil.deparenthesize(expression);
        VariableDescriptor variableDescriptor = null;
        if (receiver instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) receiver;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, nameExpression);
            if (declarationDescriptor instanceof VariableDescriptor) {
                variableDescriptor = (VariableDescriptor) declarationDescriptor;
            }
        }
        return variableDescriptor;
    }

    public static boolean isStableVariable(@NotNull VariableDescriptor variableDescriptor) {
        if (variableDescriptor.isVar()) return false;
        if (variableDescriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
                if (classDescriptor.getModality().isOverridable() && propertyDescriptor.getModality().isOverridable()) return false;
            }
            else {
                assert !propertyDescriptor.getModality().isOverridable() : "Property outside a class must not be overridable";
            }
            // TODO: check that it's internal
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            if (getter != null && !getter.isDefault()) return false;
        }
        return true;
    }
}
