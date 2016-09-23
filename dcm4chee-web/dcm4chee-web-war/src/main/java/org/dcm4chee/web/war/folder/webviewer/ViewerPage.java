package org.dcm4chee.web.war.folder.webviewer;

import org.dcm4chee.web.common.secure.SecureSessionCheckPage;

public class ViewerPage extends SecureSessionCheckPage {
    public ViewerPage() {
        super();
        add(SecureSessionCheckPage.getBaseCSSHeaderContributor());
    }
}