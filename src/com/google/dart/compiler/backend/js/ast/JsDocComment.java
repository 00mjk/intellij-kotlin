package com.google.dart.compiler.backend.js.ast;

import java.util.Collections;
import java.util.Map;

public class JsDocComment extends JsExpressionImpl {
    private final Map<String, Object> tags;

    public JsDocComment(Map<String, Object> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public JsDocComment(String tagName, JsNameRef tagValue) {
        tags = Collections.<String, Object>singletonMap(tagName, tagValue);
    }

    public JsDocComment(String tagName, String tagValue) {
        tags = Collections.<String, Object>singletonMap(tagName, tagValue);
    }

    @Override
    public NodeKind getKind() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visitDocComment(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
    }

    @Override
    public boolean hasSideEffects() {
        return false;
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return true;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }
}
