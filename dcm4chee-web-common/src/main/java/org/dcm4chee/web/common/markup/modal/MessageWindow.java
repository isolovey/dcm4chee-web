/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.web.common.markup.modal;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.interpolator.PropertyVariableInterpolator;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.exceptions.WicketExceptionWithMsgKey;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2009
 */
public class MessageWindow extends AutoOpenModalWindow {

    private static final long serialVersionUID = 1L;
    
    protected static final String TITLE_DEFAULT = "MessageWindow";
    public static final String TITLE_INFO="msgwindow.title.info";
    public static final String TITLE_WARNING="msgwindow.title.warn";
    public static final String TITLE_ERROR="msgwindow.title.error";
    
    private IModel<String> msgModel;
    private IModel<String> colorModel = new Model<String>("");

    public MessageWindow(String id) {
        super(id);
        initContent();
    }

    public MessageWindow(String id, String titleResource) {
        this(id);
        setTitle(new ResourceModel(titleResource));
    }

    protected void initContent() {
        setInitialWidth(300);
        setInitialHeight(200);
        setTitle(new ResourceModel(TITLE_INFO,TITLE_DEFAULT));
        setPageCreator(new ModalWindow.PageCreator() {
            
            private static final long serialVersionUID = 1L;
              
            @Override
            public Page createPage() {
                return new MessagePage();
            }
        });
    }
    
    public void setMessage(String msg) {
        this.msgModel = new Model<String>(msg);
    }
    public void setInfoMessage(String msg) {
        this.msgModel = new Model<String>(msg);
        setTitle(new ResourceModel(TITLE_INFO,TITLE_DEFAULT));
    }
    public void setWarningMessage(String msg) {
        this.msgModel = new Model<String>(msg);
        setTitle(new ResourceModel(TITLE_WARNING,TITLE_DEFAULT));
    }
    public void setErrorMessage(String msg) {
        this.msgModel = new Model<String>(msg);
        setTitle(new ResourceModel(TITLE_ERROR,TITLE_DEFAULT));
    }
    
    /**
     * Get Message from Exception
     * If Exception is of type WicketExceptionWithMsgKey: ResourceKey with the msgKey
     * For any other Exception: the first getLocalizedMessage()!=null of the Exception hierachie or toString of root cause
     * 
     * @param x
     * @param useCauseAsDetail
     */
    public void setErrorMessage(Exception x, boolean useCauseAsDetail) {
        IModel<String> detail = null;
        if (useCauseAsDetail && x.getCause() != null) {
            Throwable cause = x.getCause();
            if (cause instanceof WicketExceptionWithMsgKey) {
                detail = new ResourceModel(((WicketExceptionWithMsgKey) cause).getMsgKey()).wrapOnAssignment(this);
            } else {
                detail = new Model<String>(getExceptionMessage(cause));
            }
        }
        if (x instanceof WicketExceptionWithMsgKey) {
            WicketExceptionWithMsgKey we = (WicketExceptionWithMsgKey) x;
            msgModel = new StringResourceModel(we.getMsgKey(), this, detail, we.getMsgParams());
        } else {
            String msg = detail == null ? getExceptionMessage(x) : 
                PropertyVariableInterpolator.interpolate(getExceptionMessage(x), detail.getObject());
            msgModel = new Model<String>(msg);
        }
        setTitle(new ResourceModel(TITLE_ERROR,TITLE_DEFAULT));
    }
    
    private String getExceptionMessage(Throwable x) {
        if (x.getLocalizedMessage() != null) {
            return x.getLocalizedMessage();
        } else if (x.getCause() == null) {
            return x.toString();
        } else {
            return getExceptionMessage(x.getCause());
        }
    }
    
    /**
     * Called by onBeforeRender to check if window should be opened without AJAX for this request. 
     *
     * @return true when window should be opened. 
     */
    protected boolean needAutoOpen() {
        return msgModel != null;
    }
    
    @Override
    public void show(final AjaxRequestTarget target) {
        if (target != null) {
            super.show(target);
            target.focusComponent(this.get("content:close"));
        }
    }

    public void show(AjaxRequestTarget target, String msg) {
        this.msgModel = new Model<String>(msg);
        show(target);
    }

    public void setColor(String color) {
        this.colorModel.setObject("color: " + color);
    }
    
    public void show(AjaxRequestTarget target, IModel<String> msg) {
        this.msgModel = msg;
        show(target);
    }
    public void show(AjaxRequestTarget target, Exception x, boolean useCauseAsDetail) {
        this.setErrorMessage(x, useCauseAsDetail);
        show(target);
    }

    public class MessagePage extends WebPage {
        private static final long serialVersionUID = 0L;

        public MessagePage() {
            add(SecureSessionCheckPage.getBaseCSSHeaderContributor());
            add(new Label("msg", new AbstractReadOnlyModel<String>(){

                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    return msgModel == null ? null : msgModel.getObject();
                }
            }).setEscapeModelStrings(false)
            .add(new AttributeModifier("style", true, colorModel)));

            add(new AjaxFallbackLink<Object>("close"){

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    close(target);
                }
            }.add(new Label("closeLabel", new ResourceModel("closeBtn"))) );
        }

        /**
         * Return always true because ModalWindow.beforeRender set visibility of content to false!
         */
        @Override
        public boolean isVisible() {
            return true;
        }
        @Override
        protected void onAfterRender() {
            msgModel = null;
            super.onAfterRender();
        }
    }
}
