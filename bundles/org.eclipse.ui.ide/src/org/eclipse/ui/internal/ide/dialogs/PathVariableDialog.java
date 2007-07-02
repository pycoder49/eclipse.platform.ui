/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - 19346, 42056
 *******************************************************************************/
package org.eclipse.ui.internal.ide.dialogs;

import java.util.Set;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;

/**
 * Dialog that prompts the user for defining a variable's name and value. It
 * supports creating a new variable or editing an existing one. The difference
 * between the two uses is just a matter of which messages to present to the
 * user and whether the "Ok" button starts enabled or not.
 */
public class PathVariableDialog extends TitleAreaDialog {

    // UI widgets
    private Button okButton;

    private Label variableNameLabel;

    private Label variableValueLabel;

    private Text variableNameField;

    private Text variableValueField;

    private Button fileButton;

    private Button folderButton;

    /**
     * This dialog type: <code>NEW_VARIABLE</code> or
     * <code>EXISTING_VARIABLE</code>.
     */
    private int type;

    /**
     * The type of variable that can be edited in this dialog.
     * <code>IResource.FILE</code> or <code>IResource.FOLDER</code>
     */
    private int variableType;

    /**
     * The name of the variable being edited.
     */
    private String variableName;

    /**
     * The value of the variable being edited.
     */
    private String variableValue;

    /**
     * The original name of the variable being edited. It is used when testing
     * if the current variable's name is already in use.
     */
    private String originalName;

    /**
     * Used to select the proper message depending on the current mode
     * (new/existing variable).
     */
    private boolean newVariable;

    /**
     * Reference to the path variable manager. It is used for validating
     * variable names.
     */
    private IPathVariableManager pathVariableManager;

    /**
     * Set of variable names currently in use. Used when warning the user that
     * the currently selected name is already in use by another variable.
     */
    private Set namesInUse;

    /**
     * The current validation status. Its value can be one of the following:<ul>
     * <li><code>IMessageProvider.NONE</code> (default);</li>
     * <li><code>IMessageProvider.WARNING</code>;</li>
     * <li><code>IMessageProvider.ERROR</code>;</li>
     * </ul>
     * Used when validating the user input.
     */
    private int validationStatus;

    /**
     * The current validation message generated by the last
     * call to a <code>validate</code> method.
     */
    private String validationMessage;

    /**
     * Whether a variable name has been entered.  
     */
    private boolean nameEntered = false;

    /**
     * Whether a variable location has been entered.  
     */
    private boolean locationEntered = false;

    /**
     * The standard message to be shown when there are no problems being
     * reported.
     */
    final private String standardMessage;

    /**
     * Constant for defining this dialog as intended to create a new variable
     * (value = 1).
     */
    public final static int NEW_VARIABLE = 1;

    /**
     * Constant for defining this dialog as intended to edit an existing
     * variable (value = 2).
     */
    public final static int EXISTING_VARIABLE = 2;

    /**
     * Constructs a dialog for editing a new/existing path variable.
     * 
     * @param parentShell the parent shell
     * @param type the dialog type: <code>NEW_VARIABLE</code> or
     * 	<code>EXISTING_VARIABLE</code>
     * @param variableType the type of variable that can be edited in 
     * 	this dialog. <code>IResource.FILE</code> or <code>IResource.FOLDER</code>
     * @param pathVariableManager a reference to the path variable manager
     * @param namesInUse a set of variable names currently in use 
     */
    public PathVariableDialog(Shell parentShell, int type, int variableType,
            IPathVariableManager pathVariableManager, Set namesInUse) {
        super(parentShell);
        this.type = type;
        this.newVariable = type == NEW_VARIABLE;
        this.variableName = ""; //$NON-NLS-1$
        this.variableValue = ""; //$NON-NLS-1$
        this.variableType = variableType;
        this.pathVariableManager = pathVariableManager;
        this.namesInUse = namesInUse;

        if (newVariable) {
			this.standardMessage = IDEWorkbenchMessages.PathVariableDialog_message_newVariable;
		} else {
			this.standardMessage = IDEWorkbenchMessages.PathVariableDialog_message_existingVariable;
		}
    }

