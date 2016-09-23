package org.dcm4chee.web.war.tc;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.dcm4chee.web.war.tc.TCEditableObject.SaveResult;
import org.dcm4chee.web.war.tc.TCResultPanel.ITCCaseProvider;

@SuppressWarnings("serial")
public class TCViewDialog extends ModalWindow {

	private static final Logger log = Logger.getLogger(TCViewDialog.class);

	private ITCViewDialogCloseCallback closeCallback;
	
	public TCViewDialog(final String id) {
		super(id);
	}
	
    @Override
	protected boolean makeContentVisible()
	{
		return isShown();
 	}   
    
    @Override
    public final void show(AjaxRequestTarget target)
    {
        if (isShown()==false)
        {
            Component content = getContent();
            
            content.setVisible(true);
            
            target.addComponent(this);
            target.appendJavascript(getWindowOpenJavascript().replace(
                    "Wicket.Window.create", "createTCViewDialog"));
            
            target.appendJavascript("updateTCViewDialog();");
            
            if (content instanceof TCViewPanel)
            {
                //disable tabs
                TCViewPanel viewPanel = (TCViewPanel) content;
                
                if (!viewPanel.isEditable())
                {
                    target.appendJavascript(viewPanel.getDisableTabsJavascript());
                }
                
                //hide tabs
                target.appendJavascript(viewPanel.getHideTabsJavascript());
            }
            
            try
            {
                Field shown = ModalWindow.class.getDeclaredField("shown");
                shown.setAccessible(true);
                shown.set(this, true);
            }
            catch (Exception e)
            {
                log.warn(null, e);
            }
        }
    }
        
    @Override
    public final void close(AjaxRequestTarget target) {
    	closeImpl(target, false);
    }
        
    public void open(String title, AjaxRequestTarget target, TCModel tcModel, 
    		IModel<Boolean> trainingModeModel, ITCCaseProvider caseProvider, 
    		boolean editable, ITCViewDialogCloseCallback callback)
    {
        if (target==null)
        {
            target = AjaxRequestTarget.get();
        }
        
        if (target!=null)
        {
        	closeCallback = callback;
        	
            setContent(editable ? 
            		createEditableView(tcModel, trainingModeModel, caseProvider) :
            	createView(tcModel, trainingModeModel, caseProvider));
            
            setTitle(title==null?"":title);
            setInitialWidth(1024);
            setInitialHeight(780);
            setResizable(true);
            
            show(target);
        }
    }
        
    public TCViewPanel getView() {
    	Component c = getContent();
    	if (c instanceof TCViewPanel) {
    		return (TCViewPanel) c;
    	}
    	return null;
    }
    
    private TCViewPanel createView(TCModel tcModel, 
    		IModel<Boolean> trainingModeModel, ITCCaseProvider caseProvider) {
    	return new TCViewPanel(getContentId(), tcModel, 
        		new TCAttributeVisibilityStrategy(new AbstractReadOnlyModel<Boolean>() {
        			@Override
        			public Boolean getObject() {
        				return false;
        			}
        		}, trainingModeModel), caseProvider);
    }
    
    
    private TCViewEditablePanel createEditableView(TCModel tcModel, 
    		IModel<Boolean> trainingModeModel, ITCCaseProvider caseProvider) {
    	return new TCViewEditablePanel(getContentId(), tcModel, 
        		new TCAttributeVisibilityStrategy(new AbstractReadOnlyModel<Boolean>() {
        			@Override
        			public Boolean getObject() {
        				return true;
        			}
        		}, trainingModeModel), caseProvider) {
			@Override
			protected void onClose(AjaxRequestTarget target, boolean save)
			{
			    closeImpl(target, save);
			}
    	};
    }
    
    private TCEditableObject getTC() {
    	TCViewPanel view = getView();
    	if (view!=null) {
    		return view.getTC();
    	}
    	return null;
    }
        
    private void closeImpl(AjaxRequestTarget target, boolean saveChanges) {
    	super.close(target);
    	
    	TCEditableObject tc = getTC();
    	
    	SaveResult result = null;
    	
    	if (tc!=null) {
		    if (saveChanges)
		    {
		        try
		        {
		            result = tc.save();
		        }
		        catch (Exception e)
		        {
		            log.error("Saving teaching-file failed!", e);
		        }
		    }
	
		    if (result!=null && result.saved()) {
		    	TCAuditLog.logTFEdited(tc);
		    }
		    else {
		    	TCAuditLog.logTFViewed(tc);
		    }
    	}
    	
    	if (closeCallback!=null) {
    		closeCallback.dialogClosed(target, tc, result);
    	}
    }
    
    
	public static interface ITCViewDialogCloseCallback extends Serializable {
		void dialogClosed(AjaxRequestTarget target, TCEditableObject tc, SaveResult result);
	}
}
