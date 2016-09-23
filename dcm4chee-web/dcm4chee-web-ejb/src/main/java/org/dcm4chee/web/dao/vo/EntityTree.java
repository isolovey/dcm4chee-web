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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
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
package org.dcm4chee.web.dao.vo;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Apr 10, 2010
 */
public class EntityTree implements Serializable {

    private static final long serialVersionUID = 3468299157101603139L;
    
    private Collection<Instance> allInstances;
    private Map<Patient, Map<Study, Map<Series, Set<Instance>>>> entityTreeMap = 
        new HashMap<Patient, Map<Study, Map<Series, Set<Instance>>>>();
    private Set<MWLItem> mwlItems = new HashSet<MWLItem>();
    private Map<String, String> old2newUidMap = new HashMap<String, String>();
    private Map<String, String> new2oldUidMap = new HashMap<String, String>();
    private boolean containsChangedEntities;
    
    public Set<MWLItem> getMwlItems() {
        return mwlItems;
    }

    private static Logger log = LoggerFactory.getLogger(EntityTree.class);
    
    public EntityTree() {
        allInstances = new HashSet<Instance>();
    }
    public EntityTree(int size) {
        allInstances = new HashSet<Instance>(size);
    }
    public EntityTree(Collection<Instance> instances) {
        this(instances.size());
        addInstances(instances);
    }

    public EntityTree addInstances(Collection<Instance> instances) {
        for (Instance i : instances) {
            this.addInstance(i);
        }
        return this;
    }
    
    public void addInstance(Instance instance) {
        if ( !allInstances.add(instance)) {
            log.warn("Instance already in this EntityTree! :"+instance.getSOPInstanceUID());
            return;
        }
        Series series = instance.getSeries();
        MPPS mpps = series.getModalityPerformedProcedureStep();
        if (mpps != null) mpps.getPatient();
        Study study = series.getStudy();
        Patient pat = study.getPatient();
        Map<Study, Map<Series, Set<Instance>>> mapStudies = entityTreeMap.get(pat);
        if (mapStudies == null) {
            mapStudies = new HashMap<Study, Map<Series, Set<Instance>>>();
            entityTreeMap.put(pat, mapStudies);
        }
        Map<Series, Set<Instance>> mapSeries = mapStudies.get(study);
        Set<Instance> instances;
        if (mapSeries == null) {
            mapSeries = new HashMap<Series, Set<Instance>>();
            instances = new HashSet<Instance>();
            mapSeries.put(series, instances);
            mapStudies.put(study, mapSeries);
        } else {
            instances = mapSeries.get(series);
            if (instances == null) {
                instances = new HashSet<Instance>();
                mapSeries.put(series, instances);
            }
        }
        instances.add(instance);
    }
    
    public void addSeries(Series series) {
        Study study = series.getStudy();
        Patient pat = study.getPatient();
        Map<Series, Set<Instance>> mapSeries;
        Map<Study, Map<Series, Set<Instance>>> mapStudies = entityTreeMap.get(pat);
        if (mapStudies == null) {
            mapStudies = new HashMap<Study, Map<Series, Set<Instance>>>();
            entityTreeMap.put(pat, mapStudies);
            mapSeries = new HashMap<Series, Set<Instance>>();
            mapStudies.put(study, mapSeries);
        } else {
            mapSeries = mapStudies.get(study);
        }
        Set<Instance> instances = series.getInstances();
        mapSeries.put(series, instances);
        if (instances != null)
            allInstances.addAll(instances);
    }

    public void addStudy(Study study) {
        Patient pat = study.getPatient();
        Map<Series, Set<Instance>> mapSeries = new HashMap<Series, Set<Instance>>();
        Map<Study, Map<Series, Set<Instance>>> mapStudies = entityTreeMap.get(pat);
        if (mapStudies == null) {
            mapStudies = new HashMap<Study, Map<Series, Set<Instance>>>();
            entityTreeMap.put(pat, mapStudies);
        }
        mapStudies.put(study, mapSeries);
        Set<Instance> instances;
        for (Series series : study.getSeries()) {
            instances = series.getInstances();
            MPPS mpps = series.getModalityPerformedProcedureStep();
            if (mpps != null)
                mpps.getPatient();
            mapSeries.put(series, instances);
            allInstances.addAll(series.getInstances());
            if (instances != null)
                allInstances.addAll(instances);
        }
    }
    
    public boolean removeStudy(Study study) {
        Map<Study, Map<Series, Set<Instance>>> mapStudies = entityTreeMap.get(study.getPatient());
        if (mapStudies != null) {
            return mapStudies.remove(study) != null;
        } else {
            Study st = null;
            for (Map<Study, Map<Series, Set<Instance>>> studies : entityTreeMap.values()) {
                for (Iterator<Study> it = studies.keySet().iterator() ; it.hasNext() ;) {
                    st = it.next();
                    if (st.getPk() == study.getPk())
                        break;
                    st = null;
                }
                log.info("st:"+st);
                if (st != null && studies.remove(st) != null)
                    return true;
            }
            log.warn("Patient of study to remove not in entityTreeMap and study not found in EntityTree!");
            return false;
        }
    }
    
    public void addPatient(Patient pat) {
        if (entityTreeMap.get(pat) == null) {
            entityTreeMap.put(pat, new HashMap<Study, Map<Series, Set<Instance>>>());
        }
    }
    
    public void addMWLItem(MWLItem mwl) {
        mwlItems.add(mwl);
    }
    
    public boolean isEmpty() {
        return allInstances.isEmpty();
    }
    public Collection<Instance> getAllInstances() {
        return allInstances;
    }
    
    public Map<Patient, Map<Study, Map<Series, Set<Instance>>>> getEntityTreeMap() {
        return entityTreeMap;
    }
    
    public boolean isContainsChangedEntities() {
        return containsChangedEntities;
    }
    public void setContainsChangedEntities(boolean b) {
        this.containsChangedEntities = b;
    }
    
    public String getChangedUID(String oldUID) {
        String newUID = old2newUidMap.get(oldUID);
        if (newUID == null) {
            newUID = UIDUtils.createUID();
            old2newUidMap.put(oldUID, newUID);
            new2oldUidMap.put(newUID, oldUID);
        }
        return newUID;
    }
    
    public Map<String,String> getUIDMap() {
        return this.containsChangedEntities ? new2oldUidMap : old2newUidMap;
    }
    
    public Map<String,String> getOld2NewUIDMap() {
        return this.old2newUidMap;
    }
}