    /**
     * Configures this dialog's shell, setting the shell's text.
     * 
     * @see org.eclipse.jface.window.Window#configureShell(Shell)
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (newVariable) {
			shell.setText(IDEWorkbenchMessages.PathVariableDialog_shellTitle_newVariable);
		} else {
			shell
                    .setText(IDEWorkbenchMessages.PathVariableDialog_shellTitle_existingVariable);
		}
    }

    /**
     * Creates and returns the contents of this dialog (except for the button bar).
     * 
     * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea
     */
    protected Control createDialogArea(Composite parent) {
        // top level composite
        Composite parentComposite = (Composite) super.createDialogArea(parent);

        initializeDialogUnits(parentComposite);
        
        // creates dialog area composite
        Composite contents = createComposite(parentComposite);

        // creates and lay outs dialog area widgets 
        createWidgets(contents);

        // validate possibly already incorrect variable definitions
        if (type == EXISTING_VARIABLE) {
            nameEntered = locationEntered = true;
            validateVariableValue();
        }

        Dialog.applyDialogFont(parentComposite);
        
        return contents;
    }

    /**
     * Creates and configures this dialog's main composite.
     * 
     * @param parentComposite parent's composite
     * @return this dialog's main composite
     */
    private Composite createComposite(Composite parentComposite) {
        // creates a composite with standard margins and spacing
        Composite contents = new Composite(parentComposite, SWT.NONE);

        FormLayout layout = new FormLayout();

        contents.setLayout(layout);
        contents.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (newVariable) {
			setTitle(IDEWorkbenchMessages.PathVariableDialog_dialogTitle_newVariable);
		} else {
			setTitle(IDEWorkbenchMessages.PathVariableDialog_dialogTitle_existingVariable);
		}
        setMessage(standardMessage);
        return contents;
    }

