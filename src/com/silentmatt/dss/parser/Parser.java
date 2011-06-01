package com.silentmatt.dss.parser;

import java.util.*;
import com.silentmatt.dss.*;
import com.silentmatt.dss.directive.*;
import com.silentmatt.dss.term.*;
import com.silentmatt.dss.bool.*;
import com.silentmatt.dss.calc.*;

class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _atref = 2;
	public static final int _integer = 3;
	public static final int _decimal = 4;
	public static final int _stringLit = 5;
	public static final int _url = 6;
	public static final int maxT = 76;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	Token t;    // last recognized token
	Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public ErrorReporter errors;

	public DSSDocument CSSDoc;

        boolean partOfHex(String value) {
            if (value.length() == 7) { return false; }
            if (value.length() + la.val.length() > 7) { return false; }
            List<String> hexes = Arrays.asList(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "a", "b", "c", "d", "e", "f" });
            for (int i = 0; i < la.val.length(); i++) {
                char c = la.val.charAt(i);
                if (!hexes.contains(String.valueOf(c))) {
                    return false;
                }
            }
            return true;
        }
        boolean isUnit() {
            if (la.kind != 1) { return false; }
            List<String> units = Arrays.asList(new String[] { "em", "ex", "px", "gd", "rem", "vw", "vh", "vm", "ch", "mm", "cm", "in", "pt", "pc", "deg", "grad", "rad", "turn", "ms", "s", "hz", "khz", "dpi", "dpcm" });            return units.contains(la.val);
        }
        boolean isDeclaration() {
            return la.kind == _ident && scanner.Peek().val.equals(":");
        }
        boolean isNestedSelector() {
            Token next = la;
            while (!next.val.equals(";") && !next.val.equals("}")) {
                if (next.val.equals("{")) {
                    return true;
                }
                next = scanner.Peek();
            }
            return false;
        }
        boolean endOfBlock() {
            return la.val.equals("}");
        }

