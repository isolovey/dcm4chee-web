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

package org.dcm4chee.web.common.base;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;

import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.PackageResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.security.swarm.SwarmWebApplication;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.markup.modal.ConfirmationWindow;
import org.dcm4chee.web.common.model.ProgressProvider;
import org.dcm4chee.web.common.secure.SecureAjaxTabbedPanel;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.util.CloseRequestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since July 12, 2009
 */

public class ModuleSelectorPanel extends SecureAjaxTabbedPanel {

    private static final long serialVersionUID = 1L;
    private static List<String> languages;
    private static String defaultLanguage;
    private IModel<ResourceReference> baseCssModel;
    
    public boolean showLogout = true;
    
    private static Logger log = LoggerFactory.getLogger(ModuleSelectorPanel.class);
    
    private static final long LAST_REFRESHED_TIMEOUT = 5000l;

    private boolean popupsClosed = false;
    
    ConfirmationWindow<List<ProgressProvider>> confirmLogout = new ConfirmationWindow<List<ProgressProvider>>("confirmLogout") {

        private static final long serialVersionUID = 1L;

        @Override
        public void onConfirmation(AjaxRequestTarget target, List<ProgressProvider> providers) {

            if (popupsClosed) {
                getSession().invalidate();
                return;
            }
            
            if (closePopups(providers)) {
                popupsClosed = true;
                throw new IllegalStateException(ModuleSelectorPanel.this.getString("logout.logout"));
            }
            else if (isPopupOpen(providers)) {
                throw new IllegalStateException(ModuleSelectorPanel.this.getString("logout.waiting"));
            }
        }
    };

    final ModalWindow aboutWindow = new ModalWindow("aboutWindow");

    AjaxLink<Object> aboutLink = new AjaxLink<Object>("aboutLink") {
        private static final long serialVersionUID = 1L;

        @Override
        public void onClick(AjaxRequestTarget target) {
            aboutWindow.setTitle("").show(target);
        }
    };

