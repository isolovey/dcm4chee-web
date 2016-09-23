package org.dcm4chee.web.war.tc;

import java.awt.Point;
import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.dcm4chee.web.war.tc.TCPopupManager.TCPopupPosition.PopupAlign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Dec 13, 2011
 */
@SuppressWarnings("serial")
public class TCPopupManager implements Serializable
{
    private static Logger log = LoggerFactory.getLogger(TCPopupManager.class);

    private AbstractTCPopup curPopupShown;
    
    private HideOnOutsideClickBehavior globalHideOnOutsideClickHandler =
        new HideOnOutsideClickBehavior();
        
    public AbstractAjaxBehavior getGlobalHideOnOutsideClickHandler()
    {
        return globalHideOnOutsideClickHandler;
    }
    
    public void hidePopups(AjaxRequestTarget target)
    {
    	if (curPopupShown!=null)
    	{
    		curPopupShown.hide(target);
    		curPopupShown = null;
    	}
    }
        
    private class HideOnOutsideClickBehavior extends AbstractDefaultAjaxBehavior       
    {
        @Override
        protected void respond(AjaxRequestTarget target)
        {
            hidePopups(target);
        }
    }
    
    public static final class TCPopupPosition implements Serializable
    {
        public static enum PopupAlign {
            TopLeft("left top"),
            TopRight("right top"),
            BottomLeft("left bottom"),
            BottomRight("right bottom"),
            Center("center center");
            
            private String align;
            
            private PopupAlign(String align)
            {
                this.align = align;
            }
            
            public String getAlign()
            {
                return align;
            }
        }
        
        private String parentId;
        private String popupId;
        private PopupAlign parentAlign;
        private PopupAlign popupAlign;
        private Point offset;
        
        public TCPopupPosition(String parentId, String popupId, PopupAlign parentAlign, PopupAlign popupAlign, Point offset)
        {
            this.parentId = parentId;
            this.popupId = popupId;
            this.parentAlign = parentAlign;
            this.popupAlign = popupAlign;
            this.offset = offset;
        }
        
        public TCPopupPosition(String parentId, String popupId, PopupAlign parentAlign, PopupAlign popupAlign)
        {
            this(parentId, popupId, parentAlign, popupAlign, null);
        }
        
        public String getParentId()
        {
            return parentId;
        }
        
        public String getPopupId()
        {
            return popupId;
        }
        
        public PopupAlign getParentAlign()
        {
            return parentAlign;
        }
        
        public PopupAlign getPopupAlign()
        {
            return popupAlign;
        }
        
        public Point getOffset()
        {
            return offset;
        }
    }
    
    public static interface ITCPopupManagerProvider {
    	public TCPopupManager getPopupManager();
    }
    
    public static abstract class AbstractTCPopup extends WebMarkupContainer
    {
        TCPopupManager popupManager;
        boolean resizeable = false;
        boolean hideOnMouseOut = true;
        boolean hideOnOutsideClick = true;

        public AbstractTCPopup(final String id, boolean resizeable, boolean hideOnMouseOut, boolean hideOnOutsideClick)
        {
            this(id, null, resizeable, hideOnMouseOut, hideOnOutsideClick);
        }
        
        public AbstractTCPopup(final String id, TCPopupManager manager, boolean resizeable, boolean hideOnMouseOut, boolean hideOnOutsideClick)
        {
            super(id);
                        
            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);
            
            this.popupManager = manager;
            this.resizeable = resizeable;
            this.hideOnMouseOut = hideOnMouseOut;
            this.hideOnOutsideClick = hideOnOutsideClick;
            
            if (this.hideOnMouseOut)
            {
                add(new HideOnMouseOutBehavior());
            }
        }

        public final Component getPopupComponent()
        {
            return this;
        }

        public boolean isResizeable()
        {
            return resizeable;
        }

