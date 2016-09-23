package org.dcm4chee.web.war.common;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @since Apr 04, 2012
 */
public class AutoSelectInputTextBehaviour extends AbstractBehavior 
{
    @Override
    public void onComponentTag(final Component component, final ComponentTag tag)
    {
        tag.put("onfocus","this.select()");
    }
}
