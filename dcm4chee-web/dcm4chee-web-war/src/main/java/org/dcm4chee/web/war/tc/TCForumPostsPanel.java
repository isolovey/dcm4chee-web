package org.dcm4chee.web.war.tc;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

@SuppressWarnings("serial")
public class TCForumPostsPanel extends Panel {

	public TCForumPostsPanel(final String id, final IModel<String> urlModel) {
		super(id, urlModel);
		add(new TCUtilities.TCStyleAppender("height:100%"));
		add(new WebMarkupContainer("frame") {
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				String url = urlModel!=null ? urlModel.getObject() : null;
				if (url!=null) {
					tag.put("src", url);
				}
			}
		});
	}

}