        public boolean isHideOnMouseOutEnabled()
        {
            return hideOnMouseOut;
        }

        public boolean isHideOnOutsideClickEnabled()
        {
            return hideOnOutsideClick;
        }

        public synchronized void show(AjaxRequestTarget target, TCPopupPosition position)
        {
        	TCPopupManager manager = getPopupManager();
            if (manager!=null)
            {            
            	if (manager.curPopupShown!=this)
            	{
            		manager.hidePopups(target);
            	}
            	
                beforeShowing(target);
                
                showPopup(position, target);

                afterShowing(target);
            }
        }

        public synchronized void hide(AjaxRequestTarget target)
        {       
            beforeHiding(target);
            
            hidePopup(target);
            
            afterHiding(target);
        }
        
        public void installPopupTrigger(Component trigger, TCPopupPosition position)
        {
            trigger.add(new PopupTriggerBehavior(position));
        }
        
        protected void beforeShowing(AjaxRequestTarget target)
        {
        	/* do nothing by default */
        }
                
        protected void afterShowing(AjaxRequestTarget target)
        {
            /* do nothing by default */
        }

        protected void beforeHiding(AjaxRequestTarget target)
        {
            /* do nothing by default */
        }
        
        protected void afterHiding(AjaxRequestTarget target)
        {
        	/* do nothing by default */
        }
        
        protected TCPopupManager getPopupManager()
        {
            if (popupManager==null)
            {
            	ITCPopupManagerProvider prov = findParent(ITCPopupManagerProvider.class);
            	if (prov!=null) {
            		popupManager = prov.getPopupManager();
            	}
            }
            return popupManager;
        }
        
        private void showPopup(TCPopupPosition position, AjaxRequestTarget target)
        {
            try
            {
                if (target==null)
                {
                    target = AjaxRequestTarget.get();
                }
                
                TCPopupManager popupManager = getPopupManager();
                if (popupManager!=null)
                {
	                popupManager.curPopupShown = this;
                }
                
                target.appendJavascript(getShowPopupJavascript(position));
            }
            catch (Exception e)
            {
                log.warn("Showing TC popup failed!", e);
            }
        }
        
        private void hidePopup(AjaxRequestTarget target)
        {
            try
            {
                if (target==null) {
                    target = AjaxRequestTarget.get();
                }

                TCPopupManager manager = getPopupManager();
                if (manager!=null) {
                	manager.curPopupShown = null;
                }
                
                target.appendJavascript(getHidePopupJavascript());
            }
            catch (Exception e)
            {
                log.error("Hiding TC popup failed!", e);
            }
        }

        protected String getShowPopupJavascript(TCPopupPosition position)
        {
            StringBuffer sbuf = new StringBuffer();
            
            //just show popup if popup isn't already shown yet
            sbuf.append("if (!isPopupShown(")
            .append("'").append(getPopupComponent().getMarkupId()).append("'")
            .append(")) {\n");
                        
            //set resizeable
            if (isResizeable())
            {
                sbuf.append("setPopupResizeable(")
                .append("'").append(getPopupComponent().getMarkupId()).append("'")
                .append(");\n");
            }
            
            //show popup
            sbuf
            .append("showPopup(")
            .append("'").append(getPopupComponent().getMarkupId()).append("'");
            
            if (isHideOnOutsideClickEnabled())
            {
            	TCPopupManager manager = getPopupManager();
            	if (manager!=null) {
	                CharSequence callbackUrl = manager
	                    .getGlobalHideOnOutsideClickHandler().getCallbackUrl();
	                sbuf.append(",")
	                .append("'").append(callbackUrl).append("'");
            	}
            }
            
            sbuf.append(");\n");
            
            //set popup position
            if (position!=null)
            {
                Point offset = position.getOffset();
                PopupAlign parentAlign = position.getParentAlign();
                PopupAlign popupAlign = position.getPopupAlign();
                
                if (offset==null) offset = new Point(0,0);
                if (parentAlign==null) parentAlign = PopupAlign.Center;
                if (popupAlign==null) popupAlign = PopupAlign.Center;
                
                sbuf
                .append("setPositionRelativeToParent(")
                .append("'").append(position.getParentId()).append("',")
                .append("'").append(position.getPopupId()).append("',")
                .append("'").append(parentAlign.getAlign()).append("',")
                .append("'").append(popupAlign.getAlign()).append("',")
                .append(offset.x).append(",")
                .append(offset.y).append(");\n");
            }
            
            sbuf.append("};");

            return sbuf.toString();
        }
            
