package org.dcm4chee.web.war.tc.imageview;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.web.war.tc.TCUtilities;
import org.dcm4chee.web.war.tc.TCUtilities.TCPopupSettings;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Dec 02, 2011
 */
public class TCImagePage extends WebPage
{
    private static final int PREF_WINDOW_WIDTH = 1024;
    private static final int PREF_WINDOW_HEIGHT = 768;

    public TCImagePage(IModel<TCImageViewImage> model)
    {
        super(model==null ? (model=new Model<TCImageViewImage>()) : model);
        
        add(new TCWadoImage("tc-img", model));
    }
    
    public String getOpenInWindowJavascript(String title)
    {
        return getOpenInWindowJavascript(title, PREF_WINDOW_WIDTH, PREF_WINDOW_HEIGHT);
    }
    
    public String getOpenInWindowJavascript(String title, int width, int height)
    {
        TCPopupSettings settings = new TCPopupSettings(PopupSettings.SCROLLBARS);
        
        if (width>=0)
        {
            settings.setWidth(width);
        }
        if (height>=0)
        {
            settings.setHeight(height);
        }
        
        return TCUtilities.getOpenWindowJavascript(this, title, settings, false);
    }
}
