/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.texteditor.rulers;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

import org.eclipse.ui.internal.texteditor.TextEditorPlugin;

/**
 * Describes the placement specification of a contribution to the
 * <code>org.eclipse.ui.texteditor.rulerColumns</code> extension point.
 * 
 * @since 3.3
 */
public final class RulerColumnPlacement {
	/** The extension schema name of the id attribute. */
	private static final String ID= "id"; //$NON-NLS-1$
	/** The extension schema name of the optional weight attribute. */
	private static final String LOCATION= "location"; //$NON-NLS-1$
	/** The extension schema name of the before element. */
	private static final String BEFORE= "before"; //$NON-NLS-1$
	/** The extension schema name of the after element. */
	private static final String AFTER= "after"; //$NON-NLS-1$

	/** The placement weight. */
	private final float fWeight;
	/** The placement constraints (element type: {@link RulerColumnPlacementConstraint}). */
	private final Set fConstraints;

	public RulerColumnPlacement() {
		fWeight= 1f;
		fConstraints= Collections.EMPTY_SET;
	}

	public RulerColumnPlacement(IConfigurationElement element) throws InvalidRegistryObjectException {
		Assert.isLegal(element != null);
		ILog log= TextEditorPlugin.getDefault().getLog();
		ExtensionPointHelper helper= new ExtensionPointHelper(element, log);
		
		fWeight= helper.getDefaultAttribute(LOCATION, 1f);
		if (fWeight < 0 || fWeight > 1)
			helper.fail(RulerColumnMessages.RulerColumnPlacement_illegal_weight_msg);
		fConstraints= readIds(log, element.getChildren());
	}

	private Set readIds(ILog log, IConfigurationElement[] children) {
		Set constraints= new LinkedHashSet((int) (children.length / 0.75) + 1, 0.75f);
		for (int i= 0; i < children.length; i++) {
			IConfigurationElement child= children[i];
			String name= child.getName();
			ExtensionPointHelper childHelper= new ExtensionPointHelper(child, log);
			boolean before;
			if (AFTER.equals(name))
				before= false;
			else if (BEFORE.equals(name))
				before= true;
			else {
				childHelper.fail(RulerColumnMessages.RulerColumnPlacement_illegal_child_msg);
				continue;
			}
			constraints.add(new RulerColumnPlacementConstraint(childHelper.getNonNullAttribute(ID), before));
		}
		return Collections.unmodifiableSet(constraints);
	}
	
	/**
	 * The weight of the placement specification, a float in the range <code>[0, 1]</code>.
	 * 
	 * @return the weight of the placement specification
	 */
	public float getWeight() {
		return fWeight;
	}

	/**
	 * Returns the placement constraints in the order that they appear in the extension declaration.
	 * 
	 * @return the unmodifiable set of placement constraints in the order that they appear in the
	 *         extension declaration
	 */
	public Set getConstraints() {
		return fConstraints;
	}
}