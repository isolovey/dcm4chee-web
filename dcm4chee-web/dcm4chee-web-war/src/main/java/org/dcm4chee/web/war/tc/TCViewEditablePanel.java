package org.dcm4chee.web.war.tc;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.war.tc.TCResultPanel.ITCCaseProvider;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 25, 2011
 */
@SuppressWarnings("serial")
public abstract class TCViewEditablePanel extends TCViewPanel
{
    public TCViewEditablePanel(final String id, final TCModel tc, 
    		TCAttributeVisibilityStrategy attrVisibilityStrategy, ITCCaseProvider caseProvider)
    {
        super(id, tc, attrVisibilityStrategy, caseProvider);
        
        add(new AjaxLink<Void>("tc-view-ok-btn") {
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                onClose(target, true);
            }
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
            	return new IAjaxCallDecorator() {
					private static final long serialVersionUID = -6741509384950051556L;
					public final CharSequence decorateScript(CharSequence script) {
	                    return "$(this).attr('disabled','disabled'); if(typeof showMask == 'function') { showMask(); };"+script;
	                }
	            
	                public final CharSequence decorateOnSuccessScript(CharSequence script) {
	                    return "hideMask();"+script;
	                }
	            
	                public final CharSequence decorateOnFailureScript(CharSequence script) {
	                    return "hideMask();"+script;
	                }
            	};
            }
        }
        .add(new Image("tc-view-ok-img", ImageManager.IMAGE_TC_ACKNOWLEDGE)
        .add(new ImageSizeBehaviour("vertical-align: middle;")))
        .add(new Label("tc-view-ok-text", new ResourceModel("tc.view.ok.text")))
        .setMarkupId("tc-view-ok-btn"));
        
        add(new AjaxLink<Void>("tc-view-cancel-btn") {
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                onClose(target, false);
            }
        }
        .add(new Image("tc-view-cancel-img", ImageManager.IMAGE_TC_CANCEL)
        .add(new ImageSizeBehaviour("vertical-align: middle;")))
        .add(new Label("tc-view-cancel-text", new ResourceModel("tc.view.cancel.text")))
        .setMarkupId("tc-view-cancel-btn"));
    }
    
    @Override
    public boolean isEditable()
    {
        return true;
    }
    
    protected abstract void onClose(AjaxRequestTarget target, boolean save);
}
