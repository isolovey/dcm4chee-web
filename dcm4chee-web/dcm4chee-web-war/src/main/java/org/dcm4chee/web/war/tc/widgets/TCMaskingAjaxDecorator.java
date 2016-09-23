package org.dcm4chee.web.war.tc.widgets;

import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;

@SuppressWarnings("serial")
public class TCMaskingAjaxDecorator extends AjaxCallDecorator
{

	private String parentId;
	private boolean showMask = false;
	private boolean showWaitCursor = true;
	
	public TCMaskingAjaxDecorator(boolean showMask, boolean showWaitCursor)
	{
	    this(null, showMask, showWaitCursor);
	}
	
	public TCMaskingAjaxDecorator(String parentId, boolean showMask, boolean showWaitCursor)
	{
		this.parentId = parentId;
		this.showMask = showMask;
		this.showWaitCursor = showWaitCursor;
	}
	
	public boolean isMaskVisible() {
		return showMask;
	}
	
	public boolean isWaitCursorVisible() {
		return showWaitCursor;
	}
	
	public void setMaskVisible(boolean visible) {
		showMask = visible;
	}
	
	public void setWaitCursorVisible(boolean visible) {
		showWaitCursor = visible;
	}
	
	@Override
	public CharSequence decorateScript(CharSequence script)
	{
		if (parentId==null)
		{
		    if (!showMask)
		    {
		        return "Mask.showTransparent(null, " + showWaitCursor + ");" + script;
		    }
		    else
		    {
		        return "Mask.show(null, " + showWaitCursor + ");" + script;
		    }
		}
		else
		{
		    if (!showMask)
		    {
		        return "Mask.showTransparent('" + parentId + "', " + showWaitCursor + ");" + script;
		    }
		    else
		    {
		        return "Mask.show('" + parentId + "', " + showWaitCursor + ");" + script;
		    }
		}
	}

	@Override
	public CharSequence decorateOnSuccessScript(CharSequence script)
	{
		return "Mask.hide();" + script;
	}

	@Override
	public CharSequence decorateOnFailureScript(CharSequence script)
	{
		return "Mask.hide();" + script;
	}
}
