package com.tamaygz.colorfuldiag.ui;

import javax.swing.JComponent;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.tamaygz.colorfuldiag.model.ContainerInfo;

/**
 * Simple dialog for renaming a container.
 */
public class RenameContainerDialog extends DialogWrapper {
    private final JBTextField titleField;
    private final ContainerInfo container;

    public RenameContainerDialog(@Nullable Project project, ContainerInfo container) {
        super(project);
        this.container = container;
        this.titleField = new JBTextField(container.getTitle() != null ? container.getTitle() : "");
        titleField.setColumns(25);
        
        setTitle("Rename Container");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Container name:", titleField)
                .getPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return titleField;
    }

    public String getNewTitle() {
        return titleField.getText().trim();
    }

    public void applyToContainer() {
        container.setTitle(getNewTitle());
    }
}
