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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassMembersScope;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameBase;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorResolver.createEnumClassObjectValueOfMethod;
import static org.jetbrains.jet.lang.resolve.DescriptorResolver.createEnumClassObjectValuesMethod;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;

public class ClassResolver {
    private final JavaDescriptorResolver javaDescriptorResolver;

    public ClassResolver(JavaDescriptorResolver javaDescriptorResolver) {
        this.javaDescriptorResolver = javaDescriptorResolver;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        List<Runnable> tasks = Lists.newArrayList();
        ClassDescriptor clazz = resolveClass(qualifiedName, searchRule, tasks);
        for (Runnable task : tasks) {
            task.run();
        }
        return clazz;
    }

    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return resolveClass(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    public ClassDescriptor resolveClass(
            @NotNull FqName qualifiedName,
            @NotNull DescriptorSearchRule searchRule,
            @NotNull List<Runnable> tasks
    ) {
        if (qualifiedName.getFqName().endsWith(JvmAbi.TRAIT_IMPL_SUFFIX)) {
            // TODO: only if -$$TImpl class is created by Kotlin
            return null;
        }

        ClassDescriptor builtinClassDescriptor =
                javaDescriptorResolver.getSemanticServices().getKotlinBuiltinClassDescriptor(qualifiedName);
        if (builtinClassDescriptor != null) {
            return builtinClassDescriptor;
        }

        // First, let's check that this is a real Java class, not a Java's view on a Kotlin class:
        ClassDescriptor kotlinClassDescriptor = javaDescriptorResolver.getSemanticServices().getKotlinClassDescriptor(qualifiedName);
        if (kotlinClassDescriptor != null) {
            if (searchRule == DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN) {
                throw new IllegalStateException("class must not be found in kotlin: " + qualifiedName);
            }
            else if (searchRule == DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN) {
                return null;
            }
            else if (searchRule == DescriptorSearchRule.INCLUDE_KOTLIN) {
                return kotlinClassDescriptor;
            }
            else {
                throw new IllegalStateException("unknown searchRule: " + searchRule);
            }
        }

        // Not let's take a descriptor of a Java class
        JavaDescriptorResolveData.ResolverClassData classData = javaDescriptorResolver.getClassDescriptorCache().get(qualifiedName);
        if (classData == null) {
            PsiClass psiClass =
                    javaDescriptorResolver.getPsiClassFinder().findPsiClass(qualifiedName, PsiClassFinder.RuntimeClassesHandleMode.THROW);
            if (psiClass == null) {
                JavaDescriptorResolveData.ResolverClassData oldValue =
                        javaDescriptorResolver.getClassDescriptorCache()
                                .put(qualifiedName, JavaDescriptorResolveData.ResolverBinaryClassData.NEGATIVE);
                if (oldValue != null) {
                    throw new IllegalStateException("rewrite at " + qualifiedName);
                }
                return null;
            }
            classData = createJavaClassDescriptor(psiClass, tasks);
        }
        return classData.getClassDescriptor();
    }

    @NotNull
    public JavaDescriptorResolveData.ResolverClassData createJavaClassDescriptor(
            @NotNull final PsiClass psiClass,
            List<Runnable> taskList
    ) {
        final String qualifiedName = psiClass.getQualifiedName();
        assert qualifiedName != null;

        FqName fqName = new FqName(qualifiedName);
        if (javaDescriptorResolver.getClassDescriptorCache().containsKey(fqName)) {
            throw new IllegalStateException(qualifiedName);
        }

        JavaDescriptorResolver.checkPsiClassIsNotJet(psiClass);

        Name name = Name.identifier(psiClass.getName());
        JetClassAnnotation jetClassAnnotation = JetClassAnnotation.get(psiClass);
        ClassKind kind = getClassKind(psiClass, jetClassAnnotation);
        ClassOrNamespaceDescriptor containingDeclaration = javaDescriptorResolver.resolveParentDescriptor(psiClass);

        // class may be resolved during resolution of parent
        JavaDescriptorResolveData.ResolverClassData classData = javaDescriptorResolver.getClassDescriptorCache().get(fqName);
        if (classData != null) {
            return classData;
        }

        classData = new ClassDescriptorFromJvmBytecode(
                containingDeclaration, kind, psiClass, fqName, javaDescriptorResolver)
                .getResolverBinaryClassData();
        javaDescriptorResolver.getClassDescriptorCache().put(fqName, classData);
        classData.getClassDescriptor().setName(name);

        List<JetType> supertypes = new ArrayList<JetType>();

        List<JavaDescriptorSignatureResolver.TypeParameterDescriptorInitialization> typeParameterDescriptorInitializations
                = javaDescriptorResolver.getJavaDescriptorSignatureResolver().createUninitializedClassTypeParameters(psiClass, classData);

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>();
        for (JavaDescriptorSignatureResolver.TypeParameterDescriptorInitialization typeParameter : typeParameterDescriptorInitializations) {
            typeParameters.add(typeParameter.getDescriptor());
        }

        classData.getClassDescriptor().setTypeParameterDescriptors(typeParameters);
        classData.getClassDescriptor().setSupertypes(supertypes);
        classData.getClassDescriptor().setVisibility(JavaDescriptorResolver.resolveVisibility(psiClass, jetClassAnnotation));
        Modality modality;
        if (classData.getClassDescriptor().getKind() == ClassKind.ANNOTATION_CLASS) {
            modality = Modality.FINAL;
        }
        else {
            modality = Modality.convertFromFlags(
                    psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                    !psiClass.hasModifierProperty(PsiModifier.FINAL));
        }
        classData.getClassDescriptor().setModality(modality);
        classData.getClassDescriptor().createTypeConstructor();
        classData.getClassDescriptor()
                .setScopeForMemberLookup(new JavaClassMembersScope(javaDescriptorResolver.getSemanticServices(), classData));

        javaDescriptorResolver.getJavaDescriptorSignatureResolver()
                .initializeTypeParameters(typeParameterDescriptorInitializations, classData.getClassDescriptor(), "class " + qualifiedName);

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        List<TypeParameterDescriptor> classTypeParameters = classData.getClassDescriptor().getTypeConstructor().getParameters();
        supertypes.addAll(javaDescriptorResolver.getSupertypes(new PsiClassWrapper(psiClass), classData, classTypeParameters));

        MutableClassDescriptorLite classObject = createClassObjectDescriptor(classData.getClassDescriptor(), psiClass);
        if (classObject != null) {
            classData.getClassDescriptor().getBuilder().setClassObjectDescriptor(classObject);
        }

        classData.getClassDescriptor().setAnnotations(javaDescriptorResolver.resolveAnnotations(psiClass, taskList));

        javaDescriptorResolver.getTrace().record(BindingContext.CLASS, psiClass, classData.getClassDescriptor());

        return classData;
    }

    @Nullable
    public MutableClassDescriptorLite createClassObjectDescriptor(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        JavaDescriptorResolver.checkPsiClassIsNotJet(psiClass);

        if (psiClass.isEnum()) {
            return createClassObjectDescriptorForEnum(containing, psiClass);
        }

        if (!JavaDescriptorResolver.isKotlinClass(psiClass)) {
            return null;
        }

        // If there's at least one inner enum, we need to create a class object (to put this enum into)
        for (PsiClass innerClass : psiClass.getInnerClasses()) {
            if (JavaDescriptorResolver.isInnerEnum(innerClass, containing)) {
                return createSyntheticClassObject(containing, psiClass);
            }
        }

        PsiClass classObjectPsiClass = JavaDescriptorResolver.getInnerClassClassObject(psiClass);
        if (classObjectPsiClass == null) {
            return null;
        }

        final String qualifiedName = classObjectPsiClass.getQualifiedName();
        assert qualifiedName != null;
        FqName fqName = new FqName(qualifiedName);
        JavaDescriptorResolveData.ResolverClassData classData = new ClassDescriptorFromJvmBytecode(
                containing, ClassKind.CLASS_OBJECT, classObjectPsiClass, fqName, javaDescriptorResolver)
                .getResolverBinaryClassData();

        ClassDescriptorFromJvmBytecode classObjectDescriptor = classData.getClassDescriptor();
        classObjectDescriptor.setSupertypes(
                javaDescriptorResolver
                        .getSupertypes(new PsiClassWrapper(classObjectPsiClass), classData, new ArrayList<TypeParameterDescriptor>(0)));
        setUpClassObjectDescriptor(containing, fqName, classData, getClassObjectName(containing.getName()));
        return classObjectDescriptor;
    }

    public void setUpClassObjectDescriptor(
            @NotNull ClassDescriptor containing,
            @NotNull FqNameBase fqName,
            @NotNull JavaDescriptorResolveData.ResolverClassData data,
            @NotNull Name classObjectName
    ) {
        ClassDescriptorFromJvmBytecode classDescriptor = data.getClassDescriptor();
        javaDescriptorResolver.getClassDescriptorCache().put(fqName, data);
        classDescriptor.setName(classObjectName);
        classDescriptor.setModality(Modality.FINAL);
        classDescriptor.setVisibility(containing.getVisibility());
        classDescriptor.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        classDescriptor.createTypeConstructor();
        JavaClassMembersScope classMembersScope = new JavaClassMembersScope(javaDescriptorResolver.getSemanticServices(), data);
        WritableScopeImpl writableScope =
                new WritableScopeImpl(classMembersScope, classDescriptor, RedeclarationHandler.THROW_EXCEPTION, fqName.toString());
        writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        classDescriptor.setScopeForMemberLookup(writableScope);
    }

    @NotNull
    private MutableClassDescriptorLite createClassObjectDescriptorForEnum(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        MutableClassDescriptorLite classObjectDescriptor = createSyntheticClassObject(containing, psiClass);

        classObjectDescriptor.getBuilder()
                .addFunctionDescriptor(createEnumClassObjectValuesMethod(classObjectDescriptor, javaDescriptorResolver.getTrace()));
        classObjectDescriptor.getBuilder()
                .addFunctionDescriptor(createEnumClassObjectValueOfMethod(classObjectDescriptor, javaDescriptorResolver.getTrace()));

        return classObjectDescriptor;
    }

    @NotNull
    private MutableClassDescriptorLite createSyntheticClassObject(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        String psiClassQualifiedName = psiClass.getQualifiedName();
        assert psiClassQualifiedName != null : "Reading java class with no qualified name";
        FqNameUnsafe fqName = new FqNameUnsafe(psiClassQualifiedName + "." + getClassObjectName(psiClass.getName()).getName());
        ClassDescriptorFromJvmBytecode classObjectDescriptor = new ClassDescriptorFromJvmBytecode(
                containing, ClassKind.CLASS_OBJECT, psiClass, null, javaDescriptorResolver);

        JavaDescriptorResolveData.ResolverSyntheticClassObjectClassData
                data = new JavaDescriptorResolveData.ResolverSyntheticClassObjectClassData(psiClass, null, classObjectDescriptor);
        setUpClassObjectDescriptor(containing, fqName, data, getClassObjectName(containing.getName().getName()));

        return classObjectDescriptor;
    }

    public static ClassKind getClassKind(@NotNull PsiClass psiClass, @NotNull JetClassAnnotation jetClassAnnotation) {
        if (psiClass.isInterface()) {
            return (psiClass.isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT);
        }
        else {
            if (psiClass.isEnum()) {
                return ClassKind.ENUM_CLASS;
            }
            else {
                return jetClassAnnotation.kind() == JvmStdlibNames.FLAG_CLASS_KIND_OBJECT ? ClassKind.OBJECT : ClassKind.CLASS;
            }
        }
    }
}