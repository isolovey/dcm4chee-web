package org.dcm4chee.usr.ui.validator;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class ValidatorMessageLabel extends Label {

    private static final long serialVersionUID = 1L;

    private FormComponent<?> component;

    private IModel<?> text = null;

    public ValidatorMessageLabel(String id, FormComponent<?> component) {
        super(id);
        this.component = component;
    }

    public ValidatorMessageLabel(String id, FormComponent<?> component, String text) {
        this(id, component, new Model<String>(text));
    }

    public ValidatorMessageLabel(String id, FormComponent<?> component, IModel<?> iModel) {
        super(id);
        this.component = component;
        this.text=iModel;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        this.setDefaultModel(null);
        if (this.component.getFeedbackMessage() != null) {
            if (this.text != null)
                this.setDefaultModel(new Model<String>(this.text.getObject().toString().replaceAll("\"", "")));
            else
                this.setDefaultModel(new Model<String>(component.getFeedbackMessage().getMessage().toString().replaceAll("\"", "")));

            // set this message rendered to avoid the warning concerning the feedback panel
            component.getFeedbackMessage().markRendered();           
            this.add(new AttributeModifier("class", true, new Model<String>("message-validator")));
        } else
            this.setDefaultModel(null);
    }
}
