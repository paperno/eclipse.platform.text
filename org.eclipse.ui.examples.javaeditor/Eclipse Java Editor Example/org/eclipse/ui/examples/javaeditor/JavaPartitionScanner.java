/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.examples.javaeditor;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.*;

/**
 * This scanner recognizes the JavaDoc comments and Java multi line comments.
 */
public class JavaPartitionScanner extends RuleBasedPartitionScanner {

	/**
	 * Detector for empty comments.
	 */
	static class EmptyCommentDetector implements IWordDetector {

		/* (non-Javadoc)
		* Method declared on IWordDetector
	 	*/
		public boolean isWordStart(char c) {
			return (c == '/');
		}

		/* (non-Javadoc)
		* Method declared on IWordDetector
	 	*/
		public boolean isWordPart(char c) {
			return (c == '*' || c == '/');
		}
	}
	
	/**
	 * 
	 */
	static class WordPredicateRule extends WordRule implements IPredicateRule {
		
		private IToken fSuccessToken;
		
		public WordPredicateRule(IToken successToken) {
			super(new EmptyCommentDetector());
			fSuccessToken= successToken;
			addWord("/**/", fSuccessToken); //$NON-NLS-1$
		}
		
		/*
		 * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(ICharacterScanner, boolean)
		 */
		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return super.evaluate(scanner);
		}

		/*
		 * @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()
		 */
		public IToken getSuccessToken() {
			return fSuccessToken;
		}
	}

	public final static String JAVA_SINGLELINE_COMMENT= "__java_singleline_comment"; //$NON-NLS-1$
	public final static String JAVA_DOC= "__java_javadoc"; //$NON-NLS-1$
	public final static String JAVA_MULTILINE_COMMENT= "__java_multiline_comment"; //$NON-NLS-1$
	public final static String[] JAVA_PARTITION_TYPES= new String[] { JAVA_SINGLELINE_COMMENT, JAVA_MULTILINE_COMMENT, JAVA_DOC };

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	public JavaPartitionScanner() {
		super();

		IToken javaDoc= new Token(JAVA_DOC);
		IToken comment= new Token(JAVA_MULTILINE_COMMENT);
		IToken single= new Token(JAVA_SINGLELINE_COMMENT);

		List rules= new ArrayList();

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", single)); //$NON-NLS-1$

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
		rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\')); //$NON-NLS-2$ //$NON-NLS-1$

		// Add special case word rule.
		rules.add(new WordPredicateRule(comment));

		// Add rules for multi-line comments and javadoc.
		rules.add(new MultiLineRule("/**", "*/", javaDoc, (char) 0, true)); //$NON-NLS-1$ //$NON-NLS-2$
		rules.add(new MultiLineRule("/*", "*/", comment, (char) 0, true)); //$NON-NLS-1$ //$NON-NLS-2$

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
