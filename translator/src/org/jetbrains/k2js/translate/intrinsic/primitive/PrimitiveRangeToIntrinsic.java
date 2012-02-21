/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.intrinsic.primitive;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBooleanLiteral;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNew;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class PrimitiveRangeToIntrinsic implements FunctionIntrinsic {

    @NotNull
    public static PrimitiveRangeToIntrinsic newInstance() {
        return new PrimitiveRangeToIntrinsic();
    }

    private PrimitiveRangeToIntrinsic() {
    }

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression rangeStart, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "RangeTo must have one argument.";
        JsExpression rangeEnd = arguments.get(0);
        JsBinaryOperation rangeSize = AstUtil.sum(AstUtil.subtract(rangeEnd, rangeStart),
                context.program().getNumberLiteral(1));
        //TODO: provide a way not to hard code this value
        JsNew numberRangeConstructorInvocation
                = new JsNew(AstUtil.newQualifiedNameRef("Kotlin.NumberRange"));
        //TODO: add tests and correct expression for reversed ranges.
        JsBooleanLiteral isRangeReversed = context.program().getFalseLiteral();
        numberRangeConstructorInvocation.setArguments(Arrays.asList(rangeStart, rangeSize, isRangeReversed));
        return numberRangeConstructorInvocation;
    }
}