    public ModuleSelectorPanel(String id) {
        this(id, null);
    }
    public ModuleSelectorPanel(String id, IModel<ResourceReference> cssModel) {
        super(id);
        this.baseCssModel = cssModel;
        boolean found = false, cssCookie = false;
        Cookie[] cs = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
        if (cs != null)
            for (Cookie c : cs) {
                if (c.getName().equals("WEB3LOCALE")) {
                    getSession().setLocale(parseLocale(c.getValue()));
                    found = true;
                    if (cssCookie)
                        break;
                } else if (c.getName().equals("WEB3_CSS")) {
                	if (baseCssModel != null) { 
	                    for (ResourceReference rsrc : getBaseCssResources()) {
	                        if (rsrc.getName().equals(c.getValue()))
	                            baseCssModel.setObject(rsrc);
	                    }
                	}
                    cssCookie = true;
                    if (found)
                        break;
                }
            }

        if (!found) {
            Cookie c = new Cookie("WEB3LOCALE", trimTogetLanguageIfNotInSelection(getSession().getLocale()));
            c.setMaxAge(Integer.MAX_VALUE);
            ((WebResponse) RequestCycle.get().getResponse()).addCookie(c);
        }
        
        add(confirmLogout);

        try {
            InputStream is = ((SwarmWebApplication) getApplication()).getServletContext().getResourceAsStream("/WEB-INF/web.xml");
            XMLReader parser = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
            
            DefaultHandler dh = new DefaultHandler() {
                
                private StringBuffer current;
    
                @Override
                public void characters (char ch[], int start, int length) throws SAXException {
                    current = new StringBuffer().append(ch, start, length);
                }
    
                @Override
                public void endElement (String uri, String localName, String qName) throws SAXException {
                    if(qName.equals("auth-method"))
                        if (current.toString().equals("BASIC")) 
                            showLogout = false;
                }
            };
            parser.setContentHandler(dh);
            parser.parse(new InputSource(is));
        } catch (Exception ignore) {
        }
        
        add(new AjaxFallbackLink<Object>("logout") {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void onClick(final AjaxRequestTarget target) {
                List<ProgressProvider> providers = null;
                Session s = getSession();
                if (s instanceof SecureSession) {
                    providers = ((SecureSession) s).getProgressProviders();
                    if (providers.size() > 0) {
                        confirmLogout.confirm(target, 
                                new ResourceModel("logout.confirmPendingTasks").wrapOnAssignment(this), providers);
                        return;
                    }
                }
                getSession().invalidate();
                setResponsePage(getApplication().getHomePage());
            }

            @Override
            public boolean isVisible() {
                return showLogout;
            }
        }.add(new Label("logoutLabel", 
            new StringResourceModel("logout", ModuleSelectorPanel.this, null, 
                    new Object[] { 
                        ((SecureSession) RequestCycle.get().getSession()).getUsername()
                    })
        )));

        final DropDownChoice<String> languageSelector = 
            new DropDownChoice<String>("language", new Model<String>(), getLanguages(), new ChoiceRenderer<String>() {

            private static final long serialVersionUID = 1L;
            
            @Override
            public String getDisplayValue(String object) {
            	Locale l = parseLocale(object);
                return l.getDisplayName(l);
            }
        }) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectionChanged(String newSelection) {
                Cookie c = new Cookie("WEB3LOCALE", newSelection);
                c.setMaxAge(Integer.MAX_VALUE);
                ((WebResponse) RequestCycle.get().getResponse()).addCookie(c);
                getSession().setLocale(parseLocale(newSelection));
            }
        };
        String def = System.getProperty("pacs.web.default.locale", defaultLanguage);
        if (def != null && def.length() > 1) {
            getSession().setLocale(parseLocale(def));
        }
        languageSelector.setDefaultModelObject(trimTogetLanguageIfNotInSelection(getSession().getLocale()));
        languageSelector.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            protected void onUpdate(AjaxRequestTarget target) {
                languageSelector.onSelectionChanged();
                target.addComponent(getPage().setOutputMarkupId(true));
            }
        });
        add(languageSelector);
        
        final DropDownChoice<ResourceReference> cssSelector = 
            new DropDownChoice<ResourceReference>("cssSelect", baseCssModel, 
                    getBaseCssResources(), new ChoiceRenderer<ResourceReference>() {

            private static final long serialVersionUID = 1L;
            
            @Override
            public String getDisplayValue(ResourceReference object) {
                String n = object.getName();
                if (n.endsWith(".css"))
                    n = n.substring(0, n.length()-4);
                return ModuleSelectorPanel.this.getString("style.name."+n, null, n);
            }
        }) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectionChanged(ResourceReference newSelection) {
                log.info("set Base CSS resource:"+newSelection);
                Cookie c = new Cookie("WEB3_CSS", newSelection.getName());
                c.setMaxAge(Integer.MAX_VALUE);
                ((WebResponse) RequestCycle.get().getResponse()).addCookie(c);
            }
        };
        cssSelector.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            protected void onUpdate(AjaxRequestTarget target) {
                cssSelector.onSelectionChanged();
                target.addComponent(getPage());
            }
        }).add(new TooltipBehaviour("application.", "styleselect"));
        add(cssSelector);
        
        add(aboutWindow.setInitialWidth(600).setInitialHeight(400));

        add(aboutLink
                .add(new Image("img_logo", new PackageResourceReference(ModuleSelectorPanel.class, "images/logo.gif")))
                .add(new TooltipBehaviour("dicom.")).setEnabled(false));
    }

    protected List<? extends ResourceReference> getBaseCssResources() {
        return Arrays.asList(SecureSessionCheckPage.BASE_CSS, SecureSessionCheckPage.BASE_CSS_R);
    }

    private List<String> getLanguages() {
        if (languages == null) {
            String s = ((WebApplication)getApplication()).getInitParameter("Languages");
            if (s == null || s.equals("ANY")) {
                languages = Arrays.asList("en", "en_GB", "de", "ja");
            } else {
                languages = new ArrayList<String>();
                String l;
                for ( StringTokenizer st = new StringTokenizer(s, ",") ; st.hasMoreElements() ; ) {
                    l = st.nextToken().trim();
                    if (l.charAt(0) == '*') {
                        l = l.substring(1);
                        defaultLanguage = l;
                    }
                    languages.add(l);
                }
            }
        }
        return languages;
    }
    
    private String trimTogetLanguageIfNotInSelection(Locale locale) {
    	String language = locale.toString();
    	return getLanguages().contains(language) ? language : locale.getLanguage();
    }

    public void addModule(final Class<? extends Panel> clazz) {
        super.addModule(clazz, null);
    }

    public void addInstance(Panel instance) {
        addInstance(instance, null);
    }

    public void addInstance(Panel instance, IModel<String> titleModel) {
        super.addModule(instance.getClass(), titleModel);
    }

    public ModuleSelectorPanel setShowLogoutLink(boolean show) {
        showLogout = show;
        return this;
    }
    
    public ModalWindow getAboutWindow() {
        aboutLink.setEnabled(true);
        return aboutWindow;
    }

    private boolean closePopups(List<ProgressProvider> providers) {
        boolean b = false;
        if (providers != null) {
            synchronized (providers) {
                Integer pageID;
                for (int i = 0, len = providers.size() ; i < len ; i++) {
                    pageID = providers.get(i).getPopupPageId();
                    log.info("Provider has status: " + providers.get(i).getStatus());
                    if (pageID != null) {
                        Page p = ModuleSelectorPanel.this.getSession().getPage(pageID, 0);
                        log.info("Found open popup page:"+p);
                        if (p != null && (p instanceof CloseRequestSupport)) {
                            log.debug("Set close request for popup page:"+p);
                            if (!((CloseRequestSupport) p).isCloseRequested()) {
                                ((CloseRequestSupport) p).setCloseRequest();
                                b = true;
                            }
                        }
                    }
                }
            }
        }
        return b;
    }
    
    private boolean isPopupOpen(List<ProgressProvider> providers) {
        if (providers != null) {
            synchronized (providers) {
                Integer pageID;
                for (int i = 0, len = providers.size() ; i < len ; i++) {
                    pageID = providers.get(i).getPopupPageId();
                    if (pageID != null) {
                        Page p = ModuleSelectorPanel.this.getSession().getPage(pageID, 0);
                        if (p != null && (p instanceof CloseRequestSupport) && !((CloseRequestSupport)p).isClosed()) {
                            //check refresh timeout in case popup is closed without removing page in pagemap.(e.g. window close button)
                            return (System.currentTimeMillis() - providers.get(i).getLastRefreshedTimeInMillis() < LAST_REFRESHED_TIMEOUT);
                                
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private static Locale parseLocale(String s)
    {
    	try
    	{
        	String[] parts = s.split("_"); //$NON-NLS-1$
        	String lang = parts.length>0 ? parts[0].trim() : "en"; //$NON-NLS-1$
        	String country = parts.length>1 ? parts[1].trim() : ""; //$NON-NLS-1$
        	String variant = parts.length>2 ? parts[2].trim() : ""; //$NON-NLS-1$
        	return new Locale(lang,country,variant);
    	}
    	catch ( Exception e )
    	{
    		log.warn("Parsing locale failed! " + e.getMessage()); //$NON-NLS-1$
    		return new Locale(s);
    	}
    }
}
