// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.Nullable;

public final class JsBinaryOperation extends JsExpressionImpl {
    private JsExpression arg1;
    private JsExpression arg2;
    private final JsBinaryOperator op;

    public JsBinaryOperation(JsBinaryOperator op) {
        this(op, null, null);
    }

    public JsBinaryOperation(JsBinaryOperator op, @Nullable JsExpression arg1, @Nullable JsExpression arg2) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public JsExpression getArg1() {
        return arg1;
    }

    public JsExpression getArg2() {
        return arg2;
    }

    public JsBinaryOperator getOperator() {
        return op;
    }

    @Override
    public boolean hasSideEffects() {
        return op.isAssignment() || arg1.hasSideEffects() || arg2.hasSideEffects();
    }

    @Override
    public boolean isDefinitelyNotNull() {
        // Precarious coding, but none of these can have null results.
        if (op.getPrecedence() > 5) {
            return true;
        }
        if (op == JsBinaryOperator.OR) {
            if (arg1 instanceof CanBooleanEval) {
                if (((CanBooleanEval) arg1).isBooleanTrue()) {
                    assert arg1.isDefinitelyNotNull();
                    return true;
                }
            }
        }
        // AND and OR can return nulls
        if (op.isAssignment()) {
            return op != JsBinaryOperator.ASG || arg2.isDefinitelyNotNull();
        }

        return op == JsBinaryOperator.COMMA && arg2.isDefinitelyNotNull();
    }

    @Override
    public boolean isDefinitelyNull() {
        return op == JsBinaryOperator.AND && arg1.isDefinitelyNull();
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visitBinaryExpression(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        if (op.isAssignment()) {
            visitor.acceptLvalue(arg1);
        }
        else {
            visitor.accept(arg1);
        }
        visitor.accept(arg2);
    }
}