/*------------------------------------------------------------------------*
 *----- SCANNER DESCRIPTION ----------------------------------------------*
 *------------------------------------------------------------------------*/



	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new ListErrorReporter();
	}

	void SynErr(int n) {
		if (errDist >= minErrDist) {
            errors.SynErr(la.line, la.col, n);
        }
		errDist = 0;
	}

	public void SemErr(String msg) {
		if (errDist >= minErrDist) {
            errors.SemErr(t.line, t.col, msg);
        }
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) {
            Get();
        }
        else {
            SynErr(n);
        }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) {
            Get();
        }
		else {
			SynErr(n);
			while (!StartOf(follow)) {
                Get();
            }
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) { return false; }
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void CSS3() {
		CSSDoc = new DSSDocument(); 
		while (la.kind == 7 || la.kind == 8) {
			if (la.kind == 7) {
				Get();
			} else {
				Get();
			}
		}
		while (StartOf(1)) {
			Rule rule = rule();
			CSSDoc.addRule(rule); 
			while (la.kind == 7 || la.kind == 8) {
				if (la.kind == 7) {
					Get();
				} else {
					Get();
				}
			}
		}
	}

	Rule  rule() {
		Rule  rule;
		rule = null; 
		if (StartOf(2)) {
			rule = ruleset();
		} else if (StartOf(3)) {
			rule = directive();
		} else SynErr(77);
		return rule;
	}

	RuleSet  ruleset() {
		RuleSet  rset;
		rset = new RuleSet();
		Selector sel;
		List<Declaration> decs;
		Rule dir;
		Combinator cb = Combinator.Descendant;
		
		sel = selector();
		rset.getSelectors().add(sel); 
		while (la.kind == 35) {
			Get();
			sel = selector();
			rset.getSelectors().add(sel); 
		}
		Expect(36);
		while (StartOf(4)) {
			if (!isNestedSelector()) {
				decs = multideclaration();
				rset.addDeclarations(decs);
				if (endOfBlock()) {
				    break;
				}
				
				Expect(42);
			} else if (la.kind == 40) {
				dir = classDirective();
				rset.addRule(dir); 
			} else if (la.kind == 47) {
				dir = defineDirective();
				rset.addRule(dir); 
			} else {
				cb = Combinator.Descendant; 
				if (la.kind == 44) {
					Get();
				}
				if (t.pos + t.val.length() == la.pos) {
				   cb = Combinator.None;
				}
				else {
				    cb = Combinator.Descendant;
				}
				
				if (la.kind == 43 || la.kind == 45 || la.kind == 46) {
					if (la.kind == 45) {
						Get();
						cb = Combinator.PrecededImmediatelyBy; 
					} else if (la.kind == 43) {
						Get();
						cb = Combinator.ChildOf; 
					} else {
						Get();
						cb = Combinator.PrecededBy; 
					}
				}
				RuleSet nested = ruleset();
				rset.addNestedRuleSet(cb, nested); 
			}
		}
		Expect(37);
		return rset;
	}

	Rule  directive() {
		Rule  dir;
		dir = null; 
		switch (la.kind) {
		case 38: {
			dir = ifDirective();
			break;
		}
		case 34: {
			dir = mediaDirective();
			break;
		}
		case 40: {
			dir = classDirective();
			break;
		}
		case 47: {
			dir = defineDirective();
			break;
		}
		case 48: {
			dir = fontFaceDirective();
			break;
		}
		case 50: {
			dir = importDirective();
			break;
		}
		case 51: {
			dir = includeDirective();
			break;
		}
		case 52: {
			dir = charsetDirective();
			break;
		}
		case 49: {
			dir = pageDirective();
			break;
		}
		case 53: {
			dir = namespaceDirective();
			break;
		}
		case 54: {
			dir = genericDirective();
			break;
		}
		default: SynErr(78); break;
		}
		return dir;
	}

	String  QuotedString() {
		String  qs;
		Expect(5);
		qs = t.val; 
		return qs;
	}

	String  URI() {
		String  url;
		Expect(6);
		url = t.val.substring(4, t.val.length() - 1); 
		return url;
	}

	Medium  medium() {
		Medium  m;
		m = Medium.all; 
		switch (la.kind) {
		case 9: {
			Get();
			m = Medium.all; 
			break;
		}
		case 10: {
			Get();
			m = Medium.aural; 
			break;
		}
		case 11: {
			Get();
			m = Medium.braille; 
			break;
		}
		case 12: {
			Get();
			m = Medium.embossed; 
			break;
		}
		case 13: {
			Get();
			m = Medium.handheld; 
			break;
		}
		case 14: {
			Get();
			m = Medium.print; 
			break;
		}
		case 15: {
			Get();
			m = Medium.projection; 
			break;
		}
		case 16: {
			Get();
			m = Medium.screen; 
			break;
		}
		case 17: {
			Get();
			m = Medium.tty; 
			break;
		}
		case 18: {
			Get();
			m = Medium.tv; 
			break;
		}
		default: SynErr(79); break;
		}
		return m;
	}

	String  identity() {
		String  ident;
		switch (la.kind) {
		case 1: {
			Get();
			break;
		}
		case 19: {
			Get();
			break;
		}
		case 20: {
			Get();
			break;
		}
		case 9: {
			Get();
			break;
		}
		case 10: {
			Get();
			break;
		}
		case 11: {
			Get();
			break;
		}
		case 12: {
			Get();
			break;
		}
		case 13: {
			Get();
			break;
		}
		case 14: {
			Get();
			break;
		}
		case 15: {
			Get();
			break;
		}
		case 16: {
			Get();
			break;
		}
		case 17: {
			Get();
			break;
		}
		case 18: {
			Get();
			break;
		}
		case 21: {
			Get();
			break;
		}
		case 22: {
			Get();
			break;
		}
		case 23: {
			Get();
			break;
		}
		case 24: {
			Get();
			break;
		}
		case 25: {
			Get();
			break;
		}
		case 26: {
			Get();
			break;
		}
		case 27: {
			Get();
			break;
		}
		case 28: {
			Get();
			break;
		}
		case 29: {
			Get();
			break;
		}
		case 30: {
			Get();
			break;
		}
		default: SynErr(80); break;
		}
		ident = t.val; 
		return ident;
	}

	String  atReference() {
		String  ident;
		Expect(2);
		ident = t.val.substring(1); 
		return ident;
	}

	MediaQuery  mediaQuery() {
		MediaQuery  query;
		String expr, q = ""; 
		if (StartOf(5)) {
			if (la.kind == 24 || la.kind == 25) {
				if (la.kind == 24) {
					Get();
					q = "only "; 
				} else {
					Get();
					q = "not "; 
				}
			}
			String type = identity();
			q += type; 
			while (la.kind == 26) {
				Get();
				expr = mediaExpression();
				q += " and " + expr; 
			}
		} else if (la.kind == 31) {
			expr = mediaExpression();
			q += expr; 
			while (la.kind == 26) {
				Get();
				expr = mediaExpression();
				q += " and " + expr; 
			}
		} else SynErr(81);
		query = new MediaQuery(q); 
		return query;
	}

	String  mediaExpression() {
		String  expr;
		Expect(31);
		String e = identity();
		expr = e; 
		if (la.kind == 32) {
			Get();
			Expression exp = expr();
			expr += ":" + exp; 
		}
		Expect(33);
		expr = "(" + expr + ")"; 
		return expr;
	}

	Expression  expr() {
		Expression  exp;
		exp = new Expression();
		Character sep = null;
		Term trm;
		
		trm = term();
		exp.getTerms().add(trm); 
		while (StartOf(6)) {
			if (la.kind == 35 || la.kind == 59 || la.kind == 69) {
				if (la.kind == 69) {
					Get();
					sep = '/'; 
				} else if (la.kind == 35) {
					Get();
					sep = ','; 
				} else {
					Get();
					sep = '='; 
				}
			}
			trm = term();
			if (sep != null) { trm.setSeperator(sep); }
			exp.getTerms().add(trm);
			sep = null;
			
		}
		return exp;
	}

	MediaDirective  mediaDirective() {
		MediaDirective  mdir;
		List<MediaQuery> media = new ArrayList<MediaQuery>();
		List<Rule> rules = new ArrayList<Rule>();
		
		Expect(34);
		MediaQuery m = mediaQuery();
		media.add(m); 
		while (la.kind == 35) {
			Get();
			m = mediaQuery();
			media.add(m); 
		}
		Expect(36);
		while (StartOf(7)) {
			if (StartOf(2)) {
				RuleSet rset = ruleset();
				rules.add(rset); 
			} else if (la.kind == 40) {
				ClassDirective cdir = classDirective();
				rules.add(cdir); 
			} else if (la.kind == 47) {
				DefineDirective ddir = defineDirective();
				rules.add(ddir); 
			} else {
				IncludeDirective idir = includeDirective();
				rules.add(idir); 
			}
		}
		Expect(37);
		mdir = new MediaDirective(media, rules); 
		return mdir;
	}

	ClassDirective  classDirective() {
		ClassDirective  dir;
		String ident;
		List<Declaration> parameters = new ArrayList<Declaration>();
		List<Declaration> declarations = new ArrayList<Declaration>();
		List<Declaration> mdecs;
		List<NestedRuleSet> nested = new ArrayList<NestedRuleSet>();
		Declaration param;
		Combinator cb;
		boolean global = false;
		
		Expect(40);
		ident = identity();
		if (la.kind == 41) {
			Get();
			if (StartOf(5)) {
				param = parameter();
				parameters.add(param); 
				while (la.kind == 42) {
					Get();
					param = parameter();
					parameters.add(param); 
				}
			}
			Expect(43);
		}
		if (la.kind == 22) {
			Get();
			global = true; 
		}
		Expect(36);
		while (StartOf(8)) {
			if (!isNestedSelector()) {
				mdecs = multideclaration();
				declarations.addAll(mdecs);
				if (endOfBlock()) {
				    break;
				}
				
				Expect(42);
			} else {
				cb = Combinator.Descendant; 
				if (la.kind == 44) {
					Get();
				}
				if (t.pos + t.val.length() == la.pos) {
				   cb = Combinator.None;
				}
				else {
				    cb = Combinator.Descendant;
				}
				
				if (la.kind == 43 || la.kind == 45 || la.kind == 46) {
					if (la.kind == 45) {
						Get();
						cb = Combinator.PrecededImmediatelyBy; 
					} else if (la.kind == 43) {
						Get();
						cb = Combinator.ChildOf; 
					} else {
						Get();
						cb = Combinator.PrecededBy; 
					}
				}
				RuleSet nest = ruleset();
				nested.add(new NestedRuleSet(cb, nest)); 
			}
		}
		Expect(37);
		dir = new ClassDirective(ident, parameters, global, declarations, nested); 
		return dir;
	}

	DefineDirective  defineDirective() {
		DefineDirective  dir;
		List<Declaration> declarations = new ArrayList<Declaration>();
		boolean global = false;
		List<Declaration> mdecs;
		
		Expect(47);
		if (la.kind == 22) {
			Get();
			global = true; 
		}
		Expect(36);
		while (StartOf(5)) {
			mdecs = multideclaration();
			declarations.addAll(mdecs);
			if (endOfBlock()) {
			    break;
			}
			
			Expect(42);
		}
		Expect(37);
		dir = new DefineDirective(declarations, global); 
		return dir;
	}

	IncludeDirective  includeDirective() {
		IncludeDirective  dir;
		dir = null;
		boolean literal = false;
		
		Expect(51);
		if (la.kind == 27) {
			Get();
			literal = true; 
		}
		if (la.kind == 6) {
			String url = URI();
			dir = new IncludeDirective(new UrlTerm(url), literal); 
		} else if (la.kind == 5) {
			String url = QuotedString();
			dir = new IncludeDirective(new UrlTerm(url.substring(1, url.length()-1)), literal); 
		} else SynErr(82);
		Expect(42);
		return dir;
	}

	IfDirective  ifDirective() {
		IfDirective  idir;
		BooleanExpression expr;
		List<Rule> ifrules = new ArrayList<Rule>();
		List<Rule> elserules = null;
		
		Expect(38);
		expr = booleanExpression();
		Expect(36);
		while (StartOf(1)) {
			Rule rule = rule();
			ifrules.add(rule); 
		}
		Expect(37);
		if (la.kind == 39) {
			Get();
			elserules = new ArrayList<Rule>(); 
			Expect(36);
			while (StartOf(1)) {
				Rule rule = rule();
				elserules.add(rule); 
			}
			Expect(37);
		}
		idir = new IfDirective(expr, ifrules, elserules); 
		return idir;
	}

	BooleanExpression  booleanExpression() {
		BooleanExpression  expr;
		expr = orExpression();
		return expr;
	}

	Declaration  parameter() {
		Declaration  dec;
		dec = new Declaration(); 
		String ident = identity();
		dec.setName(ident); 
		if (la.kind == 32) {
			Get();
			Expression exp = expr();
			dec.setExpression(exp); 
		}
		return dec;
	}

	List<Declaration>  multideclaration() {
		List<Declaration>  decs;
		decs = new ArrayList<Declaration>();
		Declaration first = new Declaration();
		
		String ident = identity();
		first.setName(ident); decs.add(first); 
		while (la.kind == 35) {
			Get();
			String ident2 = identity();
			decs.add(new Declaration(ident2, new PropertyTerm(first.getName()).toExpression())); 
		}
		Expect(32);
		Expression exp = expr();
		first.setExpression(exp); 
		if (la.kind == 68) {
			Get();
			Expect(21);
			for (Declaration dec : decs) {
			   dec.setImportant(true);
			}
			
		}
		return decs;
	}

	FontFaceDirective  fontFaceDirective() {
		FontFaceDirective  dir;
		List<Declaration> declarations = new ArrayList<Declaration>();
		List<Declaration> mdecs;
		
		Expect(48);
		Expect(36);
		while (StartOf(5)) {
			mdecs = multideclaration();
			declarations.addAll(mdecs);
			if (endOfBlock()) {
			    break;
			}
			
			Expect(42);
		}
		Expect(37);
		dir = new FontFaceDirective(declarations); 
		return dir;
	}

	PageDirective  pageDirective() {
		PageDirective  dir;
		List<Declaration> declarations = new ArrayList<Declaration>();
		SimpleSelector ss = null;
		List<Declaration> mdecs;
		
		Expect(49);
		if (la.kind == 32) {
			String psd = pseudo();
			ss = new SimpleSelector();
			ss.setPseudo(psd);
			
		}
		Expect(36);
		while (StartOf(5)) {
			mdecs = multideclaration();
			declarations.addAll(mdecs);
			if (endOfBlock()) {
			    break;
			}
			
			Expect(42);
		}
		Expect(37);
		dir = new PageDirective(ss, declarations); 
		return dir;
	}

	String  pseudo() {
		String  pseudo;
		StringBuilder sb = new StringBuilder();
		boolean haveOpenParen = false;
		
		Expect(32);
		if (la.kind == 32) {
			Get();
			sb.append(":"); 
		}
		if (StartOf(5)) {
			String ident = identity();
			sb.append(ident); 
		} else if (la.kind == 25) {
			Get();
			sb.append("not"); 
		} else SynErr(83);
		if (la.kind == 31) {
			Get();
			if (StartOf(9)) {
				if (la.kind == 45 || la.kind == 66) {
					if (la.kind == 45) {
						Get();
						sb.append("(").append(t.val);
						haveOpenParen = true; 
					} else {
						Get();
						sb.append("(").append(t.val);
						haveOpenParen = true; 
					}
				}
				if (la.kind == 3) {
					Get();
					if (!haveOpenParen) {
					   sb.append("(");
					   haveOpenParen = true;
					}
					sb.append(t.val); 
				}
				if (la.kind == 19 || la.kind == 67) {
					if (la.kind == 19) {
						Get();
					} else {
						Get();
					}
					if (!haveOpenParen) {
					   sb.append("(");
					   haveOpenParen = true;
					}
					sb.append(t.val); 
					if (la.kind == 45 || la.kind == 66) {
						if (la.kind == 45) {
							Get();
						} else {
							Get();
						}
						sb.append(t.val); 
						Expect(3);
						sb.append(t.val); 
					}
				}
				sb.append(")"); 
			} else if (StartOf(2)) {
				SimpleSelector ss = simpleselector();
				sb.append("(").append(ss).append(")"); 
			} else SynErr(84);
			Expect(33);
		}
		pseudo = sb.toString(); 
		return pseudo;
	}

	ImportDirective  importDirective() {
		ImportDirective  dir;
		MediaQuery m = new MediaQuery("all");
		UrlTerm trm;
		
		Expect(50);
		String url = URI();
		trm = new UrlTerm(url); 
		if (StartOf(10)) {
			m = mediaQuery();
		}
		Expect(42);
		dir = new ImportDirective(trm, m); 
		return dir;
	}

	CharsetDirective  charsetDirective() {
		CharsetDirective  dir;
		Expect(52);
		Term trm = term();
		dir = new CharsetDirective(trm); 
		Expect(42);
		return dir;
	}

	Term  term() {
		Term  trm;
		String val = "";
		Expression exp;
		String ident;
		CalcExpression expression;
		trm = null;
		
		if (la.kind == 5) {
			val = QuotedString();
			trm = new StringTerm(val); 
		} else if (la.kind == 6) {
			val = URI();
			trm = new UrlTerm(val); 
		} else if (la.kind == 28) {
			Get();
			Expect(31);
			ident = identity();
			Expect(33);
			trm = new ConstTerm(ident); 
		} else if (la.kind == 29) {
			Get();
			Expect(31);
			ident = identity();
			Expect(33);
			trm = new ParamTerm(ident); 
		} else if (la.kind == 73) {
			Get();
			Expect(31);
			ident = identity();
			Expect(33);
			trm = new PropertyTerm(ident); 
		} else if (la.kind == 2) {
			ident = atReference();
			trm = new AtReferenceTerm(ident); 
		} else if (la.kind == 30) {
			Get();
			Expect(31);
			Selector s = selector();
			Expect(33);
			trm = new RuleSetClassReferenceTerm(s); 
		} else if (la.kind == 74) {
			Get();
			ident = identity();
			trm = new UnicodeTerm("U\\" + ident); 
		} else if (la.kind == 56) {
			val = HexValue();
			trm = new HexTerm(val); 
		} else if (la.kind == 23 || la.kind == 58) {
			expression = calculation();
			trm = new CalculationTerm(expression); 
		} else if (StartOf(5)) {
			ident = identity();
			trm = new StringTerm(ident); 
			while (la.kind == 32 || la.kind == 57) {
				if (la.kind == 32) {
					Get();
					((StringTerm) trm).setValue(trm.toString() + t.val); 
					if (la.kind == 32) {
						Get();
						((StringTerm) trm).setValue(trm.toString() + t.val); 
					}
					ident = identity();
					((StringTerm) trm).setValue(trm.toString() + ident); 
				} else {
					Get();
					((StringTerm) trm).setValue(trm.toString() + t.val); 
					ident = identity();
					((StringTerm) trm).setValue(trm.toString() + ident); 
				}
			}
			if (la.kind == 31 || la.kind == 41) {
				if (la.kind == 31) {
					Get();
					exp = expr();
					FunctionTerm func = new FunctionTerm();
					func.setName(trm.toString());
					func.setExpression(exp);
					trm = func;
					
					Expect(33);
				} else {
					Get();
					ClassReferenceTerm cls = new ClassReferenceTerm(trm.toString());
					Declaration dec;
					trm = cls;
					
					if (StartOf(11)) {
						if (isDeclaration()) {
							dec = declaration();
							cls.addArgument(dec); 
						} else {
							Expression arg = expr();
							cls.addArgument(new Declaration("", arg)); 
						}
						while (la.kind == 42) {
							Get();
							if (isDeclaration()) {
								dec = declaration();
								cls.addArgument(dec); 
							} else if (StartOf(11)) {
								Expression arg = expr();
								cls.addArgument(new Declaration("", arg)); 
							} else SynErr(85);
						}
					}
					Expect(43);
				}
			}
		} else if (StartOf(12)) {
			if (la.kind == 45 || la.kind == 66) {
				if (la.kind == 66) {
					Get();
					val = "-"; 
				} else {
					Get();
					val = "+"; 
				}
			}
			if (la.kind == 3) {
				Get();
			} else if (la.kind == 4) {
				Get();
			} else SynErr(86);
			val += t.val; trm = new NumberTerm(Double.parseDouble(val)); 
			if (endOfBlock()) {
				((NumberTerm) trm).setValue(Double.parseDouble(val)); 
			} else if (StartOf(13)) {
				if (la.val.equalsIgnoreCase("n")) {
					Expect(19);
					val += t.val; 
					if (la.kind == 45 || la.kind == 66) {
						if (la.kind == 45) {
							Get();
							val += '+'; 
						} else {
							Get();
							val += '-'; 
						}
						Expect(3);
						val += t.val; 
					}
					trm = new StringTerm(val); val = ""; 
				} else if (la.kind == 75) {
					Get();
					((NumberTerm) trm).setUnit(Unit.Percent); 
				} else if (StartOf(14)) {
					if (isUnit()) {
						ident = identity();
						try {
						   // TODO: What if trm isn't a NumberTerm?
						   ((NumberTerm) trm).setUnit(Unit.parse(ident));
						} catch (IllegalArgumentException ex) {
						    errors.SemErr(t.line, t.col, "Unrecognized unit '" + ident + "'");
						}
						
					}
				} else SynErr(87);
				if (trm instanceof NumberTerm) {
				   ((NumberTerm) trm).setValue(Double.parseDouble(val));
				}
				else if (trm instanceof StringTerm) {
				    StringTerm strTrm = (StringTerm) trm;
				    strTrm.setValue(strTrm.getValue() + val);
				}
				
			} else SynErr(88);
		} else SynErr(89);
		return trm;
	}

	NamespaceDirective  namespaceDirective() {
		NamespaceDirective  dir;
		String ident = null;
		String url = null;
		
		Expect(53);
		if (StartOf(5)) {
			ident = identity();
		}
		if (la.kind == 6) {
			url = URI();
		} else if (la.kind == 5) {
			url = QuotedString();
		} else SynErr(90);
		Expect(42);
		dir = new NamespaceDirective(ident, new UrlTerm(url)); 
		return dir;
	}

	GenericDirective  genericDirective() {
		GenericDirective  dir;
		Expect(54);
		String ident = identity();
		dir = new GenericDirective();
		dir.setName("@" + ident);
		
		if (StartOf(11)) {
			if (StartOf(11)) {
				Expression exp = expr();
				dir.setExpression(exp); 
			} else {
				Medium m = medium();
				dir.addMedium(m); 
			}
		}
		if (la.kind == 36) {
			Get();
			while (StartOf(1)) {
				if (StartOf(5)) {
					Declaration dec = declaration();
					dir.addDeclaration(dec);
					if (endOfBlock()) {
					    break;
					}
					
					Expect(42);
				} else if (StartOf(2)) {
					RuleSet rset = ruleset();
					dir.addRule(rset); 
				} else {
					Rule dr = directive();
					dir.addRule(dr); 
				}
			}
			Expect(37);
		} else if (la.kind == 42) {
			Get();
		} else SynErr(91);
		return dir;
	}

	Declaration  declaration() {
		Declaration  dec;
		dec = new Declaration(); 
		String ident = identity();
		dec.setName(ident); 
		Expect(32);
		Expression exp = expr();
		dec.setExpression(exp); 
		if (la.kind == 68) {
			Get();
			Expect(21);
			dec.setImportant(true); 
		}
		return dec;
	}

	Selector  selector() {
		Selector  sel;
		sel = new Selector();
		SimpleSelector ss;
		Combinator cb = Combinator.Descendant;
		
		ss = simpleselector();
		sel.getSimpleSelectors().add(ss); 
		while (StartOf(15)) {
			if (la.kind == 43 || la.kind == 45 || la.kind == 46) {
				if (la.kind == 45) {
					Get();
					cb = Combinator.PrecededImmediatelyBy; 
				} else if (la.kind == 43) {
					Get();
					cb = Combinator.ChildOf; 
				} else {
					Get();
					cb = Combinator.PrecededBy; 
				}
			}
			ss = simpleselector();
			ss.setCombinator(cb);
			sel.getSimpleSelectors().add(ss);
			cb = Combinator.Descendant;
			
		}
		return sel;
	}

	SimpleSelector  simpleselector() {
		SimpleSelector  ss;
		ss = new SimpleSelector();
		SimpleSelector parent = ss;
		String ident;
		
		if (StartOf(5)) {
			ident = identity();
			ss.setElementName(ident); 
		} else if (la.kind == 55) {
			Get();
			ss.setElementName("*"); 
		} else if (StartOf(16)) {
			if (la.kind == 56) {
				Get();
				ident = identity();
				ss.setID(ident); 
			} else if (la.kind == 57) {
				Get();
				ident = identity();
				ss.setClassName(ident); 
			} else if (la.kind == 58) {
				Attribute atb = attrib();
				ss.setAttribute(atb); 
			} else {
				String psd = pseudo();
				ss.setPseudo(psd); 
			}
		} else SynErr(92);
		while (StartOf(16)) {
			if (t.pos + t.val.length() < la.pos) {
			   break;
			}
			SimpleSelector child = new SimpleSelector();
			
			if (la.kind == 56) {
				Get();
				ident = identity();
				child.setID(ident); 
			} else if (la.kind == 57) {
				Get();
				ident = identity();
				child.setClassName(ident); 
			} else if (la.kind == 58) {
				Attribute atb = attrib();
				child.setAttribute(atb); 
			} else {
				String psd = pseudo();
				child.setPseudo(psd); 
			}
			parent.setChild(child);
			parent = child;
			
		}
		return ss;
	}

	Attribute  attrib() {
		Attribute  atb;
		atb = new Attribute();
		String quote;
		String ident;
		
		Expect(58);
		ident = identity();
		atb.setOperand(ident); 
		if (StartOf(17)) {
			switch (la.kind) {
			case 59: {
				Get();
				atb.setOperator(AttributeOperator.Equals); 
				break;
			}
			case 60: {
				Get();
				atb.setOperator(AttributeOperator.InList); 
				break;
			}
			case 61: {
				Get();
				atb.setOperator(AttributeOperator.Hyphenated); 
				break;
			}
			case 62: {
				Get();
				atb.setOperator(AttributeOperator.EndsWith); 
				break;
			}
			case 63: {
				Get();
				atb.setOperator(AttributeOperator.BeginsWith); 
				break;
			}
			case 64: {
				Get();
				atb.setOperator(AttributeOperator.Contains); 
				break;
			}
			}
			if (StartOf(5)) {
				ident = identity();
				atb.setValue(ident); 
			} else if (la.kind == 5) {
				quote = QuotedString();
				atb.setValue(quote); 
			} else if (la.kind == 3 || la.kind == 4) {
				if (la.kind == 3) {
					Get();
				} else {
					Get();
				}
				atb.setValue(t.val); 
			} else SynErr(93);
		}
		Expect(65);
		return atb;
	}

	BooleanExpression  orExpression() {
		BooleanExpression  expr;
		BooleanExpression left, right; BooleanOperation op; 
		left = andExpression();
		expr = left; 
		while (la.kind == 70 || la.kind == 71) {
			op = orop();
			right = andExpression();
			expr = new BinaryBooleanExpression(op, expr, right); 
		}
		return expr;
	}

	BooleanOperation  orop() {
		BooleanOperation  op;
		op = null; 
		if (la.kind == 70) {
			Get();
			op = BooleanOperation.OR; 
		} else if (la.kind == 71) {
			Get();
			op = BooleanOperation.XOR; 
		} else SynErr(94);
		return op;
	}

	BooleanExpression  andExpression() {
		BooleanExpression  expr;
		BooleanExpression left, right;
		left = notExpression();
		expr = left; 
		while (la.kind == 72) {
			Get();
			right = notExpression();
			expr = new BinaryBooleanExpression(BooleanOperation.AND, expr, right); 
		}
		return expr;
	}

	BooleanExpression  notExpression() {
		BooleanExpression  expr;
		BooleanExpression exp; expr = null; 
		if (StartOf(18)) {
			exp = primaryBooleanExpression();
			expr = exp; 
		} else if (la.kind == 68) {
			Get();
			exp = notExpression();
			expr = new NotExpression(exp); 
		} else SynErr(95);
		return expr;
	}

	BooleanExpression  primaryBooleanExpression() {
		BooleanExpression  expr;
		expr = null; Term trm; BooleanExpression exp; 
		if (la.kind == 31) {
			Get();
			exp = booleanExpression();
			Expect(33);
			expr = exp; 
		} else if (StartOf(11)) {
			trm = term();
			expr = new TermBooleanExpression(trm); 
		} else SynErr(96);
		return expr;
	}

	CalcExpression  lengthExpression() {
		CalcExpression  expr;
		expr = additiveExpression();
		return expr;
	}

	CalcExpression  additiveExpression() {
		CalcExpression  expr;
		CalcExpression left, right; Operation op; 
		left = multiplicativeExpression();
		expr = left; 
		while (la.kind == 45 || la.kind == 66) {
			op = addop();
			right = multiplicativeExpression();
			expr = new BinaryExpression(op, expr, right); 
		}
		return expr;
	}

	Operation  addop() {
		Operation  op;
		op = null; 
		if (la.kind == 45) {
			Get();
			op = Operation.Add; 
		} else if (la.kind == 66) {
			Get();
			op = Operation.Subtract; 
		} else SynErr(97);
		return op;
	}

	Operation  mulop() {
		Operation  op;
		op = null; 
		if (la.kind == 55) {
			Get();
			op = Operation.Multiply; 
		} else if (la.kind == 69) {
			Get();
			op = Operation.Divide; 
		} else SynErr(98);
		return op;
	}

	CalcExpression  multiplicativeExpression() {
		CalcExpression  expr;
		CalcExpression left, right; Operation op; 
		left = termExpression();
		expr = left; 
		while (la.kind == 55 || la.kind == 69) {
			op = mulop();
			right = termExpression();
			expr = new BinaryExpression(op, expr, right); 
		}
		return expr;
	}

	CalcExpression  termExpression() {
		CalcExpression  expr;
		expr = null; Term trm; CalcExpression exp; 
		if (la.kind == 66) {
			Get();
			exp = termExpression();
			expr = new NegationExpression(exp); 
		} else if (la.kind == 31) {
			Get();
			exp = lengthExpression();
			Expect(33);
			expr = exp; 
		} else if (StartOf(11)) {
			trm = term();
			expr = new TermExpression(trm); 
		} else SynErr(99);
		return expr;
	}

	CalcExpression  calculation() {
		CalcExpression  expr;
		expr = null; 
		if (la.kind == 23) {
			Get();
			Expect(31);
			expr = lengthExpression();
			Expect(33);
		} else if (la.kind == 58) {
			Get();
			expr = lengthExpression();
			Expect(65);
		} else SynErr(100);
		return expr;
	}

	String  HexValue() {
		String  val;
		StringBuilder sb = new StringBuilder();
		boolean found = false;
		
		Expect(56);
		sb.append(t.val); 
		if (la.kind == 3) {
			Get();
			sb.append(t.val); 
		} else if (la.kind == 1) {
			Get();
			sb.append(t.val); found = true; 
		} else SynErr(101);
		if (!found && partOfHex(sb.toString())) {
			Expect(1);
			sb.append(t.val); 
		}
		val = sb.toString(); 
		return val;
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		CSS3();

		Expect(0);
	}

	private static final boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, T,x,T,x, x,x,T,x, T,x,x,x, x,x,x,T, T,T,T,T, T,T,T,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,x, x,x,T,x, T,x,x,x, x,x,x,T, T,T,T,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, T,x,x,x, x,x,x,x, T,x,x,T, T,T,T,T, x,x,x,x, x,x,x,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,T,T, T,T,T,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,x,x,T, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, T,x,T,T, x,x,x,x, x,x,T,x, x,T,x,x, x,T,T,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, T,x,x,x, x,x,x,x, T,x,x,x, x,x,x,T, x,x,x,T, x,x,x,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, T,x,x,x, x,x,x,x, x,x,x,T, T,T,T,x, x,x,x,x, x,x,x,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,x,x,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,T, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,T,T, T,T,T,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, T,x,T,x, x,x,x,x, x,x,T,x, x,x,x,x, x,T,T,x, x,x},
		{x,x,x,T, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,T,T, T,T,T,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,T,x,T, T,x,x,x, x,x,T,T, x,T,x,x, x,x,x,x, x,x,x,T, T,x,T,T, x,x,x,x, x,T,T,x, T,T,T,T, T,T,T,T, x,x},
		{x,T,T,T, T,T,T,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,T,x,T, T,x,x,x, x,x,T,T, x,T,x,x, x,x,x,x, x,x,x,T, T,x,T,T, x,x,x,x, x,T,T,x, T,T,T,T, T,T,T,x, x,x},
		{x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, T,x,x,x, x,x,x,x, x,x,x,T, x,T,T,x, x,x,x,x, x,x,x,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, T,T,T,T, T,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,T,T, T,T,T,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, T,x,T,x, x,x,x,x, x,x,T,x, x,x,x,x, x,T,T,x, x,x}

	};

	public static final String getErrorMessage(int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "atref expected"; break;
			case 3: s = "integer expected"; break;
			case 4: s = "decimal expected"; break;
			case 5: s = "stringLit expected"; break;
			case 6: s = "url expected"; break;
			case 7: s = "\"<!--\" expected"; break;
			case 8: s = "\"-->\" expected"; break;
			case 9: s = "\"all\" expected"; break;
			case 10: s = "\"aural\" expected"; break;
			case 11: s = "\"braille\" expected"; break;
			case 12: s = "\"embossed\" expected"; break;
			case 13: s = "\"handheld\" expected"; break;
			case 14: s = "\"print\" expected"; break;
			case 15: s = "\"projection\" expected"; break;
			case 16: s = "\"screen\" expected"; break;
			case 17: s = "\"tty\" expected"; break;
			case 18: s = "\"tv\" expected"; break;
			case 19: s = "\"n\" expected"; break;
			case 20: s = "\"url\" expected"; break;
			case 21: s = "\"important\" expected"; break;
			case 22: s = "\"global\" expected"; break;
			case 23: s = "\"calc\" expected"; break;
			case 24: s = "\"only\" expected"; break;
			case 25: s = "\"not\" expected"; break;
			case 26: s = "\"and\" expected"; break;
			case 27: s = "\"literal\" expected"; break;
			case 28: s = "\"const\" expected"; break;
			case 29: s = "\"param\" expected"; break;
			case 30: s = "\"ruleset\" expected"; break;
			case 31: s = "\"(\" expected"; break;
			case 32: s = "\":\" expected"; break;
			case 33: s = "\")\" expected"; break;
			case 34: s = "\"@media\" expected"; break;
			case 35: s = "\",\" expected"; break;
			case 36: s = "\"{\" expected"; break;
			case 37: s = "\"}\" expected"; break;
			case 38: s = "\"@if\" expected"; break;
			case 39: s = "\"@else\" expected"; break;
			case 40: s = "\"@class\" expected"; break;
			case 41: s = "\"<\" expected"; break;
			case 42: s = "\";\" expected"; break;
			case 43: s = "\">\" expected"; break;
			case 44: s = "\"&\" expected"; break;
			case 45: s = "\"+\" expected"; break;
			case 46: s = "\"~\" expected"; break;
			case 47: s = "\"@define\" expected"; break;
			case 48: s = "\"@font-face\" expected"; break;
			case 49: s = "\"@page\" expected"; break;
			case 50: s = "\"@import\" expected"; break;
			case 51: s = "\"@include\" expected"; break;
			case 52: s = "\"@charset\" expected"; break;
			case 53: s = "\"@namespace\" expected"; break;
			case 54: s = "\"@\" expected"; break;
			case 55: s = "\"*\" expected"; break;
			case 56: s = "\"#\" expected"; break;
			case 57: s = "\".\" expected"; break;
			case 58: s = "\"[\" expected"; break;
			case 59: s = "\"=\" expected"; break;
			case 60: s = "\"~=\" expected"; break;
			case 61: s = "\"|=\" expected"; break;
			case 62: s = "\"$=\" expected"; break;
			case 63: s = "\"^=\" expected"; break;
			case 64: s = "\"*=\" expected"; break;
			case 65: s = "\"]\" expected"; break;
			case 66: s = "\"-\" expected"; break;
			case 67: s = "\"-n\" expected"; break;
			case 68: s = "\"!\" expected"; break;
			case 69: s = "\"/\" expected"; break;
			case 70: s = "\"||\" expected"; break;
			case 71: s = "\"^\" expected"; break;
			case 72: s = "\"&&\" expected"; break;
			case 73: s = "\"prop\" expected"; break;
			case 74: s = "\"U\\\\\" expected"; break;
			case 75: s = "\"%\" expected"; break;
			case 76: s = "??? expected"; break;
			case 77: s = "invalid rule"; break;
			case 78: s = "invalid directive"; break;
			case 79: s = "invalid medium"; break;
			case 80: s = "invalid identity"; break;
			case 81: s = "invalid mediaQuery"; break;
			case 82: s = "invalid includeDirective"; break;
			case 83: s = "invalid pseudo"; break;
			case 84: s = "invalid pseudo"; break;
			case 85: s = "invalid term"; break;
			case 86: s = "invalid term"; break;
			case 87: s = "invalid term"; break;
			case 88: s = "invalid term"; break;
			case 89: s = "invalid term"; break;
			case 90: s = "invalid namespaceDirective"; break;
			case 91: s = "invalid genericDirective"; break;
			case 92: s = "invalid simpleselector"; break;
			case 93: s = "invalid attrib"; break;
			case 94: s = "invalid orop"; break;
			case 95: s = "invalid notExpression"; break;
			case 96: s = "invalid primaryBooleanExpression"; break;
			case 97: s = "invalid addop"; break;
			case 98: s = "invalid mulop"; break;
			case 99: s = "invalid termExpression"; break;
			case 100: s = "invalid calculation"; break;
			case 101: s = "invalid HexValue"; break;
			default: s = "error " + n; break;
		}
		return s;
	}
} // end Parser


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}

