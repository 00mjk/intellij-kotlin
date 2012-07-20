/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;

import java.util.*;

/**
 * @author svtk
 */
public class ConstraintSystemImpl implements ConstraintSystem {

    public static ConstraintType varianceToConstraintType(Variance variance) {
        if (variance == Variance.INVARIANT) {
            return ConstraintType.EQUAL;
        }
        if (variance == Variance.OUT_VARIANCE) {
            return ConstraintType.SUB_TYPE;
        }
        return ConstraintType.SUPER_TYPE;
    }

    private final Map<TypeParameterDescriptor, TypeBounds> typeParameterBounds = Maps.newLinkedHashMap();
    private final TypeSubstitutor typeSubstitutor;
    private final Queue<ConstraintPosition> errorConstraintPositions;
    private boolean error;

    public ConstraintSystemImpl() {
        this(false, Lists.<ConstraintPosition>newLinkedList());
    }

    public ConstraintSystemImpl(boolean error, Queue<ConstraintPosition> errorConstraintPositions) {
        this.error = error;
        this.errorConstraintPositions = errorConstraintPositions;
        this.typeSubstitutor = TypeSubstitutor.create(new TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                DeclarationDescriptor declarationDescriptor = key.getDeclarationDescriptor();
                if (declarationDescriptor instanceof TypeParameterDescriptor) {
                    TypeParameterDescriptor descriptor = (TypeParameterDescriptor) declarationDescriptor;

                    JetType value = getValue(descriptor);
                    if (value != null && !dependsOnTypeParameter(value, DONT_CARE.getConstructor())) {
                        return new TypeProjection(value);
                    }
                    return new TypeProjection(DONT_CARE);
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public String toString() {
                return typeParameterBounds.toString();
            }
        });
    }

    @Override
    public boolean hasError() {
        return error;
    }

    @Override
    public Queue<ConstraintPosition> getErrorConstraintPositions() {
        return errorConstraintPositions;
    }

