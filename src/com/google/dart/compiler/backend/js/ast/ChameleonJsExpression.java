package com.google.dart.compiler.backend.js.ast;

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
    public boolean isLeaf() {
        return expression.isLeaf();
    }

    @Override
    public JsStatement makeStmt() {
        return expression.makeStmt();
    }

    @Override
    public void accept(JsVisitor visitor, JsContext context) {
        expression.accept(visitor, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        expression.acceptChildren(visitor, context);
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
