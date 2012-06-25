package com.silentmatt.dss.directive;

import com.silentmatt.dss.DeclarationList;
import com.silentmatt.dss.EvaluationState;
import com.silentmatt.dss.Immutable;
import com.silentmatt.dss.Rule;
import com.silentmatt.dss.css.CssFontFaceDirective;
import com.silentmatt.dss.css.CssRule;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Matthew Crumley
 */
@Immutable
public final class FontFaceDirective extends DeclarationDirective {
    public FontFaceDirective(DeclarationList declarations) {
        super(declarations);
    }

    @Override
    public String getName() {
        return "@font-face";
    }

    @Override
    public CssRule evaluate(EvaluationState state, List<Rule> container) throws IOException {
        return new CssFontFaceDirective(getDeclarationBlock().evaluateStyle(state, true).getCssDeclarations(state));
    }
}
