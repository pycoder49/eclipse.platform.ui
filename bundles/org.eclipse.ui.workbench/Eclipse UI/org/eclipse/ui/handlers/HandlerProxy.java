/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.handlers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.commands.AbstractHandler;
import org.eclipse.ui.commands.ExecutionException;
import org.eclipse.ui.commands.IHandler;
import org.eclipse.ui.internal.WorkbenchPlugin;

/**
 * A proxy for a handler that has been defined in XML. This delays the class
 * loading until the handler is really asked for information (besides the
 * priority or the command identifier). Asking a proxy for anything but the
 * attributes defined publicly in this class will cause the proxy to instantiate
 * the proxied handler.
 * 
 * @since 3.0
 */
public final class HandlerProxy extends AbstractHandler {

    /**
     * The name of the attribute containing the command identifier.
     */
    public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

    /**
     * The name of the attribute containing the priority.
     */
    public static final String ATTRIBUTE_PRIORITY = "priority"; //$NON-NLS-1$

    /**
     * The name of the configuration element attribute which contains the
     * information necessary to instantiate the real handler.
     */
    private static final String HANDLER_ATTRIBUTE_NAME = "handler"; //$NON-NLS-1$

    /**
     * The identifier for the command to which this proxy should be associated.
     * This value should never be <code>null</code>.
     */
    private final String commandId;

    /**
     * The configuration element from which the handler can be created. This
     * value will exist until the element is converted into a real class -- at
     * which point this value will be set to <code>null</code>.
     */
    private IConfigurationElement configurationElement;

    /**
     * The real handler. This value is <code>null</code> until the proxy is
     * forced to load the real handler. At this point, the configuration element
     * is converted, nulled out, and this handler gains a reference.
     */
    private IHandler handler;

    /**
     * The priority of this handler in the system. The priority is an integer
     * value. The greater the integer, the higher the priority.
     */
    private final Integer priority;

    /**
     * Constructs a new instance of <code>HandlerProxy</code> with all the
     * information it needs to try to avoid loading until it is needed.
     * 
     * @param newCommandId
     *            The identifier for the command to which this proxy should be
     *            associated; must not be <code>null</code>.
     * @param newPriority
     *            The priority at which this proxy should exist. The higher the
     *            number, the more priority it has.
     * @param newConfigurationElement
     *            The configuration element from which the real class can be
     *            loaded at run-time.
     */
    public HandlerProxy(final String newCommandId, final Integer newPriority,
            final IConfigurationElement newConfigurationElement) {
        commandId = newCommandId;
        priority = newPriority;
        configurationElement = newConfigurationElement;
        handler = null;
    }

    public void execute(Object parameter) throws ExecutionException {
        if (loadHandler()) {
            handler.execute(parameter);
        }
    }

    // this method is no longer part of IHandler. lets try to remove this..
    public Object getAttributeValue(String attributeName) {
        if (handler == null) {
            if (ATTRIBUTE_ID.equals(attributeName))
                return commandId;
            else if (ATTRIBUTE_PRIORITY.equals(attributeName)) return priority;
        }

        return getAttributeValuesByName().get(attributeName);
    }

    public Map getAttributeValuesByName() {
        if (loadHandler())
            return handler.getAttributeValuesByName();
        else
            return Collections.EMPTY_MAP;
    }

    /**
     * Loads the handler, if possible. If the handler is loaded, then the member
     * variables are updated accordingly.
     * 
     * @return <code>true</code> if the handler is now non-null;
     *         <code>false</code> otherwise.
     */
    private final boolean loadHandler() {
        if (handler == null) {
            // Load the handler.
            try {
                handler = (IHandler) configurationElement
                        .createExecutableExtension(HANDLER_ATTRIBUTE_NAME);
                configurationElement = null;
                return true;
            } catch (final CoreException e) {
                /*
                 * TODO If it can't be instantiated, should future attempts to
                 * instantiate be blocked?
                 */
                final String message = "The proxied handler for '" + commandId //$NON-NLS-1$
                        + "' could not be loaded"; //$NON-NLS-1$
                IStatus status = new Status(IStatus.ERROR,
                        WorkbenchPlugin.PI_WORKBENCH, 0, message, e);
                WorkbenchPlugin.log(message, status);
                return false;
            }
        }

        return true;
    }
}