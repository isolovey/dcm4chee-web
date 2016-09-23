package org.dcm4chee.web.war.tc;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Panel;

@SuppressWarnings("serial")
public abstract class TCModalDialogPanel extends Panel {

	public TCModalDialogPanel(final String id, String msg, final String ackText, final String cancelText) {
		super(id);
		setOutputMarkupId(true);
		add(new MultiLineLabel("dlg-msg", msg!=null?msg:""));
		add(new AjaxFallbackLink<Void>("dlg-ok-button") {
				@Override
				public void onClick(AjaxRequestTarget target)
				{
					onAcknowledge(target);
				}
				@Override
				public boolean isVisible() {
					return ackText!=null;
				}
			}
			.add(new Label("dlg-ok-button-text", ackText!=null?ackText:""))
		);
		add(new AjaxFallbackLink<Void>("dlg-cancel-button") {
				@Override
				public void onClick(AjaxRequestTarget target)
				{
					onCancel(target);
				}
				@Override
				public boolean isVisible() {
					return cancelText!=null;
				}
			}
			.add(new Label("dlg-cancel-button-text", cancelText!=null?cancelText:""))
		);
	}
	
	protected abstract void onAcknowledge(AjaxRequestTarget target);
	protected abstract void onCancel(AjaxRequestTarget target);
		
	public static class TCModalDialog extends ModalWindow {
		private IModalDialogCallback callback;
		public TCModalDialog(final String id, String msg, String ackText, String cancelText, IModalDialogCallback callback) {
			super(id);
			this.callback = callback;
			setContent(new TCModalDialogPanel(getContentId(), msg, ackText, cancelText) {
				protected void onAcknowledge(AjaxRequestTarget target) {
					close(target);
					if (TCModalDialog.this.callback!=null) {
						TCModalDialog.this.callback.dialogAcknowledged(target);
					}
				}
				protected void onCancel(AjaxRequestTarget target) {
					close(target);
					if (TCModalDialog.this.callback!=null) {
						TCModalDialog.this.callback.dialogCanceled(target);
					}
				}
			});
		}
		
		public static TCModalDialog getOk(final String id, String msg) {
			return new TCModalDialog(id, msg, 
					TCUtilities.getLocalizedString("tc.dialog.ok.text"),
					null, null);
		}
		
		public static TCModalDialog getOk(final String id, String msg, IModalDialogCallback callback) {
			return new TCModalDialog(id, msg, 
					TCUtilities.getLocalizedString("tc.dialog.ok.text"),
					null, callback);
		}

		public static TCModalDialog getOkCancel(final String id, String msg, IModalDialogCallback callback) {
			return new TCModalDialog(id, msg, 
					TCUtilities.getLocalizedString("tc.dialog.ok.text"),
					TCUtilities.getLocalizedString("tc.dialog.cancel.text"),
					callback);
		}
		
		public static TCModalDialog getYes(final String id, String msg) {
			return new TCModalDialog(id, msg, 
					TCUtilities.getLocalizedString("tc.dialog.yes.text"),
					null, null);
		}
		
		public static TCModalDialog getYes(final String id, String msg, IModalDialogCallback callback) {
			return new TCModalDialog(id, msg, 
					TCUtilities.getLocalizedString("tc.dialog.yes.text"),
					null, callback);
		}
		
		public static TCModalDialog getYesNo(final String id, String msg, IModalDialogCallback callback) {
			return new TCModalDialog(id, msg, 
					TCUtilities.getLocalizedString("tc.dialog.yes.text"),
					TCUtilities.getLocalizedString("tc.dialog.no.text"),
					callback);
		}
		
		public IModalDialogCallback getCallback() {
			return callback;
		}
		
		public void setCallback(IModalDialogCallback callback) {
			this.callback = callback;
		}
	}
	
	public static interface IModalDialogCallback extends Serializable {
		void dialogCanceled(AjaxRequestTarget target);
		void dialogAcknowledged(AjaxRequestTarget target);
	}
	
	public static abstract class ModalDialogCallbackAdapter implements IModalDialogCallback {
		public void dialogCanceled(AjaxRequestTarget target) {
			/* empty adapter implementation */
		}
		public void dialogAcknowledged(AjaxRequestTarget target) {
			/* empty adapter implementation */
		}
	}
}
