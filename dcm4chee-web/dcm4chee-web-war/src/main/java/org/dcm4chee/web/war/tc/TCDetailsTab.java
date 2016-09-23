package org.dcm4chee.web.war.tc;

import org.apache.wicket.markup.html.panel.Panel;

@SuppressWarnings("serial")
public abstract class TCDetailsTab extends Panel {
	
	private TCAttributeVisibilityStrategy attrVisibilityStrategy;
	
    public TCDetailsTab(final String id, 
    		TCAttributeVisibilityStrategy attrVisibilityStrategy) {
        super(id);
        this.attrVisibilityStrategy = attrVisibilityStrategy;
    }
    
    public boolean enabled() {
    	return true;
    }
    
    public boolean visible() {
    	return true;
    }
    
    public final TCAttributeVisibilityStrategy getAttributeVisibilityStrategy() {
    	return attrVisibilityStrategy;
    }
}