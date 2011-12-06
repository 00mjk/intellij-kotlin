package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class Inv implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        boolean nullable = expectedType.getSort() == Type.OBJECT;
        if(nullable) {
            expectedType = JetTypeMapper.unboxType(expectedType);
        }
        receiver.put(expectedType, v);
        if(expectedType == Type.LONG_TYPE) {
            v.lconst(-1L);
        }
        else {
            v.iconst(-1);
        }
        v.xor(expectedType);
        return StackValue.onStack(expectedType);
    }
}
