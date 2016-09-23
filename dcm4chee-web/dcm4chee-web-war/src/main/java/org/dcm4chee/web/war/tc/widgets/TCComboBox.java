package org.dcm4chee.web.war.tc.widgets;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class TCComboBox<T extends Serializable> extends DropDownChoice<T> implements IHeaderContributor {
	private static final long serialVersionUID = 4036772216132377000L;
	
	private static final String COMBOBOX_CLASS = "ui-combobox";

	public TCComboBox(final String id, List<? extends T> options)
	{
		this(id, options, (T) null, null);
	}
	
	public TCComboBox(final String id, List<? extends T> options, IChoiceRenderer<T> renderer)
	{
		this(id, options, (T) null, renderer);
	}
	
	public TCComboBox(final String id, List<? extends T> options, T selectedValue)
	{
		this(id, options, selectedValue, null);
	}
	
	public TCComboBox(final String id, List<? extends T> options, 
			T selectedValue, IChoiceRenderer<T> renderer)
	{
		this(id, options, new Model<T>(selectedValue), renderer);
	}
	
	public TCComboBox(final String id, List<? extends T> options, 
			IModel<T> selectedValue, IChoiceRenderer<T> renderer)
	{
		super(id, selectedValue, options, renderer);

		setOutputMarkupId(true);
		add(new AttributeAppender("class",new Model<String>(COMBOBOX_CLASS)," "));
	}
	
    public void renderHead(IHeaderResponse response) {
        // commented out: already included in the html header of TCPanel.html
    	//response.renderJavascriptReference(new ResourceReference(TCComboBox.class, "js/tc-jquery-ui.js"));
    }
    
    protected void valueChanged(T value) {}
    
    @Override
	protected boolean wantOnSelectionChangedNotifications()
	{
		return true;
	}
    
    @Override
	protected final void onSelectionChanged(final T newSelection)
	{
    	setModelObject(newSelection);
    	valueChanged(newSelection);
	}

}