    @Override
    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance) {
        typeParameterBounds.put(typeParameterDescriptor, new TypeBounds(positionVariance));
    }

    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull TypeBounds typeBounds) {
        typeParameterBounds.put(typeParameterDescriptor, typeBounds);
    }

    @Override
    public void addSubtypingConstraint(@NotNull JetType exactType, @NotNull JetType expectedType, @NotNull ConstraintPosition constraintPosition) {
        addConstraint(ConstraintType.SUB_TYPE, exactType, expectedType, constraintPosition);
    }

    private static boolean dependsOnTypeParameter(JetType type, TypeConstructor typeParameter) {
        if (type.getConstructor() == typeParameter) return true;
        for (TypeProjection typeProjection : type.getArguments()) {
            if (dependsOnTypeParameter(typeProjection.getType(), typeParameter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addConstraint(@NotNull ConstraintType constraintType, @NotNull JetType exactType, @NotNull JetType expectedType,
            @NotNull ConstraintPosition constraintPosition) {
        if (exactType == DONT_CARE || expectedType == DONT_CARE || exactType == TypeUtils.NO_EXPECTED_TYPE
            || expectedType == TypeUtils.NO_EXPECTED_TYPE) return;

        DeclarationDescriptor expectedTypeDescriptor = expectedType.getConstructor().getDeclarationDescriptor();
        DeclarationDescriptor exactTypeDescriptor = exactType.getConstructor().getDeclarationDescriptor();

        if (expectedTypeDescriptor instanceof TypeParameterDescriptor) {
            if (dependsOnTypeParameter(exactType, DONT_CARE.getConstructor())) return;
            if (expectedType.isNullable()) {
                exactType = TypeUtils.makeNotNullable(exactType);
            }
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) expectedTypeDescriptor;
            if (constraintType == ConstraintType.SUB_TYPE) {
                typeParameterBounds.get(typeParameter).addLowerBound(exactType);
            }
            else if (constraintType == ConstraintType.SUPER_TYPE) {
                typeParameterBounds.get(typeParameter).addUpperBound(exactType);
            }
            else {
                typeParameterBounds.get(typeParameter).setExactValue(exactType);
            }
            return;
        }

        if (exactTypeDescriptor instanceof ClassDescriptor && expectedTypeDescriptor instanceof ClassDescriptor) {
            if (constraintType != ConstraintType.SUPER_TYPE) {
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(exactType, expectedType);
                if (correspondingSupertype != null) {
                    exactType = correspondingSupertype;
                }
            }
            else {
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(expectedType, exactType);
                if (correspondingSupertype != null) {
                    expectedType = correspondingSupertype;
                }
            }

            if (exactType.getConstructor().getParameters().size() != expectedType.getConstructor().getParameters().size()) {
                errorConstraintPositions.add(constraintPosition);
                error = true;
                return;
            }

            ClassDescriptor subClass = (ClassDescriptor) exactType.getConstructor().getDeclarationDescriptor();
            ClassDescriptor superClass = (ClassDescriptor) expectedType.getConstructor().getDeclarationDescriptor();
            if (DescriptorUtils.isSubclass(subClass, superClass)) {
                List<TypeProjection> subArguments = exactType.getArguments();
                List<TypeProjection> superArguments = expectedType.getArguments();
                List<TypeParameterDescriptor> superParameters = expectedType.getConstructor().getParameters();
                for (int i = 0; i < superArguments.size(); i++) {
                    addConstraint(varianceToConstraintType(superParameters.get(i).getVariance()), subArguments.get(i).getType(), superArguments.get(i).getType(),
                                  constraintPosition);
                }
                return;
            }
        }
        error = true;
        errorConstraintPositions.add(constraintPosition);
    }

    private boolean checkConstraints(TypeParameterDescriptor typeParameterDescriptor) {
        //todo refactor
        if (error) {
            return false;
        }
        TypeBounds typeBounds = typeParameterBounds.get(typeParameterDescriptor);
        if (typeBounds == null || typeBounds.isEmpty()) return true;
        JetType exactType = null;
        if (typeBounds.getExactValues().size() > 1) {
            return false;
        }
        if (typeBounds.getExactValues().size() == 1) {
            exactType = typeBounds.getExactValues().iterator().next();
        }
        JetType superTypeOfLowerBounds = null;
        if (exactType == null && !typeBounds.getLowerBounds().isEmpty()) {
            superTypeOfLowerBounds = CommonSupertypes.commonSupertype(typeBounds.getLowerBounds());
        }
        JetType subTypeOfUpperBounds = null;
        if (exactType == null && superTypeOfLowerBounds == null && !typeBounds.getUpperBounds().isEmpty()) {
            subTypeOfUpperBounds = typeBounds.getUpperBounds().iterator().next(); //todo
        }
        JetType type = exactType != null ? exactType : superTypeOfLowerBounds != null ? superTypeOfLowerBounds : subTypeOfUpperBounds;
        if (type == null) {
            return false;
        }

        if (subTypeOfUpperBounds != null) return true;
        for (JetType upperType : typeBounds.getUpperBounds()) {
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(type, upperType)) {
                return false;
            }
        }
        if (superTypeOfLowerBounds != null) return true;
        for (JetType lowerType : typeBounds.getLowerBounds()) {
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(lowerType, type)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public JetType getValue(TypeParameterDescriptor typeParameter) {
        //todo all checks
        TypeBounds typeBounds = typeParameterBounds.get(typeParameter);
        //todo variance dependance
        if (typeBounds == null) {
            //todo assert typeBounds != null;
            return null;
        }
        if (typeBounds.isEmpty()) return null;
        Set<JetType> exactValues = typeBounds.getExactValues();
        if (!exactValues.isEmpty()) {
            return exactValues.iterator().next();
        }
        if (!typeBounds.getLowerBounds().isEmpty()) {
            return CommonSupertypes.commonSupertype(typeBounds.getLowerBounds());
        }
        if (!typeBounds.getUpperBounds().isEmpty()) {
            return typeBounds.getUpperBounds().iterator().next();
        }
        return null;
    }

    @NotNull
    public JetType getSafeValue(TypeParameterDescriptor typeParameter) {
        JetType type = getValue(typeParameter);
        if (type != null) {
            return type;
        }
        //todo may be error type
        return typeParameter.getUpperBoundsAsType();
    }

    @NotNull
    public Collection<JetType> getValues(TypeParameterDescriptor typeParameter) {
        TypeBounds typeBounds = typeParameterBounds.get(typeParameter);
        Set<JetType> values = Sets.newLinkedHashSet();
        if (typeBounds != null && !typeBounds.isEmpty()) {
            values.addAll(typeBounds.getExactValues());
            if (!typeBounds.getLowerBounds().isEmpty()) {
                JetType superTypeOfLowerBounds = CommonSupertypes.commonSupertype(typeBounds.getLowerBounds());
                for (JetType value : values) {
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(superTypeOfLowerBounds, value)) {
                        values.add(superTypeOfLowerBounds);
                    }
                }
            }
        }
        //todo subTypeOfLowerBounds
        return values;
    }

    @Override
    @Nullable
    public TypeParameterDescriptor getFirstConflictingParameter() {
        for (TypeParameterDescriptor typeParameter : typeParameterBounds.keySet()) {
            Collection<JetType> values = getValues(typeParameter);
            if (!values.isEmpty()) {
                return typeParameter;
            }
        }
        return null;
    }

    @Override
    public Collection<TypeSubstitutor> getSubstitutors() {
        TypeParameterDescriptor firstConflictingParameter = getFirstConflictingParameter();
        if (firstConflictingParameter == null) return Collections.emptyList();

        Collection<JetType> conflictingTypes = getValues(firstConflictingParameter);

        ArrayList<Map<TypeConstructor, TypeProjection>> substitutionContexts = Lists.newArrayList();
        for (JetType type : conflictingTypes) {
            Map<TypeConstructor, TypeProjection> context = Maps.newLinkedHashMap();
            context.put(firstConflictingParameter.getTypeConstructor(), new TypeProjection(type));
            substitutionContexts.add(context);
        }

        for (TypeParameterDescriptor typeParameter : typeParameterBounds.keySet()) {
            if (typeParameter == firstConflictingParameter) continue;

            JetType safeType = getSafeValue(typeParameter);
            for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
                TypeProjection typeProjection = new TypeProjection(safeType);
                context.put(typeParameter.getTypeConstructor(), typeProjection);
            }
        }
        Collection<TypeSubstitutor> typeSubstitutors = Lists.newArrayList();
        for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
            typeSubstitutors.add(TypeSubstitutor.create(context));
        }
        return typeSubstitutors;
    }

    @Override
    @NotNull
    public TypeBounds getTypeBounds(TypeParameterDescriptor typeParameterDescriptor) {
        return typeParameterBounds.get(typeParameterDescriptor);
    }

    @Override
    public Map<TypeParameterDescriptor, TypeBounds> getTypeBoundsMap() {
        return typeParameterBounds;
    }

    @Override
    public boolean isSuccessful() {
        return !error && !hasUnknownParameters() && !hasContradiction();
    }

    @Override
    public boolean hasContradiction() {
        for (TypeParameterDescriptor typeParameter : typeParameterBounds.keySet()) {
            if (!checkConstraints(typeParameter)) return true;
        }
        return false;
    }

    private boolean hasUnknownParameters() {
        for (TypeBounds bounds : typeParameterBounds.values()) {
            if (bounds.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TypeSubstitutor getSubstitutor() {
        return typeSubstitutor;
    }

    public boolean upperBoundsAreSatisfied() {
        for (TypeParameterDescriptor typeParameter : typeParameterBounds.keySet()) {
            JetType type = getValue(typeParameter);
            JetType upperBound = typeParameter.getUpperBoundsAsType();
            JetType substitute = getSubstitutor().substitute(upperBound, Variance.INVARIANT);

            if (type != null) {
                if (substitute == null || !JetTypeChecker.INSTANCE.isSubtypeOf(type, substitute)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean checkUpperBound(@NotNull TypeParameterDescriptor typeParameter) {
        assert typeParameterBounds.containsKey(typeParameter);
        JetType type = getValue(typeParameter);
        JetType upperBound = typeParameter.getUpperBoundsAsType();
        JetType substitute = getSubstitutor().substitute(upperBound, Variance.INVARIANT);

        if (type != null) {
            if (substitute == null || !JetTypeChecker.INSTANCE.isSubtypeOf(type, substitute)) {
                return false;
            }
        }
        return true;
    }
}
