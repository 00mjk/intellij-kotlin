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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

/**
 * @author Pavel Talanov
 */
public final class ReferenceAccessTranslator extends AccessTranslator {

    @NotNull
    /*package*/ static ReferenceAccessTranslator newInstance(@NotNull JetSimpleNameExpression expression,
                                                             @NotNull TranslationContext context) {
        return new ReferenceAccessTranslator(expression, context);
    }

    @NotNull
    private final JetSimpleNameExpression expression;

    private ReferenceAccessTranslator(@NotNull JetSimpleNameExpression expression,
                                      @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        //TODO: consider evaluating only once
        return ReferenceTranslator.translateSimpleName(expression, context());
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        //TODO: consider evaluating only once
        JsExpression reference = ReferenceTranslator.translateSimpleName(expression, context());
        assert reference instanceof JsNameRef;
        return AstUtil.newAssignment((JsNameRef) reference, toSetTo);
    }

}
