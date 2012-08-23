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

/*
 * @author max
 * @author alex.tkachman
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.signature.SignatureWriter;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;

public class ClosureCodegen extends ObjectOrClosureCodegen {

    private final BindingContext bindingContext;
    private final ClosureAnnotator closureAnnotator;
    private final JetTypeMapper typeMapper;

    public ClosureCodegen(GenerationState state, ExpressionCodegen exprContext, CodegenContext context) {
        super(exprContext, context, state);
        bindingContext = state.getBindingContext();
        typeMapper = this.state.getInjector().getJetTypeMapper();
        closureAnnotator = typeMapper.getClosureAnnotator();
    }

    private static JvmMethodSignature erasedInvokeSignature(FunctionDescriptor fd) {

        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        signatureWriter.writeFormalTypeParametersStart();
        signatureWriter.writeFormalTypeParametersEnd();

        boolean isExtensionFunction = fd.getReceiverParameter().exists();
        int paramCount = fd.getValueParameters().size();
        if (isExtensionFunction) {
            paramCount++;
        }

        signatureWriter.writeParametersStart();

        for (int i = 0; i < paramCount; ++i) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            signatureWriter.writeAsmType(JetTypeMapper.TYPE_OBJECT, true);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        signatureWriter.writeAsmType(JetTypeMapper.TYPE_OBJECT, true);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature("invoke");
    }

    public static CallableMethod asCallableMethod(FunctionDescriptor fd, @NotNull JetTypeMapper typeMapper) {
        JvmMethodSignature descriptor = erasedInvokeSignature(fd);
        JvmClassName owner = getInternalClassName(fd);
        Type receiverParameterType;
        if (fd.getReceiverParameter().exists()) {
            receiverParameterType = typeMapper.mapType(fd.getOriginal().getReceiverParameter().getType(), MapTypeMode.VALUE);
        }
        else {
            receiverParameterType = null;
        }
        return new CallableMethod(
                owner, null, null, descriptor, INVOKEVIRTUAL,
                getInternalClassName(fd), receiverParameterType, getInternalClassName(fd).getAsmType());
    }

    protected JvmMethodSignature invokeSignature(FunctionDescriptor fd) {
        return typeMapper.mapSignature(Name.identifier("invoke"), fd);
    }

    public GeneratedAnonymousClassDescriptor gen(JetExpression fun) {
        final Pair<JvmClassName, ClassBuilder> nameAndVisitor = state.forAnonymousSubclass(fun);

        final FunctionDescriptor funDescriptor = bindingContext.get(BindingContext.FUNCTION, fun);

        cv = nameAndVisitor.getSecond();
        name = nameAndVisitor.getFirst();

        SignatureWriter signatureWriter = new SignatureWriter();

        assert funDescriptor != null;
        final List<ValueParameterDescriptor> parameters = funDescriptor.getValueParameters();
        final JvmClassName funClass = getInternalClassName(funDescriptor);
        signatureWriter.visitClassType(funClass.getInternalName());
        for (ValueParameterDescriptor parameter : parameters) {
            appendType(signatureWriter, parameter.getType(), '=');
        }

        appendType(signatureWriter, funDescriptor.getReturnType(), '=');
        signatureWriter.visitEnd();

        cv.defineClass(fun,
                       V1_6,
                       ACC_PUBLIC/*|ACC_SUPER*/,
                       name.getInternalName(),
                       null,
                       funClass.getInternalName(),
                       new String[0]
        );
        cv.visitSource(fun.getContainingFile().getName(), null);


        generateBridge(name.getInternalName(), funDescriptor, fun, cv);
        captureThis = generateBody(funDescriptor, cv, (JetDeclarationWithBody) fun);

        final Type enclosingType = !context.hasThisDescriptor()
                                   ? null
                                   : typeMapper.mapType(context.getThisDescriptor().getDefaultType(), MapTypeMode.VALUE);
        if (enclosingType == null) {
            captureThis = null;
        }

        final Method constructor = generateConstructor(funClass, fun, funDescriptor);

        if (captureThis != null) {
            cv.newField(fun, ACC_FINAL, "this$0", enclosingType.getDescriptor(), null, null);
        }

        if (isConst()) {
            generateConstInstance(fun);
        }

        cv.done();

        final GeneratedAnonymousClassDescriptor answer =
                new GeneratedAnonymousClassDescriptor(name, constructor, captureThis, captureReceiver);
        for (DeclarationDescriptor descriptor : closure.keySet()) {
            if (descriptor == funDescriptor) {
                continue;
            }
            if (descriptor instanceof VariableDescriptor || CodegenUtil.isNamedFun(descriptor, state.getBindingContext()) &&
                                                            descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
                final EnclosedValueDescriptor valueDescriptor = closure.get(descriptor);
                answer.addArg(valueDescriptor.getOuterValue());
            }
        }
        return answer;
    }

    private void generateConstInstance(PsiElement fun) {
        String classDescr = name.getDescriptor();
        cv.newField(fun, ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "$instance", classDescr, null, null);

        MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC | ACC_STATIC, "$getInstance", "()" + classDescr, null, new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, name.getInternalName(), "$instance", classDescr);
            mv.visitInsn(DUP);
            Label ret = new Label();
            mv.visitJumpInsn(IFNONNULL, ret);

            mv.visitInsn(POP);
            mv.visitTypeInsn(NEW, name.getInternalName());
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, name.getInternalName(), "<init>", "()V");
            mv.visitInsn(DUP);
            mv.visitFieldInsn(PUTSTATIC, name.getInternalName(), "$instance", classDescr);

            mv.visitLabel(ret);
            mv.visitInsn(ARETURN);
            FunctionCodegen.endVisit(mv, "$getInstance", fun);
        }
    }

    private Type generateBody(FunctionDescriptor funDescriptor, ClassBuilder cv, JetDeclarationWithBody body) {
        ClassDescriptor function =
                closureAnnotator.classDescriptorForFunctionDescriptor(funDescriptor);

        final CodegenContexts.ClosureContext closureContext = context.intoClosure(
                funDescriptor, function, name, this, typeMapper);
        FunctionCodegen fc = new FunctionCodegen(closureContext, cv, state);
        JvmMethodSignature jvmMethodSignature = invokeSignature(funDescriptor);
        fc.generateMethod(body, jvmMethodSignature, false, null, funDescriptor);
        return closureContext.outerWasUsed;
    }

    private void generateBridge(String className, FunctionDescriptor funDescriptor, JetExpression fun, ClassBuilder cv) {
        final JvmMethodSignature bridge = erasedInvokeSignature(funDescriptor);
        final Method delegate = invokeSignature(funDescriptor).getAsmMethod();

        if (bridge.getAsmMethod().getDescriptor().equals(delegate.getDescriptor())) {
            return;
        }

        final MethodVisitor mv =
                cv.newMethod(fun, ACC_PUBLIC | ACC_BRIDGE | ACC_VOLATILE, "invoke", bridge.getAsmMethod().getDescriptor(), null,
                             new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            InstructionAdapter iv = new InstructionAdapter(mv);

            iv.load(0, Type.getObjectType(className));

            final ReceiverDescriptor receiver = funDescriptor.getReceiverParameter();
            int count = 1;
            if (receiver.exists()) {
                StackValue.local(count, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
                StackValue.onStack(JetTypeMapper.TYPE_OBJECT)
                        .upcast(typeMapper.mapType(receiver.getType(), MapTypeMode.VALUE), iv);
                count++;
            }

            final List<ValueParameterDescriptor> params = funDescriptor.getValueParameters();
            for (ValueParameterDescriptor param : params) {
                StackValue.local(count, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
                StackValue.onStack(JetTypeMapper.TYPE_OBJECT)
                        .upcast(typeMapper.mapType(param.getType(), MapTypeMode.VALUE), iv);
                count++;
            }

            iv.invokevirtual(className, "invoke", delegate.getDescriptor());
            StackValue.onStack(delegate.getReturnType()).put(JetTypeMapper.TYPE_OBJECT, iv);

            iv.areturn(JetTypeMapper.TYPE_OBJECT);

            FunctionCodegen.endVisit(mv, "bridge", fun);
        }
    }

    private Method generateConstructor(JvmClassName funClass, JetExpression fun, FunctionDescriptor funDescriptor) {
        final ArrayList<Pair<String, Type>> args = new ArrayList<Pair<String, Type>>();
        boolean putFieldForMyself = calculateConstructorParameters(funDescriptor, args);

        final Type[] argTypes = nameAnTypeListToTypeArray(args);

        final Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        final MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC, "<init>", constructor.getDescriptor(), null, new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            iv.load(0, funClass.getAsmType());
            iv.invokespecial(funClass.getInternalName(), "<init>", "()V");

            int k = 1;
            for (int i = 0; i != argTypes.length; ++i) {
                StackValue.local(0, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
                final Pair<String, Type> nameAndType = args.get(i);
                final Type type = nameAndType.second;
                StackValue.local(k, type).put(type, iv);
                k += type.getSize();
                StackValue.field(type, name, nameAndType.first, false).store(type, iv);
            }

            if (putFieldForMyself) {
                Type type = name.getAsmType();
                String fieldName = "$" + funDescriptor.getName();
                iv.load(0, type);
                iv.dup();
                StackValue.field(type, name, fieldName, false).store(type, iv);
            }

            iv.visitInsn(RETURN);

            FunctionCodegen.endVisit(iv, "constructor", fun);
        }
        return constructor;
    }

    private boolean calculateConstructorParameters(FunctionDescriptor funDescriptor, List<Pair<String, Type>> args) {
        if (captureThis != null) {
            final Type type = typeMapper.mapType(context.getThisDescriptor().getDefaultType(), MapTypeMode.VALUE);
            args.add(new Pair<String, Type>("this$0", type));
        }
        if (captureReceiver != null) {
            args.add(new Pair<String, Type>("receiver$0", captureReceiver));
        }

        boolean putFieldForMyself = false;

        for (DeclarationDescriptor descriptor : closure.keySet()) {
            if (descriptor == funDescriptor) {
                putFieldForMyself = true;
            }
            else if (descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                final Type sharedVarType = typeMapper.getSharedVarType(descriptor);

                final Type type = sharedVarType != null
                                  ? sharedVarType
                                  : typeMapper.mapType(((VariableDescriptor) descriptor).getType(), MapTypeMode.VALUE);
                args.add(new Pair<String, Type>("$" + descriptor.getName().getName(), type));
            }
            else if (CodegenUtil.isNamedFun(descriptor, state.getBindingContext()) &&
                     descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
                final Type type = closureAnnotator
                        .classNameForAnonymousClass((JetElement) BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor))
                        .getAsmType();

                args.add(new Pair<String, Type>("$" + descriptor.getName().getName(), type));
            }
            else if (descriptor instanceof FunctionDescriptor) {
                assert captureReceiver != null;
            }
        }
        return putFieldForMyself;
    }

    private static Type[] nameAnTypeListToTypeArray(List<Pair<String, Type>> args) {
        final Type[] argTypes = new Type[args.size()];
        for (int i = 0; i != argTypes.length; ++i) {
            argTypes[i] = args.get(i).second;
        }
        return argTypes;
    }

    @NotNull
    public static JvmClassName getInternalClassName(FunctionDescriptor descriptor) {
        final int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter().exists()) {
            return JvmClassName.byInternalName("jet/ExtensionFunction" + paramCount);
        }
        else {
            return JvmClassName.byInternalName("jet/Function" + paramCount);
        }
    }

    private void appendType(SignatureWriter signatureWriter, JetType type, char variance) {
        signatureWriter.visitTypeArgument(variance);

        final Type rawRetType = typeMapper.mapType(type, MapTypeMode.TYPE_PARAMETER);
        signatureWriter.visitClassType(rawRetType.getInternalName());
        signatureWriter.visitEnd();
    }
}
