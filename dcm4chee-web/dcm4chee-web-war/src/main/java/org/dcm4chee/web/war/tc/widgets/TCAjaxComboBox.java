package org.dcm4chee.web.war.tc.widgets;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.web.war.tc.TCUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public abstract class TCAjaxComboBox<T extends Serializable> extends TCComboBox<T> {

	private static final Logger log = LoggerFactory.getLogger(TCAjaxComboBox.class);
	
	private static final String CALLBACK_URL_KEY = "wicket-callback-url";
	private static final String INITIAL_VALUE_KEY = "initial-value";
	
	private AbstractAjaxBehavior selectionChangedBehavior;
	
	public TCAjaxComboBox(final String id, List<? extends T> options)
	{
		this(id, options, (T) null, null);
	}
	
	public TCAjaxComboBox(final String id, List<? extends T> options, IChoiceRenderer<T> renderer)
	{
		this(id, options, (T) null, renderer);
	}
	
	public TCAjaxComboBox(final String id, List<? extends T> options, T selectedValue)
	{
		this(id, options, selectedValue, null);
	}
	
	public TCAjaxComboBox(final String id, List<? extends T> options, 
			T selectedValue, IChoiceRenderer<T> renderer)
	{
		this(id, options, new Model<T>(selectedValue), renderer);
	}
	
	public TCAjaxComboBox(final String id, List<? extends T> options, 
			IModel<T> selectedValue, IChoiceRenderer<T> renderer)
	{
		super(id, options, selectedValue, renderer);
		add(selectionChangedBehavior=new SelectionChangedBehavior());
	}
	
    @Override
    protected void onComponentTag(ComponentTag tag)
    {
    	super.onComponentTag(tag);

    	tag.put(CALLBACK_URL_KEY, selectionChangedBehavior.getCallbackUrl());
    	tag.put(INITIAL_VALUE_KEY, getDefaultModelObjectAsString());
    }
    
    @Override
    public void validate() {
    	/* don't include AJAX based component in default form processing */
    }
    
    @Override
    public void updateModel() {
    	/* don't include AJAX based component in default form processing */
    }
	
	protected T convertValue(String value) throws Exception {
    	if (value!=null) {
    		IChoiceRenderer<? super T> renderer = getChoiceRenderer();
    		List<? extends T> options = getChoices();
    		for (T option : options) {
    			if (value.equals(renderer.getDisplayValue(option).toString())) {
    				return option;
    			}
    		}
    	}
    	return null;
	}
	
	protected boolean shallCommitValue(T oldValue, T newValue, AjaxRequestTarget target) {
		 return !TCUtilities.equals(oldValue, newValue);
	}
	
	protected void valueChanged(T newValue, T oldValue, AjaxRequestTarget target) {
		valueChanged( newValue );
	}
	
    private class SelectionChangedBehavior extends AbstractDefaultAjaxBehavior {
		@SuppressWarnings("unchecked")
		@Override
    	public void respond(AjaxRequestTarget target) {
			try {
	    		String valueString = RequestCycle.get().getRequest().getParameter("selectedValue");
	    		T value = convertValue(valueString);
	    		if (value==null && valueString!=null) {
	    			String valueString2 = new String(valueString.getBytes("ISO-8859-1"),"UTF-8");
	    			value = convertValue(valueString2);
	    		}

	    		if (shallCommitValue((T)getDefaultModelObject(), value, target)) {
	    			T oldValue = getModelObject();
	    			setDefaultModelObject(value);
		    		valueChanged(value, oldValue, target);
	    		}
			}
			catch (Exception e) {
				log.error("Unable to update model: Value conversion failed!", e);
			}
    	}
    }
}
