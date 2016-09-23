package org.dcm4chee.web.war.tc.keywords;

import java.util.ArrayList;
import java.util.List;

import org.dcm4chee.web.dao.tc.ITextOrCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;

public abstract class AbstractTCKeywordInput extends AbstractTCInput implements TCKeywordInput 
{
	private static final long serialVersionUID = 8282801535686632421L;

	private boolean exclusive;
	
	public AbstractTCKeywordInput(final String id, TCQueryFilterKey filterKey, 
			boolean usedForSearch, boolean exclusive)
    {
        super(id, filterKey, usedForSearch);
        this.exclusive = exclusive;
    }
	
    @Override
    public boolean isExclusive()
    {
        return exclusive;
    }
	
	@Override
	public TCKeyword getKeyword()
	{
		TCKeyword[] keywords = getKeywords();
		return keywords!=null && keywords.length>0 ? keywords[0] : null;
	}
	
	@Override
	public TCKeyword getValue()
	{
		return getKeyword();
	}
    
    @Override
    public TCKeyword[] getValues()
    {
        return getKeywords();
    }
    
    @Override
    public void setValues(ITextOrCode...values) {
    	List<TCKeyword> keywords = new ArrayList<TCKeyword>(3);
    	if (values!=null) {
    		for (ITextOrCode toc : values) {
    			TCKeyword keyword = TCKeyword.create(
    					TCKeywordCatalogueProvider.getInstance().getCatalogue(
    							getFilterKey()), toc);
    			if (keyword!=null) {
    				keywords.add(keyword);
    			}
    		}
    	}
    	
    	if (keywords!=null && !keywords.isEmpty()) {
    		setKeywords(keywords.toArray(new TCKeyword[0]));
    	}
    	else
    	{
    		setKeywords();
    	}
    }
}
