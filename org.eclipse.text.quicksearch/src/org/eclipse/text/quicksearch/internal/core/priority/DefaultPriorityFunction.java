/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.text.quicksearch.internal.core.priority;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.text.quicksearch.internal.core.preferences.QuickSearchPreferences;

/**
 * Default implementation of PriorityFunction. It doesn't de-emphasize anything but
 * if has some list of extensions and names that are used to ignore some types of
 * files and directories.
 * <p>
 * This class can be used as is and customised by changing the lists, or it can be
 * subclassed and inherit some of the default behaviour by calling super.priority()
 */
public class DefaultPriorityFunction extends PriorityFunction {

	/**
	 * If true, any resources marked as 'derived' in the Eclipse workspace will
	 * be ignored.
	 */
	public boolean ignoreDerived = true;

	/**
	 * The default priority function causes any resources that end with these strings to
	 * be ignored.
	 */
	public String[] ignoredExtensions = {
			"~", ".bin", ".bmp", ".class", ".com", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			".doc", ".docx", ".exe", ".gif", ".jar", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			".jpg", ".jpeg", ".odp", ".odt", ".p12", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			".pdf", ".png", ".ppt", ".pptx", ".psd", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			".svg", ".xls", ".xlsx", ".zip" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	};

	/**
	 * The default priority function causes any resource who's name (i.e last path segment)
	 * starts with any of these Strings to be ignored.
	 */
	public String[] ignoredPrefixes = {
		"."  //$NON-NLS-1$
	};

	/**
	 * The default priority function causes any resources who's name equals any of these
	 * Strings to be ignored.
	 */
	public String[] ignoredNames = {
		"bin", "build", "target" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	};

	public Set<IResource> ignoredResources = null;

	@Override
	public double priority(IResource r) {
		if (r!=null && r.isAccessible()) {
			if (ignoreDerived && r.isDerived()) {
				return PRIORITY_IGNORE;
			}
			if (ignoredResources!=null && ignoredResources.contains(r)) {
				return PRIORITY_IGNORE;
			}
			String name = r.getName();
			for (String ext : ignoredExtensions) {
				if (name.regionMatches(true, name.length() - ext.length(), ext, 0, ext.length())) {
					return PRIORITY_IGNORE;
				}
			}
			for (String pre : ignoredPrefixes) {
				if (name.startsWith(pre)) {
					return PRIORITY_IGNORE;
				}
			}
			for (String n : ignoredNames) {
				if (name.equals(n)) {
					return PRIORITY_IGNORE;
				}
			}
			return PRIORITY_DEFAULT;
		}
		return PRIORITY_IGNORE;
	}

	/**
	 * Initialize some configurable settings from an instance of QuickSearchPreferences
	 */
	public void configure(QuickSearchPreferences preferences) {
		String[] pref = preferences.getIgnoredExtensions();
		if (pref!=null) {
			this.ignoredExtensions = pref;
		}
		pref = preferences.getIgnoredNames();
		if (pref!=null) {
			this.ignoredNames = pref;
		}
		pref = preferences.getIgnoredPrefixes();
		if (pref!=null) {
			this.ignoredPrefixes = pref;
		}
		computeIgnoredFolders();
	}

	/**
	 * We want to avoid searchin the same files / folders twice in cases where users have 'overlapping projects'.
	 * I.e a project contains folders that are actually correspond to other projects also imported in the workspace.
	 * <p>
	 * See https://issuetracker.springsource.com/browse/STS-3783
	 * <p>
	 * This method computes a set of folders to ignore.
	 */
	private void computeIgnoredFolders() {
		//TODO: Hopefully this won't take too long to compute. Otherwise we may need to look at ways of caching it.
		// it probably doesn't change that often.
		IProject[] allprojects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject p : allprojects) {
			if (p.isAccessible()) {
				URI location = p.getLocationURI();
				if (location!=null) {
					IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(location);
					if (containers!=null) {
						for (IContainer folder : containers) {
							if (!folder.equals(p)) {
								ignore(folder);
							}
						}
					}
				}
			}
		}
	}

	private void ignore(IContainer folder) {
		if (ignoredResources==null) {
			ignoredResources = new HashSet<>();
		}
		ignoredResources.add(folder);
	}
}
