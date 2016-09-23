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
 ***** END LICENSE BLOCK ***** */

package org.dcm4chee.web.common.model;

import java.io.Serializable;


/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @param <E>
 * @since May 8, 2011
 */

public interface ProgressProvider extends Serializable {

    public static final int NOT_STARTED = 0;
    public static final int BUSY = 1;
    public static final int PAUSED = 2;
    public static final int WAITING = 3;
    public static final int FINISHED = 4;
    
    String getName();
    
    boolean inProgress();
    int getStatus();
    long getTotal();
    long getSuccessful();
    long getWarnings();
    long getFailures();
    long getRemaining();
    String getResultString();

    long getStartTimeInMillis();
    long getEndTimeInMillis();
    
    /**
     * This (numeric) PageID is used to close pending tasks pages before logout.
     * Therefore the Page must also implement CloseRequestSupport!
     * <code>null</code> means that this provider isn't part of a PopUp page.
     * 
     * @return Page ID of Page using this ExportProvider
     */
    Integer getPopupPageId();
    
    /**
     * The page class name can be used to reopen the page for a ProgressProvider.
     * 
     * @return
     */
    String getPageClassName();
    
    void updateRefreshed();
    long getLastRefreshedTimeInMillis();
}

