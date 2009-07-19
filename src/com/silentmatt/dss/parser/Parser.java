package com.silentmatt.dss.parser;

import java.util.*;
import com.silentmatt.dss.*;
import com.silentmatt.dss.expression.*;

public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _integer = 2;
	public static final int _decimal = 3;
	public static final int _s = 4;
	public static final int _stringLit = 5;
	public static final int maxT = 59;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	Token t;    // last recognized token
	Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public ErrorReporter errors;

	public CSSDocument CSSDoc;

		boolean PartOfHex(String value) {
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
		boolean IsUnit() {
			if (la.kind != 1) { return false; }
			List<String> units = Arrays.asList(new String[] { "em", "ex", "px", "gd", "rem", "vw", "vh", "vm", "ch", "mm", "cm", "in", "pt", "pc", "deg", "grad", "rad", "turn", "ms", "s", "hz", "khz" });
			return units.contains(la.val);
		}

/*------------------------------------------------------------------------*
 *----- SCANNER DESCRIPTION ----------------------------------------------*
 *------------------------------------------------------------------------*/



	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new ListErrorReporter();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
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
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
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
		CSSDoc = new CSSDocument();
		String cset = null;
		RuleSet rset = null;
		Directive dir = null;
		
		while (la.kind == 4 || la.kind == 6 || la.kind == 7) {
			if (la.kind == 6) {
				Get();
			} else if (la.kind == 7) {
				Get();
			} else {
				Get();
			}
		}
		while (StartOf(1)) {
			if (StartOf(2)) {
				rset = ruleset();
				CSSDoc.addRuleSet(rset); 
			} else {
				dir = directive();
				CSSDoc.addDirective(dir); 
			}
			while (la.kind == 4 || la.kind == 6 || la.kind == 7) {
				if (la.kind == 6) {
					Get();
				} else if (la.kind == 7) {
					Get();
				} else {
					Get();
				}
			}
		}
	}

	RuleSet  ruleset() {
		RuleSet  rset;
		rset = new RuleSet();
		Selector sel = null;
		Declaration dec = null;
		
		sel = selector();
		rset.getSelectors().add(sel); 
		while (la.kind == 39) {
			Get();
			sel = selector();
			rset.getSelectors().add(sel); 
		}
		Expect(23);
		while (StartOf(3)) {
			if (StartOf(4)) {
				dec = declaration();
				Expect(28);
				rset.addDeclaration(dec); 
			} else if (la.kind == 26) {
				DirectiveBuilder dir = classDirective();
				rset.addDirective(dir.build()); 
			} else {
				DirectiveBuilder dir = defineDirective();
				rset.addDirective(dir.build()); 
			}
		}
		Expect(24);
		return rset;
	}

	Directive  directive() {
		Directive  dir;
		dir = null; //new GenericDirective();
		Declaration dec = null;
		RuleSet rset = null;
		Expression exp = null;
		Directive dr = null;
		String ident = null;
		Medium m;
		                           DirectiveBuilder dirb = new DirectiveBuilder();
		
		switch (la.kind) {
		case 22: {
			dirb = mediaDirective();
			break;
		}
		case 26: {
			dirb = classDirective();
			break;
		}
		case 30: {
			dirb = defineDirective();
			break;
		}
		case 32: {
			dirb = fontFaceDirective();
			break;
		}
		case 34: {
			dirb = importDirective();
			break;
		}
		case 35: {
			dirb = includeDirective();
			break;
		}
		case 36: {
			dirb = charsetDirective();
			break;
		}
		case 33: {
			dirb = pageDirective();
			break;
		}
		case 37: {
			dirb = namespaceDirective();
			break;
		}
		case 38: {
			Get();
			ident = identity();
			dirb.setName("@" + ident);
			String lcName = ident.toLowerCase();
			dirb.setType(DirectiveType.Other);
			
			if (StartOf(5)) {
				if (StartOf(5)) {
					exp = expr();
					dirb.setExpression(exp); 
				} else {
					m = medium();
					dirb.getMediums().add(m); 
				}
			}
			if (la.kind == 23) {
				Get();
				while (StartOf(1)) {
					if (StartOf(4)) {
						dec = declaration();
						Expect(28);
						dirb.addDeclaration(dec); 
					} else if (StartOf(2)) {
						rset = ruleset();
						dirb.addRuleSet(rset); 
					} else {
						dr = directive();
						dirb.addDirective(dr); 
					}
				}
				Expect(24);
			} else if (la.kind == 28) {
				Get();
			} else SynErr(60);
			break;
		}
		default: SynErr(61); break;
		}
		dir = dirb.build(); 
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
		url = ""; 
		Expect(8);
		if (la.kind == 9) {
			Get();
		}
		if (la.kind == 5) {
			url = QuotedString();
		} else if (StartOf(6)) {
			while (StartOf(7)) {
				Get();
				url += t.val; 
				if (la.val.equals(")")) { break; } 
			}
		} else SynErr(62);
		if (la.kind == 10) {
			Get();
		}
		return url;
	}

	Medium  medium() {
		Medium  m;
		m = Medium.all; 
		switch (la.kind) {
		case 11: {
			Get();
			m = Medium.all; 
			break;
		}
		case 12: {
			Get();
			m = Medium.aural; 
			break;
		}
		case 13: {
			Get();
			m = Medium.braille; 
			break;
		}
		case 14: {
			Get();
			m = Medium.embossed; 
			break;
		}
		case 15: {
			Get();
			m = Medium.handheld; 
			break;
		}
		case 16: {
			Get();
			m = Medium.print; 
			break;
		}
		case 17: {
			Get();
			m = Medium.projection; 
			break;
		}
		case 18: {
			Get();
			m = Medium.screen; 
			break;
		}
		case 19: {
			Get();
			m = Medium.tty; 
			break;
		}
		case 20: {
			Get();
			m = Medium.tv; 
			break;
		}
		default: SynErr(63); break;
		}
		return m;
	}

	String  identity() {
		String  ident;
		ident = null; 
		switch (la.kind) {
		case 1: {
			Get();
			break;
		}
		case 21: {
			Get();
			break;
		}
		case 8: {
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
		case 19: {
			Get();
			break;
		}
		case 20: {
			Get();
			break;
		}
		default: SynErr(64); break;
		}
		ident = t.val; 
		return ident;
	}

	DirectiveBuilder  mediaDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Medium m;
		RuleSet rset = new RuleSet();
		dirb.setName("@media");
		dirb.setType(DirectiveType.Media);
		
		Expect(22);
		m = medium();
		dirb.getMediums().add(m); 
		Expect(23);
		while (StartOf(8)) {
			if (StartOf(2)) {
				rset = ruleset();
				dirb.addRuleSet(rset); 
			} else if (la.kind == 26) {
				DirectiveBuilder dir = classDirective();
				dirb.addDirective(dir.build()); 
			} else if (la.kind == 30) {
				DirectiveBuilder dir = defineDirective();
				dirb.addDirective(dir.build()); 
			} else {
				DirectiveBuilder dir = includeDirective();
				dirb.addDirective(dir.build()); 
			}
		}
		Expect(24);
		return dirb;
	}

	DirectiveBuilder  classDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		String ident;
		Declaration param;
		RuleSet rset = new RuleSet();
		dirb.setType(DirectiveType.Class);
		
		Expect(26);
		ident = identity();
		dirb.setID(ident); 
		if (la.kind == 27) {
			Get();
			if (StartOf(4)) {
				param = parameter();
				dirb.addParameter(param); 
				while (la.kind == 28) {
					Get();
					param = parameter();
					dirb.addParameter(param); 
				}
			}
			Expect(29);
		}
		Expect(23);
		if (StartOf(4)) {
			dec = declaration();
			dirb.addDeclaration(dec); 
			while (la.kind == 28) {
				Get();
				if (la.val.equals("}")) { Get(); return dirb; } 
				dec = declaration();
				dirb.addDeclaration(dec); 
			}
			if (la.kind == 28) {
				Get();
			}
		}
		Expect(24);
		return dirb;
	}

	DirectiveBuilder  defineDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		RuleSet rset = new RuleSet();
		dirb.setType(DirectiveType.Define);
		
		Expect(30);
		if (la.kind == 31) {
			Get();
			dirb.setID("global"); 
		}
		Expect(23);
		while (StartOf(4)) {
			dec = declaration();
			Expect(28);
			dirb.addDeclaration(dec); 
		}
		Expect(24);
		return dirb;
	}

	DirectiveBuilder  includeDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		RuleSet rset = new RuleSet();
		Term trm = new Term();
		Expression expr = new Expression();
		dirb.setType(DirectiveType.Include);
		String url;
		
		Expect(35);
		url = URI();
		trm.setValue(url);
		trm.setType(TermType.Url);
		expr.getTerms().add(trm);
		dirb.setExpression(expr); 
		Expect(28);
		return dirb;
	}

	Declaration  parameter() {
		Declaration  dec;
		dec = new Declaration();
		Expression exp = null;
		String ident = null;
		
		ident = identity();
		dec.setName(ident); 
		if (la.kind == 25) {
			Get();
			exp = expr();
			dec.setExpression(exp); 
		}
		return dec;
	}

	Expression  expr() {
		Expression  exp;
		exp = new Expression();
		Character sep = null;
		Term trm = null;
		
		trm = term();
		exp.getTerms().add(trm); 
		while (StartOf(9)) {
			if (la.kind == 39 || la.kind == 54) {
				if (la.kind == 54) {
					Get();
					sep = '/'; 
				} else {
					Get();
					sep = ','; 
				}
			}
			trm = term();
			if (sep != null) { trm.setSeperator(sep); }
			exp.getTerms().add(trm);
			sep = null;
			
		}
		return exp;
	}

	Declaration  declaration() {
		Declaration  dec;
		dec = new Declaration();
		Expression exp = null;
		String ident = null;
		
		ident = identity();
		dec.setName(ident); 
		Expect(25);
		exp = expr();
		dec.setExpression(exp); 
		if (la.kind == 53) {
			Get();
			dec.setImportant(true); 
		}
		return dec;
	}

	DirectiveBuilder  fontFaceDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		RuleSet rset = new RuleSet();
		dirb.setType(DirectiveType.FontFace);
		
		Expect(32);
		Expect(23);
		if (StartOf(4)) {
			dec = declaration();
			dirb.addDeclaration(dec); 
			while (la.kind == 28) {
				Get();
				if (la.val.equals("}")) { Get(); return dirb; } 
				dec = declaration();
				dirb.addDeclaration(dec); 
			}
			if (la.kind == 28) {
				Get();
			}
		}
		Expect(24);
		return dirb;
	}

	DirectiveBuilder  pageDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		SimpleSelector ss;
		String psd;
		dirb.setType(DirectiveType.Page);
		
		Expect(33);
		if (la.kind == 25) {
			psd = pseudo();
			ss = new SimpleSelector();
			ss.setPseudo(psd);
			dirb.setSimpleSelector(ss);
			
		}
		Expect(23);
		if (StartOf(4)) {
			dec = declaration();
			dirb.addDeclaration(dec); 
			while (la.kind == 28) {
				Get();
				if (la.val.equals("}")) { Get(); return dirb; } 
				dec = declaration();
				dirb.addDeclaration(dec); 
			}
			if (la.kind == 28) {
				Get();
			}
		}
		Expect(24);
		return dirb;
	}

	String  pseudo() {
		String  pseudo;
		pseudo = "";
		Expression exp = null;
		String ident = null;
		
		Expect(25);
		if (la.kind == 25) {
			Get();
		}
		ident = identity();
		pseudo = ident; 
		if (la.kind == 9) {
			Get();
			pseudo += t.val; 
			exp = expr();
			pseudo += exp.toString(); 
			Expect(10);
			pseudo += t.val; 
		}
		return pseudo;
	}

	DirectiveBuilder  importDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		RuleSet rset = new RuleSet();
		Term trm = new Term();
		Expression expr = new Expression();
		dirb.setType(DirectiveType.Import);
		Medium m;
		String url;
		
		Expect(34);
		url = URI();
		trm.setValue(url);
		trm.setType(TermType.Url);
		expr.getTerms().add(trm);
		dirb.setExpression(expr); 
		if (StartOf(10)) {
			m = medium();
			dirb.getMediums().add(m); 
		}
		Expect(28);
		return dirb;
	}

	DirectiveBuilder  charsetDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		RuleSet rset = new RuleSet();
		Term trm;
		Expression expr = new Expression();
		dirb.setType(DirectiveType.Charset);
		
		Expect(36);
		trm = term();
		expr.getTerms().add(trm);
		dirb.setExpression(expr); 
		Expect(28);
		return dirb;
	}

	Term  term() {
		Term  trm;
		trm = new Term();
		String val = "";
		Expression exp = null;
		String ident = null;
		                              CalcExpression expression = null;
		
		if (la.kind == 5) {
			val = QuotedString();
			trm.setValue(val); trm.setType(TermType.String); 
		} else if (la.kind == 8) {
			val = URI();
			trm.setValue(val); trm.setType(TermType.Url); 
		} else if (la.kind == 57) {
			Get();
			ident = identity();
			trm.setValue("U\\" + ident); trm.setType(TermType.Unicode); 
		} else if (la.kind == 43) {
			val = HexValue();
			trm.setValue(val); trm.setType(TermType.Hex); 
		} else if (la.kind == 56) {
			expression = calculation();
			trm.setCalculation(expression); trm.setType(TermType.Calculation); 
		} else if (StartOf(4)) {
			ident = identity();
			trm.setValue(ident); trm.setType(TermType.String); 
			if (StartOf(11)) {
				while (la.kind == 25 || la.kind == 44 || la.kind == 46) {
					if (la.kind == 25) {
						Get();
						trm.setValue(trm.getValue() + t.val); 
						if (la.kind == 25) {
							Get();
							trm.setValue(trm.getValue() + t.val); 
						}
						ident = identity();
						trm.setValue(trm.getValue() + ident); 
					} else if (la.kind == 44) {
						Get();
						trm.setValue(trm.getValue() + t.val); 
						ident = identity();
						trm.setValue(trm.getValue() + ident); 
					} else {
						Get();
						trm.setValue(trm.getValue() + t.val); 
						if (StartOf(4)) {
							ident = identity();
							trm.setValue(trm.getValue() + ident); 
						} else if (la.kind == 2) {
							Get();
							trm.setValue(trm.getValue() + t.val); 
						} else SynErr(65);
					}
				}
			}
			if (la.kind == 9 || la.kind == 27) {
				if (la.kind == 9) {
					Get();
					exp = expr();
					Function func = new Function();
					func.setName(trm.getValue());
					func.setExpression(exp);
					trm.setValue(null);
					trm.setFunction(func);
					trm.setType(TermType.Function);
					
					Expect(10);
				} else {
					Get();
					ClassReference cls = new ClassReference();
					Declaration dec;
					cls.setName(trm.getValue());
					trm.setValue(null);
					trm.setClassReference(cls);
					trm.setType(TermType.ClassReference);
					
					if (StartOf(4)) {
						dec = declaration();
						cls.addArgument(dec); 
						while (la.kind == 28) {
							Get();
							dec = declaration();
							cls.addArgument(dec); 
						}
					}
					Expect(29);
				}
			}
		} else if (StartOf(12)) {
			if (la.kind == 40 || la.kind == 55) {
				if (la.kind == 55) {
					Get();
					trm.setSign('-'); 
				} else {
					Get();
					trm.setSign('+'); 
				}
			}
			if (la.kind == 2) {
				Get();
			} else if (la.kind == 3) {
				Get();
			} else SynErr(66);
			val = t.val; 
			if (StartOf(13)) {
				if (la.val.toLowerCase().equals("n")) {
					Expect(21);
					val += t.val; 
					if (la.kind == 40 || la.kind == 55) {
						if (la.kind == 40) {
							Get();
							val += t.val; 
						} else {
							Get();
							val += t.val; 
						}
						Expect(2);
						val += t.val; 
					}
				} else if (la.kind == 58) {
					Get();
					trm.setUnit(Unit.Percent); 
				} else {
					if (IsUnit()) {
						ident = identity();
						try {
						trm.setUnit(Unit.parse(ident));
						} catch (Exception ex) {
							errors.SemErr(t.line, t.col, "Unrecognized unit '" + ident + "'");
						}
						
					}
				}
			}
			trm.setValue(val); trm.setType(TermType.Number); 
		} else SynErr(67);
		return trm;
	}

	DirectiveBuilder  namespaceDirective() {
		DirectiveBuilder  dirb;
		dirb = new DirectiveBuilder();
		Declaration dec = null;
		RuleSet rset = new RuleSet();
		Term trm = new Term();
		Expression expr = new Expression();
		dirb.setType(DirectiveType.Namespace);
		String ident;
		String url;
		
		Expect(37);
		if (StartOf(4)) {
			ident = identity();
			dirb.setID(ident); 
		}
		if (la.kind == 8) {
			url = URI();
			trm.setValue(url);
			trm.setType(TermType.Url);
			expr.getTerms().add(trm);
			dirb.setExpression(expr);
			
		} else if (la.kind == 5) {
			url = QuotedString();
			trm.setValue(url);
			trm.setType(TermType.Url);
			expr.getTerms().add(trm);
			dirb.setExpression(expr);
			
		} else SynErr(68);
		Expect(28);
		return dirb;
	}

	Selector  selector() {
		Selector  sel;
		sel = new Selector();
		SimpleSelector ss = null;
		Combinator cb = null;
		
		ss = simpleselector();
		sel.getSimpleSelectors().add(ss); 
		while (StartOf(14)) {
			if (la.kind == 29 || la.kind == 40 || la.kind == 41) {
				if (la.kind == 40) {
					Get();
					cb = Combinator.PrecededImmediatelyBy; 
				} else if (la.kind == 29) {
					Get();
					cb = Combinator.ChildOf; 
				} else {
					Get();
					cb = Combinator.PrecededBy; 
				}
			}
			ss = simpleselector();
			if (cb != null) { ss.setCombinator(cb); }
			sel.getSimpleSelectors().add(ss);
			
			cb = null; 
		}
		return sel;
	}

	SimpleSelector  simpleselector() {
		SimpleSelector  ss;
		ss = new SimpleSelector();
		String psd = null;
		Attribute atb = null;
		SimpleSelector parent = ss;
		String ident = null;
		
		if (StartOf(4)) {
			ident = identity();
			ss.setElementName(ident); 
		} else if (la.kind == 42) {
			Get();
			ss.setElementName("*"); 
		} else if (StartOf(15)) {
			if (la.kind == 43) {
				Get();
				ident = identity();
				ss.setID(ident); 
			} else if (la.kind == 44) {
				Get();
				ident = identity();
				ss.setClassName(ident); 
			} else if (la.kind == 45) {
				atb = attrib();
				ss.setAttribute(atb); 
			} else {
				psd = pseudo();
				ss.setPseudo(psd); 
			}
		} else SynErr(69);
		while (StartOf(15)) {
			SimpleSelector child = new SimpleSelector(); 
			if (la.kind == 43) {
				Get();
				ident = identity();
				child.setID(ident); 
			} else if (la.kind == 44) {
				Get();
				ident = identity();
				child.setClassName(ident); 
			} else if (la.kind == 45) {
				atb = attrib();
				child.setAttribute(atb); 
			} else {
				psd = pseudo();
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
		String quote = null;
		String ident = null;
		
		Expect(45);
		ident = identity();
		atb.setOperand(ident); 
		if (StartOf(16)) {
			switch (la.kind) {
			case 46: {
				Get();
				atb.setOperator(AttributeOperator.Equals); 
				break;
			}
			case 47: {
				Get();
				atb.setOperator(AttributeOperator.InList); 
				break;
			}
			case 48: {
				Get();
				atb.setOperator(AttributeOperator.Hyphenated); 
				break;
			}
			case 49: {
				Get();
				atb.setOperator(AttributeOperator.EndsWith); 
				break;
			}
			case 50: {
				Get();
				atb.setOperator(AttributeOperator.BeginsWith); 
				break;
			}
			case 51: {
				Get();
				atb.setOperator(AttributeOperator.Contains); 
				break;
			}
			}
			if (StartOf(4)) {
				ident = identity();
				atb.setValue(ident); 
			} else if (la.kind == 5) {
				quote = QuotedString();
				atb.setValue(quote); 
			} else SynErr(70);
		}
		Expect(52);
		return atb;
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
		while (la.kind == 40 || la.kind == 55) {
			op = addop();
			right = multiplicativeExpression();
			expr = new BinaryExpression(op, expr, right); 
		}
		return expr;
	}

	Operation  addop() {
		Operation  op;
		op = null; 
		if (la.kind == 40) {
			Get();
			op = Operation.Add; 
		} else if (la.kind == 55) {
			Get();
			op = Operation.Subtract; 
		} else SynErr(71);
		return op;
	}

	Operation  mulop() {
		Operation  op;
		op = null; 
		if (la.kind == 42) {
			Get();
			op = Operation.Multiply; 
		} else if (la.kind == 54) {
			Get();
			op = Operation.Divide; 
		} else SynErr(72);
		return op;
	}

	CalcExpression  multiplicativeExpression() {
		CalcExpression  expr;
		CalcExpression left, right; Term trm; Operation op; 
		left = termExpression();
		expr = left; 
		while (la.kind == 42 || la.kind == 54) {
			op = mulop();
			right = termExpression();
			expr = new BinaryExpression(op, expr, right); 
		}
		return expr;
	}

	CalcExpression  termExpression() {
		CalcExpression  expr;
		expr = null; Term trm; CalcExpression exp; 
		if (la.kind == 9) {
			Get();
			exp = lengthExpression();
			Expect(10);
			expr = exp; 
		} else if (StartOf(5)) {
			trm = term();
			expr = new TermExpression(trm); 
		} else SynErr(73);
		return expr;
	}

	CalcExpression  calculation() {
		CalcExpression  expr;
		Expect(56);
		Expect(9);
		expr = lengthExpression();
		Expect(10);
		return expr;
	}

	String  HexValue() {
		String  val;
		val = "";
		boolean found = false;
		
		Expect(43);
		val += t.val; 
		if (la.kind == 2) {
			Get();
			val += t.val; 
		} else if (PartOfHex(val)) {
			Expect(1);
			val += t.val; found = true; 
		} else SynErr(74);
		if (!found && PartOfHex(val)) {
			Expect(1);
			val += t.val; 
		}
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
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,T,x,x, x,x,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,T,x, x,T,T,x, x,x,T,x, T,T,T,T, T,T,T,x, x,x,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,T,x,x, x,x,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,T,x,x, x,x,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,x,T,x, x,x,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,T,x,x, x,x,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,T,T,T, x,T,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,T, x,x,x,x, x,x,x,x, x,x,x,T, T,T,x,x, x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, x},
		{x,T,T,T, T,x,T,T, T,T,x,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, x},
		{x,T,x,x, x,x,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,T,T,x, x,x,T,x, x,x,x,T, x,x,x,x, x,x,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,T,T,T, x,T,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, T,x,x,T, x,x,x,x, x,x,x,x, x,x,T,T, T,T,x,x, x},
		{x,x,x,x, x,x,x,x, x,x,x,T, T,T,T,T, T,T,T,T, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,x,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, x,x,x,x, x},
		{x,T,x,x, x,x,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,x, x},
		{x,T,x,x, x,x,x,x, T,x,x,T, T,T,T,T, T,T,T,T, T,T,x,x, x,T,x,x, x,T,x,x, x,x,x,x, x,x,x,x, T,T,T,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,T, T,T,T,T, x,x,x,x, x,x,x,x, x}

	};

	public static final String getErrorMessage(int n) {
		String s = null;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "integer expected"; break;
			case 3: s = "decimal expected"; break;
			case 4: s = "s expected"; break;
			case 5: s = "stringLit expected"; break;
			case 6: s = "\"<!--\" expected"; break;
			case 7: s = "\"-->\" expected"; break;
			case 8: s = "\"url\" expected"; break;
			case 9: s = "\"(\" expected"; break;
			case 10: s = "\")\" expected"; break;
			case 11: s = "\"all\" expected"; break;
			case 12: s = "\"aural\" expected"; break;
			case 13: s = "\"braille\" expected"; break;
			case 14: s = "\"embossed\" expected"; break;
			case 15: s = "\"handheld\" expected"; break;
			case 16: s = "\"print\" expected"; break;
			case 17: s = "\"projection\" expected"; break;
			case 18: s = "\"screen\" expected"; break;
			case 19: s = "\"tty\" expected"; break;
			case 20: s = "\"tv\" expected"; break;
			case 21: s = "\"n\" expected"; break;
			case 22: s = "\"@media\" expected"; break;
			case 23: s = "\"{\" expected"; break;
			case 24: s = "\"}\" expected"; break;
			case 25: s = "\":\" expected"; break;
			case 26: s = "\"@class\" expected"; break;
			case 27: s = "\"<\" expected"; break;
			case 28: s = "\";\" expected"; break;
			case 29: s = "\">\" expected"; break;
			case 30: s = "\"@define\" expected"; break;
			case 31: s = "\"global\" expected"; break;
			case 32: s = "\"@font-face\" expected"; break;
			case 33: s = "\"@page\" expected"; break;
			case 34: s = "\"@import\" expected"; break;
			case 35: s = "\"@include\" expected"; break;
			case 36: s = "\"@charset\" expected"; break;
			case 37: s = "\"@namespace\" expected"; break;
			case 38: s = "\"@\" expected"; break;
			case 39: s = "\",\" expected"; break;
			case 40: s = "\"+\" expected"; break;
			case 41: s = "\"~\" expected"; break;
			case 42: s = "\"*\" expected"; break;
			case 43: s = "\"#\" expected"; break;
			case 44: s = "\".\" expected"; break;
			case 45: s = "\"[\" expected"; break;
			case 46: s = "\"=\" expected"; break;
			case 47: s = "\"~=\" expected"; break;
			case 48: s = "\"|=\" expected"; break;
			case 49: s = "\"$=\" expected"; break;
			case 50: s = "\"^=\" expected"; break;
			case 51: s = "\"*=\" expected"; break;
			case 52: s = "\"]\" expected"; break;
			case 53: s = "\"!important\" expected"; break;
			case 54: s = "\"/\" expected"; break;
			case 55: s = "\"-\" expected"; break;
			case 56: s = "\"calc\" expected"; break;
			case 57: s = "\"U\\\\\" expected"; break;
			case 58: s = "\"%\" expected"; break;
			case 59: s = "??? expected"; break;
			case 60: s = "invalid directive"; break;
			case 61: s = "invalid directive"; break;
			case 62: s = "invalid URI"; break;
			case 63: s = "invalid medium"; break;
			case 64: s = "invalid identity"; break;
			case 65: s = "invalid term"; break;
			case 66: s = "invalid term"; break;
			case 67: s = "invalid term"; break;
			case 68: s = "invalid namespaceDirective"; break;
			case 69: s = "invalid simpleselector"; break;
			case 70: s = "invalid attrib"; break;
			case 71: s = "invalid addop"; break;
			case 72: s = "invalid mulop"; break;
			case 73: s = "invalid termExpression"; break;
			case 74: s = "invalid HexValue"; break;
			default: s = "error " + n; break;
		}
		return s;
	}
} // end Parser


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}

