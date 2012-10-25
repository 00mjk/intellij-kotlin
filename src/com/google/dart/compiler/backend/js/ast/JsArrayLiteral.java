// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.intellij.util.SmartList;

import java.util.List;

/**
 * Represents a JavaScript expression for array literals.
 */
public final class JsArrayLiteral extends JsLiteral {
    private final List<JsExpression> expressions;

    public JsArrayLiteral() {
        expressions = new SmartList<JsExpression>();
    }

    public JsArrayLiteral(List<JsExpression> expressions) {
        this.expressions = expressions;
    }

    public List<JsExpression> getExpressions() {
        return expressions;
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visitArray(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        visitor.acceptWithInsertRemove(expressions);
    }
}
