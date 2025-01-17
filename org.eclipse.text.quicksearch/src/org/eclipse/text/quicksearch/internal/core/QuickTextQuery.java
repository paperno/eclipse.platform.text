/*******************************************************************************
 * Copyright (c) 2013, 2010 Pivotal Software, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.text.quicksearch.internal.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IRegion;

/**
 * Represents something you can search for with a 'quick search' text searcher.
 *
 * @author Kris De Volder
 */
public class QuickTextQuery {

	//TODO: delete and use jface Region class instead.
	public class TextRange implements IRegion {
		public final int start;
		public final int len;
		public TextRange(int start, int len) {
			this.start = start;
			this.len = len;
		}
		public int getLength() {
			return len;
		}
		public int getOffset() {
			return start;
		}
	}

	private boolean caseSensitive;
	private String orgPattern; //Original pattern case preserved even if search is case insensitive.
	private Pattern pattern;

	/**
	 * A query that matches anything.
	 */
	public QuickTextQuery() {
		this("", true); //$NON-NLS-1$
	}

	public QuickTextQuery(String substring, boolean caseSensitive) {
		this.orgPattern = substring;
		this.caseSensitive = caseSensitive;
		createMatcher(substring, caseSensitive);
	}

	/**
	 * Compile a pattern string into an RegExp and create a Matcher for that
	 * regexp. This is so we can 'compile' the pattern once and then keep reusing
	 * the matcher or compiled pattern.
	 */
	private void createMatcher(String patString, boolean caseSensitive) {
		StringBuilder segment = new StringBuilder(); //Accumulates text that needs to be 'quoted'
		StringBuilder regexp = new StringBuilder(); //Accumulates 'compiled' pattern
		int pos = 0, len = patString.length();
		while (pos<len) {
			char c = patString.charAt(pos++);
			switch (c) {
			case '?':
				appendSegment(segment, regexp);
				regexp.append('.');
				break;
			case '*':
				appendSegment(segment, regexp);
				regexp.append(".*"); //$NON-NLS-1$
				break;
			case '\\':
				if (pos<len) {
					char nextChar = patString.charAt(pos);
					if (nextChar=='*' || nextChar=='?' || nextChar=='\\') {
						segment.append(nextChar);
						pos++;
						break;
					}
				}
			default:
				//Char is 'nothing special'. Add it to segment that will be wrapped in 'quotes'
				segment.append(c);
				break;
			}
		}
		//Don't forget to process that last segment.
		appendSegment(segment, regexp);

		this.pattern = Pattern.compile(regexp.toString(), caseSensitive?0:Pattern.CASE_INSENSITIVE);
//		this.matcher = pattern.matcher("");
	}

	private void appendSegment(StringBuilder segment, StringBuilder regexp) {
		if (segment.length()>0) {
			regexp.append(Pattern.quote(segment.toString()));
			segment.setLength(0); //clear: ready for next segment
		}
		//else {
		// nothing to append
		//}
	}

	public boolean equalsFilter(QuickTextQuery o) {
		//TODO: actually for case insensitive matches we could relax this and treat patterns that
		// differ only in case as the same.
		return this.caseSensitive == o.caseSensitive && this.orgPattern.equals(o.orgPattern);
	}

	/**
	 * Returns true if the other query is a specialisation of this query. I.e. any results matching the other
	 * query must also match this query. If this method returns true then we can optimise the search for other
	 * re-using already found results for this query.
	 * <p>
	 * If it is hard or impossible to decide whether other query is a specialisation of this query then this
	 * method is allowed to 'punt' and just return false. However, the consequence of this is that the query
	 * will be re-run instead of incrementally updated.
	 */
	public boolean isSubFilter(QuickTextQuery other) {
		if (this.isTrivial()) {
			return false;
		}
		if (this.caseSensitive==other.caseSensitive) {
			boolean caseSensitive = this.caseSensitive;
			String otherPat = normalize(other.orgPattern, caseSensitive);
			String thisPat = normalize(this.orgPattern, caseSensitive);
			return otherPat.contains(thisPat);
		}
		return false;
	}

	/**
	 * Transforms a pattern string so we can use a simple 'substring' test to determine
	 * whether one pattern is sub-pattern of the other.
	 */
	private String normalize(String pat, boolean caseSensitive) {
		if (pat.endsWith("\\")) { //$NON-NLS-1$
			pat = pat + "\\"; //$NON-NLS-1$
		}
		if (!caseSensitive) {
			pat = pat.toLowerCase();
		}
		return pat;
	}

	/**
	 * @return whether the LineItem text contains the search pattern.
	 */
	public boolean matchItem(LineItem item) {
		return matchItem(item.getText());
	}

	/**
	 * Same as matchItem except only takes the text of the item. This can
	 * be useful for efficient processing. In particular to avoid creating
	 * LineItem instances for non-matching lines.
	 */
	public boolean matchItem(String item) {
		//Alternate implementation. This is thread safe without synchronized,
		// but it creates some garbage.
		Matcher matcher = pattern.matcher(item); //Creating garbage here
		return matcher.find();
	}

	/**
	 * A trivial query is one that either
	 *  - matches anything
	 *  - matches nothing
	 * In other words, if a query is 'trivial' then it returns either nothing or all the text in the scope
	 * of the search.
	 */
	public boolean isTrivial() {
		return "".equals(this.orgPattern); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "QTQuery("+orgPattern+", "+(caseSensitive?"caseSens":"caseInSens")+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	public List<TextRange> findAll(String text) {
		//alternate implementation without 'synchronized' but creates more garbage
		if (isTrivial()) {
			return Arrays.asList();
		} else {
			List<TextRange> ranges = new ArrayList<>();
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end();
				ranges.add(new TextRange(start, end-start));
			}
			return ranges;
		}
	}

	public TextRange findFirst(String str) {
		//TODO: more efficient implementation, just search the first one
		// no need to find all matches then toss away everything except the
		// first one.
		List<TextRange> all = findAll(str);
		if (all!=null && !all.isEmpty()) {
			return all.get(0);
		}
		return null;
	}

	public String getPatternString() {
		return orgPattern;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}


}
