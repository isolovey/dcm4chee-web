package org.dcm4chee.web.war.tc.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.dcm4chee.icons.ImageManager;

@SuppressWarnings("serial")
public class TCMaxImageSizeBehavior extends AbstractBehavior {
    private int width = ImageManager.defaultWidth;
    private int height = ImageManager.defaultHeight;

    private String additionalCSS;

    public TCMaxImageSizeBehavior(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public TCMaxImageSizeBehavior setAdditionalCSS(String css) {
    	this.additionalCSS = css;
    	return this;
    }
    
    public void onComponentTag(Component c, ComponentTag tag) {
        tag.put("style", "max-width: " + this.width + "px; max-height: " + this.height + "px;" 
                + (this.additionalCSS == null ? "" : " " + this.additionalCSS));
    }    
}
