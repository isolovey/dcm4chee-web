package org.dcm4chee.web.common.markup;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.CloseButtonCallback;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;

public class ModalWindowLink extends AjaxFallbackLink<Object> {

    private static final long serialVersionUID = 1L;
    
    private ModalWindow modalWindow;
    
    public ModalWindowLink(String id, ModalWindow modalWindow) {
        super(id);
        this.modalWindow = modalWindow;
    }
    
    public ModalWindowLink(String id, ModalWindow modalWindow, int width, int height) {
        this(id, modalWindow);
        modalWindow.setInitialWidth(width);
        modalWindow.setInitialHeight(height);
    }
    
    @Override
    public void onClick(AjaxRequestTarget target) {
        modalWindow.setCloseButtonCallback(new CloseButtonCallback() {

            private static final long serialVersionUID = 1L;

            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });
        modalWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {              
            
            private static final long serialVersionUID = 1L;

            @Override
            public void onClose(AjaxRequestTarget target) {
                modalWindow.getPage().setOutputMarkupId(true);
                target.addComponent(modalWindow.getPage());
            }
        });
        modalWindow.add(new DisableDefaultConfirmBehavior());
        modalWindow.show(target);
    }
    
    public class DisableDefaultConfirmBehavior extends AbstractBehavior implements IHeaderContributor {

        private static final long serialVersionUID = 1L;

        @Override
        public void renderHead(IHeaderResponse response) {
            response.renderOnDomReadyJavascript ("Wicket.Window.unloadConfirmation = false");
        }
    }
}