        protected String getHidePopupJavascript()
        {
            StringBuffer sbuf = new StringBuffer();
            
            sbuf.append("hidePopup(")
            .append("'").append(getPopupComponent().getMarkupId()).append("'");

            if (isHideOnOutsideClickEnabled())
            {
            	TCPopupManager manager = getPopupManager();
            	if (manager!=null) {
	            	CharSequence callbackUrl = manager
	            			.getGlobalHideOnOutsideClickHandler().getCallbackUrl();
	            	sbuf.append(",")
	            	.append("'").append(callbackUrl).append("'");
            	}
            }

            sbuf.append(");");
            
            return sbuf.toString();
        }
        
        private class PopupTriggerBehavior extends AbstractDefaultAjaxBehavior
        {
            private TCPopupPosition position;
            private IAjaxCallDecorator callDecorator;

            public PopupTriggerBehavior(TCPopupPosition position)
            {
                this.position = position;
                this.callDecorator = new AjaxCallDecorator() {
                    @Override
                    public CharSequence decorateScript(CharSequence script)
                    {
                        StringBuffer sbuf = new StringBuffer(script);
                        
                        sbuf.insert(0, "event.stopPropagation();");
                        sbuf.append("return false;");
                        
                        return sbuf;
                    }
                };
            }
            
            @Override
            public void renderHead(IHeaderResponse head)
            {
                super.renderHead(head);
                
                StringBuffer sbuf = new StringBuffer();
                sbuf.append("$(document).on('click." + getComponent().getMarkupId(true)+"',");
                sbuf.append("'#").append(getComponent().getMarkupId(true)).append("',");
                sbuf.append("function(event) {");
                sbuf.append(getCallbackScript());
                sbuf.append("});");
                head.renderOnDomReadyJavascript(sbuf.toString());
            }
            
            @Override
            protected void respond(AjaxRequestTarget target)
            {
                show(target, position);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator()
            {
                return callDecorator;    
            }
        }
        
        private class HideOnMouseOutBehavior extends AbstractDefaultAjaxBehavior       
        {
            private IAjaxCallDecorator callDecorator;
            
            public HideOnMouseOutBehavior()
            {
                this.callDecorator = new AjaxCallDecorator() {
                    @Override
                    public CharSequence decorateScript(CharSequence script)
                    {
                        StringBuffer sbuf = new StringBuffer(script);
                        
                        sbuf.insert(0, "if (shouldHandlePopupMouseOut(event,'" + getPopupComponent().getMarkupId() + "')){");
                        sbuf.append("};");
                        
                        return sbuf;
                    }
                };
            }
            
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator()
            {
                return callDecorator;    
            }
            
            @Override
            public void renderHead(IHeaderResponse head)
            {
                super.renderHead(head);
                
                StringBuffer sbuf = new StringBuffer();
                sbuf.append("$(document).on('mouseout',");
                sbuf.append("'#").append(getComponent().getMarkupId(true)).append("',");
                sbuf.append("function(event) {");
                sbuf.append(getCallbackScript());
                sbuf.append("});");
                head.renderOnDomReadyJavascript(sbuf.toString());
            }
            
            @Override
            protected void respond(AjaxRequestTarget target)
            {
                hide(target);
            }
        }
    }
        
}
