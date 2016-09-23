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

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.web.common.ajax.MaskingAjaxCallBehavior;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.behaviours.FocusOnLoadBehaviour;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Dec 11, 2009
 */
public abstract class ConfirmationWindow<T> extends ModalWindow {
    
    private static final long serialVersionUID = 1L;

    public static final String FOCUS_ON_CONFIRM = "confirm";
    public static final String FOCUS_ON_DECLINE = "decline";
    public static final String FOCUS_ON_CANCEL = "cancel";

    public static final int UNCONFIRMED = 0;
    public static final int CONFIRMED = 1;
    public static final int DECLINED = 2;
    public static final int CANCELED = 3;

    private T userObject;

    private IModel<?> remark, confirm, decline, cancel;
    
    protected boolean hasStatus;
    private boolean showCancel = false;
    private int state = UNCONFIRMED;
    
    public MessageWindowPanel messageWindowPanel;

    public Model<Component> focusComponentModel = new Model<Component>(null);
    
    public ConfirmationWindow(String id, String titleResource) {
        this(id);
        setTitle(new ResourceModel(titleResource));
    }
    
    public ConfirmationWindow(String id) {
        
        this(id, new ResourceModel("yesBtn"), new ResourceModel("noBtn"), new ResourceModel("cancelBtn"));

        setCloseButtonCallback(new CloseButtonCallback() {

            private static final long serialVersionUID = 1L;

            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                messageWindowPanel.msg = null;
                close(target);
                return true;
            }
        });
        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {  
            
            private static final long serialVersionUID = 1L;

            @Override
            public void onClose(AjaxRequestTarget target) {
                getPage().setOutputMarkupId(true);
                target.addComponent(getPage());
            }
        });
    }

    public ConfirmationWindow(String id, IModel<?> confirm, IModel<?> decline, IModel<?> cancel) {
        super(id);
        this.confirm = confirm;
        this.decline = decline;
        this.cancel = cancel;
        initContent(400, 300);
    }

    public void initContent(int width, int height) {
    	initSize(width, height);

        messageWindowPanel = new MessageWindowPanel("panel");
        
        setPageCreator(new ModalWindow.PageCreator() {
            
            private static final long serialVersionUID = 1L;
              
            public Page createPage() {
                return new ConfirmPage();
            }
        });
        add(new DisableDefaultConfirmBehavior());
    }

    private void initSize(int width, int height) {
        setInitialWidth(width);
        setInitialHeight(height);
    }

    public abstract void onConfirmation(AjaxRequestTarget target, T userObject);
    public void onDecline(AjaxRequestTarget target, T userObject) {}
    public void onCancel(AjaxRequestTarget target, T userObject) {}
    public void onOk(AjaxRequestTarget target) {}
    
    @Override
    public void show(final AjaxRequestTarget target) {
        hasStatus = false;
        super.show(target);
    }
    
    public void confirm(AjaxRequestTarget target, IModel<?> msg, T userObject) {
        confirm(target, msg, userObject, FOCUS_ON_DECLINE);
    }
    public void confirm(AjaxRequestTarget target, IModel<?> msg, T userObject, String focusElementId) {
        confirm(target, msg, userObject, focusElementId, false);
    }
    public void confirm(AjaxRequestTarget target, IModel<?> msg, T userObject, String focusElementId, boolean showCancel) {
        this.messageWindowPanel.msg = msg;
        this.userObject = userObject;
        this.focusComponentModel.setObject(getFocusComponent(focusElementId));
        this.showCancel = showCancel;
        show(target);
    }

    private Component getFocusComponent(String focusElementId) {
        if (focusElementId == null) {
            return null;
        } else if (FOCUS_ON_DECLINE.equals(focusElementId)) {
            return this.messageWindowPanel.declineBtn;
        } else if (FOCUS_ON_CANCEL.equals(focusElementId)) {
            return this.messageWindowPanel.cancelBtn;
        } else if (FOCUS_ON_CONFIRM.equals(focusElementId)) {
            return this.messageWindowPanel.confirmBtn;
        }
        return null;
    }

    public void confirmWithCancel(AjaxRequestTarget target, IModel<?> msg, T userObject) {
        confirm(target, msg, userObject, FOCUS_ON_CANCEL, true);
    }
    
    public void setStatus(IModel<?> statusMsg) {
        messageWindowPanel.msg = statusMsg;
        hasStatus = true;
    }

    public void setRemark(IModel<?> remark) {
        this.remark = remark;
    }
    
    public T getUserObject() {
        return userObject;
    }    
    
    public int getState() {
        return state;
    }
    
    public void setImage(ResourceReference image) {
    	messageWindowPanel.replace(new Image("warnImg", image));
    }

    public class ConfirmPage extends WebPage {
        public ConfirmPage() {
            add(SecureSessionCheckPage.getBaseCSSHeaderContributor());
            add(messageWindowPanel);
        }
    }
    @SuppressWarnings("hiding")
    public class MessageWindowPanel extends Panel {
        
        private static final long serialVersionUID = 1L;
        
        private final Logger log = LoggerFactory.getLogger(MessageWindowPanel.class);
        
        private IndicatingAjaxFallbackLink<Object> confirmBtn;
        private AjaxFallbackLink<Object> declineBtn;
        private AjaxFallbackLink<Object> cancelBtn;
        private AjaxFallbackLink<Object> okBtn;
        
        private IModel<?> msg;
        private Label msgLabel;
        private Label remarkLabel;
        
        private boolean logout = false;
        
        public MessageWindowPanel(String id) {
            super(id);
            
            final MaskingAjaxCallBehavior macb = new MaskingAjaxCallBehavior();
            add(macb);
            
            add(new Label("warnImg").setVisible(false));
            
            add((msgLabel = new Label("msg", new AbstractReadOnlyModel<Object>() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return msg == null ? null : msg.getObject();
                }
            })).setOutputMarkupId(true)
            .setEscapeModelStrings(false));
            
            add((remarkLabel = new Label("remark", new AbstractReadOnlyModel<Object>() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return remark == null ? null : remark.getObject();
                }
            }){
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return !hasStatus;
                }

            }).setOutputMarkupId(true)
            .setEscapeModelStrings(false));

            confirmBtn = new IndicatingAjaxFallbackLink<Object>("confirm") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        onConfirmation(target, userObject);
                        state = CONFIRMED;
                        if (hasStatus) {
                            target.addComponent(MessageWindowPanel.this);
                        } else {
                            msg = null;
                            close(target);
                        }
                    } catch (Exception x) {
                        logout = true;
                        setStatus(new Model<String>(x.getMessage()));
                        target.addComponent(MessageWindowPanel.this);
                    }
                }
                
                @Override
                public boolean isVisible() {
                    return !hasStatus;
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    try {
                        return macb.getAjaxCallDecorator();
                    } catch (Exception e) {
                        log.error("Failed to get IAjaxCallDecorator: ", e);
                    }
                    return null;
                }
            };
            confirmBtn.add(new Label("confirmLabel", confirm));
            confirmBtn.setOutputMarkupId(true);
            add(confirmBtn);
            
            declineBtn = new AjaxFallbackLink<Object>("decline"){

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    onDecline(target, userObject);
                    state = DECLINED;
                    if (hasStatus) {
                        target.addComponent(MessageWindowPanel.this);
                    } else {
                        msg = null;
                        close(target);
                    }
                }
                @Override
                public boolean isVisible() {
                    return !hasStatus;
                }
            };
            declineBtn.add(new Label("declineLabel", decline)).setOutputMarkupId(true);
            add(declineBtn);
            
            cancelBtn = new AjaxFallbackLink<Object>("cancel"){
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    onCancel(target, userObject);
                    state = CANCELED;
                    msg = null;
                    close(target);
                }
                @Override
                public boolean isVisible() {
                    return !hasStatus && showCancel;
                }
            };
            add(cancelBtn.add(new Label("cancelLabel", cancel)).setOutputMarkupId(true));
            
            add(okBtn = new IndicatingAjaxFallbackLink<Object>("ok") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (logout)   
                        onConfirmation(target, userObject);
                    else 
                        onOk(target);
                    msg = null;
                    close(target);
                }
                
                @Override
                public boolean isVisible() {
                    return hasStatus;
                }
                
                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    try {
                        return macb.getAjaxCallDecorator();
                    } catch (Exception e) {
                        log.error("Failed to get IAjaxCallDecorator: ", e);
                    }
                    return null;
                }
            });
            getOkBtn().add(new Label("okLabel", new ResourceModel("okBtn")));
            getOkBtn()
            .setOutputMarkupId(true)
            .setOutputMarkupPlaceholderTag(true);
            this.setOutputMarkupId(true);
            add(FocusOnLoadBehaviour.newComponentModelFocusBehaviour(focusComponentModel));
        }
        
        /**
         * Return always true because ModalWindow.beforeRender set visibility of content to false!
         */
        @Override
        public boolean isVisible() {
            return true;
        }

        public AjaxFallbackLink<Object> getOkBtn() {
            return okBtn;
        }

        public Label getMsgLabel() {
            return msgLabel;
        }

        public Label getRemarkLabel() {
            return remarkLabel;
        }        
    }
    
    public MessageWindowPanel getMessageWindowPanel() {
        return messageWindowPanel;
    }    

    public class DisableDefaultConfirmBehavior extends AbstractBehavior implements IHeaderContributor {

        private static final long serialVersionUID = 1L;

        @Override
        public void renderHead(IHeaderResponse response) {
            response.renderOnDomReadyJavascript ("Wicket.Window.unloadConfirmation = false");
        }
    }
}
