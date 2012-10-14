package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

public class ChameleonJsExpression implements JsExpression {
    private JsExpression expression;

    public ChameleonJsExpression(JsExpression initialExpression) {
        expression = initialExpression;
    }

    public ChameleonJsExpression() {
    }

    public void resolve(JsExpression expression) {
        this.expression = expression;
    }

    @Override
    public boolean hasSideEffects() {
        return expression.hasSideEffects();
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return expression.isDefinitelyNotNull();
    }

    @Override
    public boolean isDefinitelyNull() {
        return expression.isDefinitelyNull();
    }

    @Override
    public boolean isLeaf() {
        return expression.isLeaf();
    }

    @Override
    public JsStatement makeStmt() {
        return expression.makeStmt();
    }

    @Override
    public NodeKind getKind() {
        return expression.getKind();
    }

    @Override
    public void traverse(JsVisitor visitor, JsContext context) {
        expression.traverse(visitor, context);
    }

    @Override
    public Object getSourceInfo() {
        return expression.getSourceInfo();
    }

    @Override
    public void setSourceInfo(Object info) {
        expression.setSourceInfo(info);
    }
}
