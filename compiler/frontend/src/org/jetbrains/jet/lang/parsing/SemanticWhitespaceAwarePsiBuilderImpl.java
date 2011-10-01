package org.jetbrains.jet.lang.parsing;


import com.intellij.lang.PsiBuilder;
import com.intellij.lang.impl.PsiBuilderAdapter;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Stack;

import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderImpl extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {
    private final Stack<Boolean> newlinesEnabled = new Stack<Boolean>();

    public SemanticWhitespaceAwarePsiBuilderImpl(final PsiBuilder delegate) {
        super(delegate);
        newlinesEnabled.push(true);
    }

    @Override
    public boolean newlineBeforeCurrentToken() {
        if (!newlinesEnabled.peek()) return false;
        if (eof()) return true;
        // TODO: maybe, memoize this somehow?
        for (int i = 1; i <= getCurrentOffset(); i++) {
            IElementType previousToken = rawLookup(-i);
            if (previousToken == JetTokens.BLOCK_COMMENT
                    || previousToken == JetTokens.DOC_COMMENT
                    || previousToken == JetTokens.EOL_COMMENT) {
                continue;
            }
            if (previousToken != TokenType.WHITE_SPACE) {
                break;
            }
            int previousTokenStart = rawTokenTypeStart(-i);
            int previousTokenEnd = rawTokenTypeStart(-i + 1);
            assert previousTokenStart >= 0;
            assert previousTokenEnd < getOriginalText().length();
            for (int j = previousTokenStart; j < previousTokenEnd; j++) {
                if (getOriginalText().charAt(j) == '\n') return true;
            }
        }
        return false;
    }

    @Override
    public void disableNewlines() {
        newlinesEnabled.push(false);
    }

    @Override
    public void enableNewlines() {
        newlinesEnabled.push(true);
    }

    @Override
    public void restoreNewlinesState() {
        assert newlinesEnabled.size() > 1;
        newlinesEnabled.pop();
    }

    private final TokenSet complexTokens = TokenSet.create(SAFE_ACCESS, ELVIS);
    private boolean joinComplexTokens = true;

    @Override
    public void enableJoiningComplexTokens() {
        joinComplexTokens = true;
    }

    @Override
    public void disableJoiningComplexTokens() {
        joinComplexTokens = false;
    }

    @Override
    public IElementType getTokenType() {
        if (!joinComplexTokens) return super.getTokenType();
        return getJoinedTokenType(super.getTokenType(), 1);
    }

    private IElementType getJoinedTokenType(IElementType rawTokenType, int rawLookupSteps) {
        if (rawTokenType == QUEST) {
            IElementType nextRawToken = rawLookup(rawLookupSteps);
            if (nextRawToken == DOT) return SAFE_ACCESS;
            if (nextRawToken == COLON) return ELVIS;
        }
        return rawTokenType;
    }

    @Override
    public void advanceLexer() {
        if (!joinComplexTokens) {
            super.advanceLexer();
            return;
        }
        IElementType tokenType = getTokenType();
        if (complexTokens.contains(tokenType)) {
            Marker mark = mark();
            super.advanceLexer();
            super.advanceLexer();
            mark.collapse(tokenType);
        }
        else {
            super.advanceLexer();
        }
    }

    @Override
    public String getTokenText() {
        if (!joinComplexTokens) return super.getTokenText();
        IElementType tokenType = getTokenType();
        if (complexTokens.contains(tokenType)) {
                if (tokenType == ELVIS) return "?:";
                if (tokenType == SAFE_ACCESS) return "?.";
            }
        return super.getTokenText();
    }

    @Override
    public IElementType lookAhead(int steps) {
        if (!joinComplexTokens) return super.lookAhead(steps);

        if (complexTokens.contains(getTokenType())) {
            return super.lookAhead(steps + 1);
        }
        return getJoinedTokenType(super.lookAhead(steps), 2);
    }
}
