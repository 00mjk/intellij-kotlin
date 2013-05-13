/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.preloading.instrumentation;

public class MethodDataImpl extends MemberDataImpl implements MethodData {
    private final FieldData ownerField;
    private final int parameterCount;
    private final int thisParameterIndex;
    private final int methodNameParameterIndex;
    private final int methodDescParameterIndex;
    private final int allArgsParameterIndex;

    MethodDataImpl(
            FieldData ownerField,
            String declaringClass,
            String name,
            String desc,
            int parameterCount,
            int thisParameterIndex,
            int methodNameParameterIndex,
            int methodDescParameterIndex,
            int allArgsParameterIndex
    ) {
        super(declaringClass, name, desc);
        this.ownerField = ownerField;
        this.parameterCount = parameterCount;
        this.thisParameterIndex = thisParameterIndex;
        this.methodNameParameterIndex = methodNameParameterIndex;
        this.methodDescParameterIndex = methodDescParameterIndex;
        this.allArgsParameterIndex = allArgsParameterIndex;
    }

    @Override
    public FieldData getOwnerField() {
        return ownerField;
    }

    @Override
    public int getParameterCount() {
        return parameterCount;
    }

    @Override
    public int getThisParameterIndex() {
        return thisParameterIndex;
    }

    @Override
    public int getMethodNameParameterIndex() {
        return methodNameParameterIndex;
    }

    @Override
    public int getMethodDescParameterIndex() {
        return methodDescParameterIndex;
    }

    @Override
    public int getAllArgsParameterIndex() {
        return allArgsParameterIndex;
    }
}
