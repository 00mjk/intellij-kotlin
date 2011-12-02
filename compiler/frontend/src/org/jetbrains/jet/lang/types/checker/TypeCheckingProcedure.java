package org.jetbrains.jet.lang.types.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.List;

import static org.jetbrains.jet.lang.types.Variance.*;

/**
* @author abreslav
*/
public class TypeCheckingProcedure {

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    private static JetType findCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        if (constructor.equals(supertype.getConstructor())) {
            return subtype;
        }
        for (JetType immediateSupertype : constructor.getSupertypes()) {
            JetType correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype);
            if (correspondingSupertype != null) {
                return TypeSubstitutor.create(subtype).safeSubstitute(correspondingSupertype, Variance.INVARIANT);
            }
        }
        return null;
    }

    private static JetType getOutType(TypeParameterDescriptor parameter, TypeProjection argument) {
        boolean isOutProjected = argument.getProjectionKind() == IN_VARIANCE || parameter.getVariance() == IN_VARIANCE;
        return isOutProjected ? parameter.getUpperBoundsAsType() : argument.getType();
    }

    private static JetType getInType(TypeParameterDescriptor parameter, TypeProjection argument) {
        boolean isOutProjected = argument.getProjectionKind() == OUT_VARIANCE || parameter.getVariance() == OUT_VARIANCE;
        return isOutProjected ? JetStandardClasses.getNothingType() : argument.getType();
    }

    private final TypingConstraints constraints;

    public TypeCheckingProcedure(TypingConstraints constraints) {
        this.constraints = constraints;
    }

    public boolean equalTypes(@NotNull JetType type1, @NotNull JetType type2) {
        if (type1.isNullable() != type2.isNullable()) {
            return false;
        }

        if (type1.isNullable()) {
            // Then type2 is nullable, too (see the previous condition
            return constraints.assertEqualTypes(TypeUtils.makeNotNullable(type1), TypeUtils.makeNotNullable(type2), this);
        }

        TypeConstructor constructor1 = type1.getConstructor();
        TypeConstructor constructor2 = type2.getConstructor();

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false;
        }

        List<TypeProjection> type1Arguments = type1.getArguments();
        List<TypeProjection> type2Arguments = type2.getArguments();
        if (type1Arguments.size() != type2Arguments.size()) {
            return false;
        }

        for (int i = 0; i < type1Arguments.size(); i++) {
            TypeProjection typeProjection1 = type1Arguments.get(i);
            TypeProjection typeProjection2 = type2Arguments.get(i);
            if (typeProjection1.getProjectionKind() != typeProjection2.getProjectionKind()) {
                return false;
            }

            if (!constraints.assertEqualTypes(typeProjection1.getType(), typeProjection2.getType(), this)) {
                return false;
            }
        }
        return true;
    }

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
        if (ErrorUtils.isErrorType(subtype) || ErrorUtils.isErrorType(supertype)) {
            return true;
        }
        if (!supertype.isNullable() && subtype.isNullable()) {
            return false;
        }
        subtype = TypeUtils.makeNotNullable(subtype);
        supertype = TypeUtils.makeNotNullable(supertype);
        if (JetStandardClasses.isNothingOrNullableNothing(subtype)) {
            return true;
        }
        @Nullable JetType closestSupertype = findCorrespondingSupertype(subtype, supertype);
        if (closestSupertype == null) {
            return constraints.noCorrespondingSupertype(subtype, supertype); // if this returns true, there still isn't any supertype to continue with
        }

        return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
    }

    private boolean checkSubtypeForTheSameConstructor(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        assert constructor.equals(supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

        List<TypeProjection> subArguments = subtype.getArguments();
        List<TypeProjection> superArguments = supertype.getArguments();
        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);


            TypeProjection subArgument = subArguments.get(i);
            JetType subIn = getInType(parameter, subArgument);
            JetType subOut = getOutType(parameter, subArgument);

            TypeProjection superArgument = superArguments.get(i);
            JetType superIn = getInType(parameter, superArgument);
            JetType superOut = getOutType(parameter, superArgument);

            if (parameter.getVariance() == INVARIANT && subArgument.getProjectionKind() == INVARIANT && superArgument.getProjectionKind() == INVARIANT) {
                if (!constraints.assertEqualTypes(subArgument.getType(), superArgument.getType(), this)) return false;
            }
            else {
                if (!constraints.assertSubtype(subOut, superOut, this)) return false;
                if (!constraints.assertSubtype(superIn, subIn, this)) return false;
            }
        }
        return true;
    }
}
