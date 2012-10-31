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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.data.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.prop.PropertyNameUtils;
import org.jetbrains.jet.lang.resolve.java.prop.PropertyParseResult;
import org.jetbrains.jet.lang.resolve.java.wrapper.*;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class MembersByNameCache {

    @NotNull
    private final Map<Name, NamedMembers> namedMembersMap = new HashMap<Name, NamedMembers>();

    @Nullable
    public NamedMembers get(@NotNull Name name) {
        return namedMembersMap.get(name);
    }

    @NotNull
    public Collection<NamedMembers> allMembers() {
        return namedMembersMap.values();
    }

    @NotNull
    private NamedMembers getOrCreateEmpty(@NotNull Name name) {
        NamedMembers r = namedMembersMap.get(name);
        if (r == null) {
            r = new NamedMembers(name);
            namedMembersMap.put(name, r);
        }
        return r;
    }

    @NotNull
    public static MembersByNameCache buildMembersByNameCache(
            @Nullable PsiClass psiClass,
            @Nullable PsiPackage psiPackage,
            boolean staticMembers,
            boolean isKotlin
    ) {
        PsiClassWrapper classWrapper = psiClass == null ? null : new PsiClassWrapper(psiClass);
        Builder builder = new Builder(classWrapper, psiPackage, staticMembers, isKotlin);
        builder.build();
        return builder.cache;
    }

    private static class Builder {
        private final PsiClassWrapper psiClass;
        private final PsiPackage psiPackage;
        private final boolean staticMembers;
        private final boolean kotlin;

        private final MembersByNameCache cache = new MembersByNameCache();

        private Builder(@Nullable PsiClassWrapper psiClass, @Nullable PsiPackage psiPackage, boolean staticMembers, boolean kotlin) {
            this.psiClass = psiClass;
            this.psiPackage = psiPackage;
            this.staticMembers = staticMembers;
            this.kotlin = kotlin;
        }

        @NotNull
        public MembersByNameCache build() {
            if (psiClass != null) {
                processFields();
                processMethods();
            }
            processObjectClasses();
        }

        private boolean includeMember(PsiMemberWrapper member) {
            if (psiClass.getPsiClass().isEnum() && staticMembers) {
                return member.isStatic();
            }

            if (member.isStatic() != staticMembers) {
                return false;
            }

            if (member.getPsiMember().getContainingClass() != psiClass.getPsiClass()) {
                return false;
            }

            if (member.isPrivate()) {
                return false;
            }

            return true;
        }

        private void processFields() {
            // Hack to load static members for enum class loaded from class file
            if (kotlin && !psiClass.getPsiClass().isEnum()) {
                return;
            }
            for (PsiField field0 : psiClass.getPsiClass().getAllFields()) {
                PsiFieldWrapper field = new PsiFieldWrapper(field0);

                // group must be created even for excluded field
                NamedMembers namedMembers = cache.getOrCreateEmpty(Name.identifier(field.getName()));

                if (!includeMember(field)) {
                    continue;
                }

                TypeSource type = new TypeSource("", field.getType(), field0);
                namedMembers.addPropertyAccessor(new PropertyAccessorData(field, type, null));
            }
        }

        private void processMethods() {
            for (PsiMethod method : psiClass.getPsiClass().getAllMethods()) {
                createEmptyEntry(Name.identifier(method.getName()));

                PropertyParseResult propertyParseResult = PropertyNameUtils.parseMethodToProperty(method.getName());
                if (propertyParseResult != null) {
                    cache.getOrCreateEmpty(Name.identifier(propertyParseResult.getPropertyName()));
                }
            }


            for (PsiMethod method0 : psiClass.getPsiClass().getMethods()) {
                PsiMethodWrapper method = new PsiMethodWrapper(method0);

                if (!includeMember(method)) {
                    continue;
                }

                PropertyParseResult propertyParseResult = PropertyNameUtils.parseMethodToProperty(method.getName());

                // TODO: remove getJavaClass
                if (propertyParseResult != null && propertyParseResult.isGetter()) {

                    String propertyName = propertyParseResult.getPropertyName();
                    NamedMembers members = cache.getOrCreateEmpty(Name.identifier(propertyName));

                    // TODO: some java properties too
                    if (method.getJetMethod().hasPropertyFlag()) {

                        int i = 0;

                        TypeSource receiverType;
                        if (i < method.getParameters().size() && method.getParameter(i).getJetValueParameter().receiver()) {
                            PsiParameterWrapper receiverParameter = method.getParameter(i);
                            receiverType = new TypeSource(receiverParameter.getJetValueParameter().type(), receiverParameter.getPsiParameter().getType(), receiverParameter.getPsiParameter());
                            ++i;
                        }
                        else {
                            receiverType = null;
                        }

                        while (i < method.getParameters().size() && method.getParameter(i).getJetTypeParameter().isDefined()) {
                            // TODO: store is reified
                            ++i;
                        }

                        if (i != method.getParameters().size()) {
                            // TODO: report error properly
                            throw new IllegalStateException("something is wrong with method " + method0);
                        }

                        // TODO: what if returnType == null?
                        final PsiType returnType = method.getReturnType();
                        assert returnType != null;
                        TypeSource propertyType = new TypeSource(method.getJetMethod().propertyType(), returnType, method.getPsiMethod());

                        members.addPropertyAccessor(new PropertyAccessorData(method, true, propertyType, receiverType));
                    }
                }
                else if (propertyParseResult != null && !propertyParseResult.isGetter()) {

                    String propertyName = propertyParseResult.getPropertyName();
                    NamedMembers members = cache.getOrCreateEmpty(Name.identifier(propertyName));

                    if (method.getJetMethod().hasPropertyFlag()) {
                        if (method.getParameters().size() == 0) {
                            // TODO: report error properly
                            throw new IllegalStateException();
                        }

                        int i = 0;

                        TypeSource receiverType = null;
                        PsiParameterWrapper p1 = method.getParameter(0);
                        if (p1.getJetValueParameter().receiver()) {
                            receiverType = new TypeSource(p1.getJetValueParameter().type(), p1.getPsiParameter().getType(), p1.getPsiParameter());
                            ++i;
                        }

                        while (i < method.getParameters().size() && method.getParameter(i).getJetTypeParameter().isDefined()) {
                            ++i;
                        }

                        if (i + 1 != method.getParameters().size()) {
                            throw new IllegalStateException();
                        }

                        PsiParameterWrapper propertyTypeParameter = method.getParameter(i);
                        TypeSource propertyType = new TypeSource(method.getJetMethod().propertyType(), propertyTypeParameter.getPsiParameter().getType(), propertyTypeParameter.getPsiParameter());

                        members.addPropertyAccessor(new PropertyAccessorData(method, false, propertyType, receiverType));
                    }
                }

                if (!method.getJetMethod().hasPropertyFlag()) {
                    NamedMembers namedMembers = cache.getOrCreateEmpty(Name.identifier(method.getName()));
                    namedMembers.addMethod(method);
                }
            }
        }

        private void createEmptyEntry(@NotNull Name identifier) {
            cache.getOrCreateEmpty(identifier);
        }
        
        private void processObjectClasses() {
            PsiClass[] classes = psiPackage != null ? psiPackage.getClasses() : psiClass.getPsiClass().getInnerClasses();
            for (PsiClass psiClass : classes) {
                if (!psiClass.isPhysical()) { // to filter out JetLightClasses
                    continue;
                }
                if (JetClassAnnotation.get(psiClass).kind() != JvmStdlibNames.FLAG_CLASS_KIND_OBJECT) {
                    continue;
                }
                PsiField instanceField = psiClass.findFieldByName(JvmAbi.INSTANCE_FIELD, false);
                if (instanceField != null) {
                    NamedMembers namedMembers = cache.getOrCreateEmpty(Name.identifier(psiClass.getName()));

                    TypeSource type = new TypeSource("", instanceField.getType(), instanceField);
                    namedMembers.addPropertyAccessor(new PropertyAccessorData(new PsiFieldWrapper(instanceField), type, null));
                }
            }
        }
    }
}
