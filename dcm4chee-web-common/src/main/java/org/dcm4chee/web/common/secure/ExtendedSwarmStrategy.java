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
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
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

package org.dcm4chee.web.common.secure;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.security.actions.WaspAction;
import org.apache.wicket.security.components.ISecureComponent;
import org.apache.wicket.security.components.SecureComponentHelper;
import org.apache.wicket.security.hive.authorization.permissions.ComponentPermission;
import org.apache.wicket.security.swarm.actions.SwarmAction;
import org.apache.wicket.security.swarm.strategies.SwarmStrategy;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 23.09.2010
 */
public class ExtendedSwarmStrategy extends SwarmStrategy {

    private static final long serialVersionUID = 1L;

    public ExtendedSwarmStrategy(Object hiveQueen) {
        super(hiveQueen);
    }

    public ExtendedSwarmStrategy(Class<? extends ISecureComponent> secureClass, Object hiveQueen) {
        super(secureClass, hiveQueen);
    }

    @Override
    public boolean isComponentAuthorized(Component component, WaspAction action) {
      if(containsBehavior(component, SecurityBehavior.class))
            return hasPermission(new ComponentPermission(buildHiveKey(component), (SwarmAction) action));
       else
            return hasPermission(new ComponentPermission(component, (SwarmAction) action)); 
    }

    private  String buildHiveKey(Component component) {

        if(component == null)
             throw new SecurityException(this.getClass() + ": Specified component is null");

        MarkupContainer markupContainer = findLowestSecureContainer(component);
        String alias = SecureComponentHelper.alias(markupContainer.getClass());
        String relative = (String) component.getMetaData(new ComponentHiveKey(String.class));
        if (relative == null|| "".equals(relative)) return alias;
        else return alias + ":"+ relative;
    }

    private MarkupContainer findLowestSecureContainer(Component component) {

        final MarkupContainer[] lowestSecureParent = new MarkupContainer[1];

        component.visitParents(MarkupContainer.class, new IVisitor<Component>() {
            public Object component(Component component) {
                if(component instanceof ISecureComponent) {
                     lowestSecureParent[0] = (MarkupContainer) component;
                     return IVisitor.STOP_TRAVERSAL;
                }
                return null;
             }
         });

         if (null == lowestSecureParent[0]) {
             try{
               lowestSecureParent[0] = component.getPage();
             } catch(IllegalStateException e) {
                throw new SecurityException(this.getClass() + ": Unable to create alias for component: "+ component, e);
             }
         }

         MarkupContainer markupContainer = lowestSecureParent[0];
         return markupContainer;
    }

    private boolean containsBehavior(org.apache.wicket.Component component, Class<SecurityBehavior> clazz) {

        List<IBehavior> behaviors = component.getBehaviors();
        for(IBehavior object : behaviors) {
             if(object.getClass().isAssignableFrom(clazz)) 
                    return true;
        }
        return false;
    }
}