    /**
     * Creates widgets for this dialog.
     * 
     * @param contents the parent composite where to create widgets
     */
    private void createWidgets(Composite contents) {
        FormData data;

        String nameLabelText = IDEWorkbenchMessages.PathVariableDialog_variableName;
        String valueLabelText = IDEWorkbenchMessages.PathVariableDialog_variableValue;

        // variable name label
        variableNameLabel = new Label(contents, SWT.LEFT);
        variableNameLabel.setText(nameLabelText);

        data = new FormData();
        data.top = new FormAttachment(0,
        		convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN));
        data.left = new FormAttachment(0,
        		convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN));
        variableNameLabel.setLayoutData(data);
     
        // variable name field.  Attachments done after all widgets created.
        variableNameField = new Text(contents, SWT.SINGLE | SWT.BORDER);
        variableNameField.setText(variableName);
        variableNameField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                variableNameModified();
            }
        });
        
        // variable value label
        variableValueLabel = new Label(contents, SWT.LEFT);
        variableValueLabel.setText(valueLabelText);

        data = new FormData();
        data.left = new FormAttachment(0,
        		convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN));
        data.top = new FormAttachment(variableNameLabel,
                convertVerticalDLUsToPixels(5));
        variableValueLabel.setLayoutData(data);

        // variable value field.  Attachments done after all widgets created.
        variableValueField = new Text(contents, SWT.SINGLE | SWT.BORDER);
        variableValueField.setText(variableValue);
        variableValueField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                variableValueModified();
            }
        });

        // select file path button
        fileButton = new Button(contents, SWT.PUSH);
        fileButton.setText(IDEWorkbenchMessages.PathVariableDialog_file);
        if ((variableType & IResource.FILE) == 0) {
			fileButton.setEnabled(false);
		}

        data = setButtonFormLayoutData(fileButton);
        data.top = new FormAttachment(variableValueLabel, 0, SWT.CENTER);
        data.right = new FormAttachment(100,
        		-convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN));        
        fileButton.setLayoutData(data);

        fileButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                selectFile();
            }
        });

        // select folder path button
        folderButton = new Button(contents, SWT.PUSH);
        folderButton.setText(IDEWorkbenchMessages.PathVariableDialog_folder);
        if ((variableType & IResource.FOLDER) == 0) {
			folderButton.setEnabled(false);
		}

        data = setButtonFormLayoutData(folderButton);
        data.top = new FormAttachment(fileButton, convertVerticalDLUsToPixels(2));
        data.right = new FormAttachment(100,
        		-convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN));
        folderButton.setLayoutData(data);

        folderButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                selectFolder();
            }
        });
        
        // Attaching variable name and value fields to file and folder buttons,
        // so do this now that those buttons have been created.
        
        // the larger label will be used in the left attachments for the fields  
        Label largerLabel = nameLabelText.length() > valueLabelText.length() ? variableNameLabel
                : variableValueLabel;
 
        data = new FormData();
        data.left = new FormAttachment(largerLabel,
                convertHorizontalDLUsToPixels(5));
        data.right = new FormAttachment(fileButton, -convertHorizontalDLUsToPixels(5));
        data.top = new FormAttachment(variableNameLabel,
                convertVerticalDLUsToPixels(5), SWT.CENTER);
        variableNameField.setLayoutData(data);
        

        data = new FormData();
        data.left = new FormAttachment(largerLabel,
                convertHorizontalDLUsToPixels(5));
        data.right = new FormAttachment(fileButton, -convertHorizontalDLUsToPixels(5));
        data.top = new FormAttachment(variableValueLabel, 0, SWT.CENTER);
        variableValueField.setLayoutData(data);

  
 
    }

    /**
     * Sets the <code>FormData</code> on the specified button to be one that is
     * spaced for the current dialog page units. The method
     * <code>initializeDialogUnits</code> must be called once before calling this
     * method for the first time.
     * 
     * @param button the button to set the <code>FormData</code>
     * @return the <code>FormData</code> set on the specified button
     */
    private FormData setButtonFormLayoutData(Button button) {
        FormData data = new FormData();
        int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        data.width = Math.max(widthHint, button.computeSize(SWT.DEFAULT,
                SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        return data;
    }

    /**
     * Fires validations (variable name first) and updates enabled state for the
     * "Ok" button accordingly.
     */
    private void variableNameModified() {
        // updates and validates the variable name
        variableName = variableNameField.getText();
        validationStatus = IMessageProvider.NONE;
        okButton.setEnabled(validateVariableName() && validateVariableValue());
        nameEntered = true;
    }

    /**
     * Fires validations (variable value first) and updates enabled state for the
     * "Ok" button accordingly.
     */
    private void variableValueModified() {
        // updates and validates the variable value
        variableValue = variableValueField.getText().trim();
        validationStatus = IMessageProvider.NONE;
        okButton.setEnabled(validateVariableValue() && validateVariableName());
        locationEntered = true;
    }

    /**
     * Opens a dialog where the user can select a folder path.
     */
    private void selectFolder() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setText(IDEWorkbenchMessages.PathVariableDialog_selectFolderTitle);
        dialog.setMessage(IDEWorkbenchMessages.PathVariableDialog_selectFolderMessage);
        dialog.setFilterPath(variableValue);
        String res = dialog.open();
        if (res != null) {
            variableValue = new Path(res).makeAbsolute().toOSString();
            variableValueField.setText(variableValue);
        }
    }

    /**
     * Opens a dialog where the user can select a file path.
     */
    private void selectFile() {
        FileDialog dialog = new FileDialog(getShell());
        dialog.setText(IDEWorkbenchMessages.PathVariableDialog_selectFileTitle);
        dialog.setFilterPath(variableValue);
        String res = dialog.open();
        if (res != null) {
            variableValue = new Path(res).makeAbsolute().toOSString();
            variableValueField.setText(variableValue);
        }
    }

    /**
     * Adds buttons to this dialog's button bar.
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar
     */
    protected void createButtonsForButtonBar(Composite parent) {
        okButton = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        okButton.setEnabled(type == EXISTING_VARIABLE);

        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * Validates the current variable name, and updates this dialog's message.
     * 
     * @return true if the name is valid, false otherwise
     */
    private boolean validateVariableName() {
        boolean allowFinish = false;

        // if the current validationStatus is ERROR, no additional validation applies
        if (validationStatus == IMessageProvider.ERROR) {
			return false;
		}

        // assumes everything will be ok
        String message = standardMessage;
        int newValidationStatus = IMessageProvider.NONE;

        if (variableName.length() == 0) {
            // the variable name is empty
            if (nameEntered) {
                // a name was entered before and is now empty
                newValidationStatus = IMessageProvider.ERROR;
                message = IDEWorkbenchMessages.PathVariableDialog_variableNameEmptyMessage;
            }
        } else {
            IStatus status = pathVariableManager.validateName(variableName);
            if (!status.isOK()) {
                // the variable name is not valid
                newValidationStatus = IMessageProvider.ERROR;
                message = status.getMessage();
            } else if (namesInUse.contains(variableName)
                    && !variableName.equals(originalName)) {
                // the variable name is already in use
                message = IDEWorkbenchMessages.PathVariableDialog_variableAlreadyExistsMessage;
                newValidationStatus = IMessageProvider.ERROR;
            } else {
                allowFinish = true;
            }
        }

        // overwrite the current validation status / message only if everything is ok (clearing them)
        // or if we have a more serious problem than the current one
        if (validationStatus == IMessageProvider.NONE
                || newValidationStatus == IMessageProvider.ERROR) {
            validationStatus = newValidationStatus;
            validationMessage = message;
        }
        // only set the message here if it is not going to be set in 
        // validateVariableValue to avoid flashing.
        if (allowFinish == false) {
			setMessage(validationMessage, validationStatus);
		}
        return allowFinish;
    }

    /**
     * Validates the current variable value, and updates this dialog's message.
     * 
     * @return true if the value is valid, false otherwise
     */
    private boolean validateVariableValue() {
        boolean allowFinish = false;

        // if the current validationStatus is ERROR, no additional validation applies
        if (validationStatus == IMessageProvider.ERROR) {
			return false;
		}

        // assumes everything will be ok
        String message = standardMessage;
        int newValidationStatus = IMessageProvider.NONE;

        if (variableValue.length() == 0) {
            // the variable value is empty
            if (locationEntered) {
                // a location value was entered before and is now empty
                newValidationStatus = IMessageProvider.ERROR;
                message = IDEWorkbenchMessages.PathVariableDialog_variableValueEmptyMessage;
            }
        } else if (!Path.EMPTY.isValidPath(variableValue)) {
            // the variable value is an invalid path
            message = IDEWorkbenchMessages.PathVariableDialog_variableValueInvalidMessage;
            newValidationStatus = IMessageProvider.ERROR;
        } else if (!new Path(variableValue).isAbsolute()) {
            // the variable value is a relative path
            message = IDEWorkbenchMessages.PathVariableDialog_pathIsRelativeMessage;
            newValidationStatus = IMessageProvider.ERROR;
        } else if (!IDEResourceInfoUtils.exists(variableValue)) {
            // the path does not exist (warning)
            message = IDEWorkbenchMessages.PathVariableDialog_pathDoesNotExistMessage;
            newValidationStatus = IMessageProvider.WARNING;
            allowFinish = true;
        } else {
            allowFinish = true;
        }

        // overwrite the current validation status / message only if everything is ok (clearing them)
        // or if we have a more serious problem than the current one
        if (validationStatus == IMessageProvider.NONE
                || newValidationStatus > validationStatus) {
            validationStatus = newValidationStatus;
            validationMessage = message;
        }
        setMessage(validationMessage, validationStatus);
        return allowFinish;
    }

    /**
     * Returns the variable name.
     * 
     * @return the variable name
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Returns the variable value.
     * 
     * @return the variable value
     */
    public String getVariableValue() {
        return variableValue;
    }

    /**
     * Sets the variable name.
     * 
     * @param variableName the new variable name
     */
    public void setVariableName(String variableName) {
        this.variableName = variableName.trim();
        this.originalName = this.variableName;
    }

    /**
     * Sets the variable value.
     * 
     * @param variableValue the new variable value
     */
    public void setVariableValue(String variableValue) {
        this.variableValue = variableValue;
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#isResizable()
     */
    protected boolean isResizable() {
    	return true;
    }

}
