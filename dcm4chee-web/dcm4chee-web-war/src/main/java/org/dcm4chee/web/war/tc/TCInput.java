package org.dcm4chee.web.war.tc;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.dcm4chee.web.dao.tc.ITextOrCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;

public interface TCInput extends Serializable {
    
	public boolean isUsedForSearch();
	
	public TCQueryFilterKey getFilterKey();
	
	public ITextOrCode getValue();
	
    public ITextOrCode[] getValues();
    
    public void setValues(ITextOrCode...values);

    public Component getComponent();

    public void addChangeListener(ValueChangeListener l);
    
    public void removeChangeListener(ValueChangeListener l);
    
    public static interface ValueChangeListener extends Serializable
    {
        public void valueChanged(ITextOrCode[] value);
    }
}