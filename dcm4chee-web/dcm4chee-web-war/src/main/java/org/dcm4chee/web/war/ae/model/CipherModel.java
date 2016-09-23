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

package org.dcm4chee.web.war.ae.model;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.dcm4chee.archive.entity.AE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 16609 $ $Date: 2012-02-16 16:16:21 +0100 (Do, 16 Feb 2012) $
 * @since June 4, 2009
 */
public class CipherModel implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private AE ae;
    int len;
    List<String> ciphers;
    
    private static Logger log = LoggerFactory.getLogger(CipherModel.class);

    public CipherModel(AE ae, int len) {
        this.ae = ae;
        this.len = len;
        update();
        
    }
    
    public SingleCipherModel getSingleCipherModel(int idx) {
        if (idx < ciphers.size()) {
            return new SingleCipherModel(this, idx);
        } else {
            throw new IllegalArgumentException("Wrong idx, must be less than "+ciphers.size());
        }
    }
    
    public void update() {
        this.ciphers = ae.getCipherSuites();
        if (ciphers.size() > len) {
            log.warn("AE contains more ciphers than CipherModel will handle!");
        }
        while (ciphers.size() < len)
            ciphers.add(null);
    }
    
    public class SingleCipherModel implements IModel<String>{
        CipherModel model;
        int idx;
        public SingleCipherModel(CipherModel m, int idx) {
            model = m;
            this.idx = idx;
        }
        public String getObject() {
            String c = ciphers.get(idx);
            return c == null ? "-" : c;
        }

        public void setObject(String s) {
            model.ciphers.set(idx, "-".equals(s) ? null : s);
            model.ae.setCipherSuites(ciphers);
        }

        public void detach() {
        }
    }
}

