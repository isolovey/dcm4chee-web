package org.dcm4chee.web.war.tc;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.security.WaspSession;
import org.apache.wicket.security.hive.authentication.Subject;
import org.apache.wicket.security.hive.authorization.Principal;
import org.apache.wicket.security.swarm.strategies.SwarmStrategy;
import org.dcm4chee.web.common.base.ExternalWebApp;
import org.dcm4chee.web.common.base.ExternalWebAppGroupPanel;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since July 31, 2013
 */
public class TCEnvironment {
	
	private static final Logger log = LoggerFactory.getLogger(TCEnvironment.class);

    private static final ResourceReference LAYOUT_CSS = new CompressedResourceReference(
            TCPanel.class, "css/tc-layout.css");
    
    private static final ResourceReference BASE_CSS = new CompressedResourceReference(
            TCPanel.class, "css/tc-style.css");
    
    private static final ResourceReference BASE_DARKROOM_CSS = new CompressedResourceReference(
            TCPanel.class, "css/tc-style-darkroom.css");
    
    private static final ResourceReference THEME_CSS = new CompressedResourceReference(
            TCPanel.class, "css/theme/theme.css");
    
    private static final ResourceReference THEME_DARKROOM_CSS = new CompressedResourceReference(
            TCPanel.class, "css/theme-darkroom/theme.css");
    
    private static final ResourceReference CASE_PAGE_CSS = new CompressedResourceReference(
            TCPanel.class, "css/tc-case-page.css");
    
	@SuppressWarnings("serial")
	public static void init(List<ExternalWebApp> list) {
		try {
			if (list!=null) {
				for (ExternalWebApp app : list) {
					Panel forumAdminPanel = findForumAdminPanel(app);
					if (forumAdminPanel!=null) {
							WebCfgDelegate cfg = WebCfgDelegate.getInstance();
							final TCForumIntegration forum = TCForumIntegration.get(
									cfg.getTCForumIntegrationType());
							
							if (forum!=null) {
								app.getPanel().add(new AbstractBehavior() {
									@Override
									public void beforeRender(Component c) {
										super.beforeRender(c);
										try {
											forum.setAdminUserCookie();
										}
										catch (Exception e) {
											log.error(null, e);
										}
									}
								});
							}
							
							break;
					}
				}
			}
		}
		catch (Exception e) {
			log.error(null, e);
		}
	}
	
	@SuppressWarnings("serial")
	public static HeaderContributor getCSSHeaderContributor() {
		return new HeaderContributor(new IHeaderContributor() {
			public void renderHead(IHeaderResponse response)
			{
				IModel<ResourceReference> cssModel = Session.get().getMetaData(SecureSessionCheckPage.BASE_CSS_MODEL_MKEY);
				boolean darkroom = cssModel!=null &&
						SecureSessionCheckPage.BASE_CSS_R==cssModel.getObject();
				
				response.renderCSSReference(CASE_PAGE_CSS);
				
				if (darkroom)
				{
					response.renderCSSReference(THEME_DARKROOM_CSS);
					response.renderCSSReference(LAYOUT_CSS);
					response.renderCSSReference(BASE_DARKROOM_CSS);
				}
				else
				{
					response.renderCSSReference(THEME_CSS);
					response.renderCSSReference(LAYOUT_CSS);
					response.renderCSSReference(BASE_CSS);
				}
			}
		});
	}
	
	public static boolean isPrincipalAuthorized(String principalName) {
		Session session = Session.get();
		if (session instanceof WaspSession) {
			IAuthorizationStrategy authStrategy = ((WaspSession)session).getAuthorizationStrategy();
			if (authStrategy instanceof SwarmStrategy) {
				Subject subject = ((SwarmStrategy)authStrategy).getSubject();
				if (subject!=null) {
					for (Principal principal : subject.getPrincipals()) {
						if (principalName.equals(principal.getName())) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}
	
	private static Panel findForumAdminPanel(ExternalWebApp app) {
		Panel appPanel = app.getPanel();
		List<Panel> panels = new ArrayList<Panel>(3);
		if (appPanel instanceof ExternalWebAppGroupPanel) {
			List<ITab> tabs = ((ExternalWebAppGroupPanel)appPanel).getTabs();
			if (tabs!=null) {
				for (ITab tab : tabs) {
					Panel tabPanel = tab.getPanel(null);
					if (tabPanel!=null) {
						panels.add(tabPanel);
					}
				}
			}
		}
		else {
			panels.add(appPanel);
		}
		
		for (Panel panel : panels) {
			String url = panel.getDefaultModelObjectAsString();
			if (url.endsWith("jforum/forums/list.page")) {
				return panel;
			}
		}
		
		return null;
	}
}
