package org.dcm4chee.web.war.tc.keywords;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.dcm4chee.web.dao.tc.ITextOrCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.tc.TCInput;

public abstract class AbstractTCInput extends Panel implements TCInput 
{
	private static final long serialVersionUID = 2482055678931466780L;
	
	private static final String MULTIPLE_KEYWORD_DELIMITER = ";";
	
	private boolean usedForSearch;
	private TCQueryFilterKey filterKey;
    private List<ValueChangeListener> changeListener;
    
    public AbstractTCInput(final String id, TCQueryFilterKey filterKey, boolean usedForSearch)
    {
        super(id);
        this.usedForSearch = usedForSearch;
        this.filterKey = filterKey;
    }
    
    @Override
    public Panel getComponent() 
    {
        return this;
    }
    
    @Override
    public TCQueryFilterKey getFilterKey()
    {
    	return filterKey;
    }
    
    @Override
    public ITextOrCode getValue()
    {
    	ITextOrCode[] values = getValues();
    	return values!=null && values.length>0 ? values[0] : null;
    }
    
    @Override
    public boolean isUsedForSearch()
    {
    	return usedForSearch;
    }
    
    public boolean isMultipleKeywordSearchEnabled()
    {
    	return TCQueryFilterKey.Keyword.equals(filterKey) &&
    			WebCfgDelegate.getInstance().isTCMultipleKeywordSearchEnabled() &&
    			isUsedForSearch();
    }
    
    public String getMultipleKeywordDelimiter()
    {
    	return MULTIPLE_KEYWORD_DELIMITER;
    }

    protected void fireValueChanged()
    {
        if (changeListener!=null)
        {
            for (ValueChangeListener l : changeListener)
            {
                l.valueChanged(getValues());
            }
        }
    }

    @Override
    public void removeChangeListener(ValueChangeListener l) {
        if (changeListener!=null)
        {
            changeListener.remove(l);
        }
    }

    @Override
    public void addChangeListener(ValueChangeListener l) {
        if (changeListener==null)
        {
            changeListener = new ArrayList<ValueChangeListener>();
        }
        if (!changeListener.contains(l))
        {
            changeListener.add(l);
        }
    }
    
    protected abstract class MultipleItemsTextModel extends Model<String>
    {
		private static final long serialVersionUID = 565772747585574478L;

		public MultipleItemsTextModel()
		{
			this(null);
		}
		
		public MultipleItemsTextModel(String text)
		{
			super(text);
		}
		
		public List<String> getStringItems()
    	{
        	String text = getObject();
        	List<String> list = null;
        	
        	if (text!=null && !text.isEmpty())
        	{
    	    	if (isMultipleKeywordSearchEnabled())
    	    	{
    	    		String[] items = text.split("\\"+getMultipleKeywordDelimiter());
    	    		if (items!=null)
    	    		{
    	    			list = new ArrayList<String>(3);
    	    			for (String item : items)
    	    			{
    	    				if (item!=null)
    	    				{
    		    				String trimmed = item.trim();
    		    				if (trimmed.length()>0)
    		    				{
    		    					list.add(trimmed);
    		    				}
    	    				}
    	    			}
    	    		}
    	    	}
    	    	else
    	    	{
    	    		list = Collections.singletonList(text);
    	    	}
        	}
        	
        	return list;
    	}
		
    	protected String toString(List<? extends ITextOrCode> items)
    	{
    		if (items!=null)
    		{
    			StringBuilder sbuilder = new StringBuilder();
    			for (int i=0; i<items.size(); i++)
    			{
    				ITextOrCode item = items.get(i);
    				if (item!=null)
    				{
    					if (i>0)
    					{
    						sbuilder.append(getMultipleKeywordDelimiter());
    					}
    					sbuilder.append(item);
    				}
    			}
    			return sbuilder.toString();
    		}
    		return null;
    	}
    }
}
