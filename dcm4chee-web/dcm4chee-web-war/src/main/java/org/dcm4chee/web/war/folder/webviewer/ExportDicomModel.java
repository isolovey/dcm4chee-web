package org.dcm4chee.web.war.folder.webviewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.dcm4che2.data.Tag;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.folder.SelectedEntities;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PPSModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;

public class ExportDicomModel {
    private final SelectedEntities selected;
    private final Map<String, Map<Integer, Object>> patients;

    public ExportDicomModel(SelectedEntities selected) {
        this.selected = selected;
        this.patients = new HashMap<String, Map<Integer, Object>>();
        init();
    }
    
    public ExportDicomModel(AbstractDicomModel model) {
        this.selected = new SelectedEntities();
        this.patients = new HashMap<String, Map<Integer, Object>>();
        
        if(model instanceof PatientModel){    
            this.selected.getPatients().add((PatientModel) model);
        }
        else if(model instanceof StudyModel){
            this.selected.getStudies().add((StudyModel) model); 
        }
        else if(model instanceof SeriesModel){
            this.selected.getSeries().add((SeriesModel) model);
        }
        else if(model instanceof InstanceModel){
            this.selected.getInstances().add((InstanceModel) model);
        }
        
        init();
    }

    private void init() {
        Iterator<PatientModel> pts = selected.getPatients().iterator();
        while (pts.hasNext()) {
            PatientModel patientModel = pts.next();
            fillStudies(getPatient(patientModel), patientModel.getStudies());
        }

        Iterator<StudyModel> sts = selected.getStudies().iterator();
        while (sts.hasNext()) {
            StudyModel studyModel = sts.next();
            PatientModel patientModel = studyModel.getPatient();

            Map<Integer, Object> patient = getPatient(patientModel);

            List<StudyModel> list = new ArrayList<StudyModel>(1);
            list.add(studyModel);

            fillStudies(patient, list);

            // System.err.println( Arrays.toString(patient.entrySet().toArray()));
        }

        Iterator<SeriesModel> ses = selected.getSeries().iterator();
        while (ses.hasNext()) {
            SeriesModel seriesModel = ses.next();
            StudyModel studyModel = seriesModel.getPPS().getStudy();
            PatientModel patientModel = studyModel.getPatient();

            Map<Integer, Object> patient = getPatient(patientModel);

            Map<String, Map<Integer, Object>> studies = (Map<String, Map<Integer, Object>>) patient.get(0);
            if (studies == null) {
                patient.put(0, studies = new HashMap<String, Map<Integer, Object>>());
            }
            Map<Integer, Object> study = getStudy(studyModel, studies);

            List<SeriesModel> list = new ArrayList<SeriesModel>(1);
            list.add(seriesModel);
            fillSeries(study, list);
        }

        Iterator<InstanceModel> ins = selected.getInstances().iterator();
        while (ins.hasNext()) {
            InstanceModel instanceModel = ins.next();
            SeriesModel seriesModel = instanceModel.getSeries();
            StudyModel studyModel = seriesModel.getPPS().getStudy();
            PatientModel patientModel = studyModel.getPatient();

            Map<Integer, Object> patient = getPatient(patientModel);

            Map<String, Map<Integer, Object>> studies = (Map<String, Map<Integer, Object>>) patient.get(0);
            if (studies == null) {
                patient.put(0, studies = new HashMap<String, Map<Integer, Object>>());
            }
            Map<Integer, Object> study = getStudy(studyModel, studies);

            Map<String, Map<Integer, Object>> series = (Map<String, Map<Integer, Object>>) study.get(0);
            if (series == null) {
                study.put(0, series = new HashMap<String, Map<Integer, Object>>());
            }
            Map<Integer, Object> s = getSeries(seriesModel, series);

            List<InstanceModel> list = new ArrayList<InstanceModel>(1);
            list.add(instanceModel);
            fillInstances(s, list);
        }
    }

    private Map<Integer, Object> getSeries(SeriesModel seriesModel, Map<String, Map<Integer, Object>> series) {
        Map<Integer, Object> s = series.get(seriesModel.getSeriesInstanceUID());
        if (s == null) {
            s = new HashMap<Integer, Object>();
            s.put(Tag.SeriesInstanceUID, seriesModel.getSeriesInstanceUID());
            s.put(Tag.SeriesDescription, seriesModel.getAttributeValueAsString(Tag.SeriesDescription));
            s.put(Tag.Modality, seriesModel.getAttributeValueAsString(Tag.Modality));
            s.put(Tag.SeriesNumber, seriesModel.getSeriesNumber());

            series.put(seriesModel.getSeriesInstanceUID(), s);
        }
        return s;
    }

    private Map<Integer, Object> getStudy(StudyModel studyModel, Map<String, Map<Integer, Object>> studies) {
        Map<Integer, Object> study = studies.get(studyModel.getStudyInstanceUID());

        if (study == null) {
            study = new HashMap<Integer, Object>();
            study.put(Tag.StudyInstanceUID, studyModel.getStudyInstanceUID());
            study.put(Tag.StudyDescription, studyModel.getAttributeValueAsString(Tag.StudyDescription));
            study.put(Tag.StudyDate, studyModel.getAttributeValueAsString(Tag.StudyDate));
            study.put(Tag.StudyTime, studyModel.getAttributeValueAsString(Tag.StudyTime));
            study.put(Tag.StudyID, studyModel.getId());
            study.put(Tag.AccessionNumber, studyModel.getAccessionNumber());
            study.put(Tag.ReferringPhysicianName, studyModel.getAttributeValueAsString(Tag.ReferringPhysicianName));

            studies.put(studyModel.getStudyInstanceUID(), study);
        }
        return study;
    }

    private Map<Integer, Object> getPatient(PatientModel patientModel) {
        Map<Integer, Object> patient = patients.get(patientModel.getIdAndIssuer());

        if (patient == null) {
            patient = new HashMap<Integer, Object>();
            patient.put(Tag.PatientID, patientModel.getId());
            patient.put(Tag.IssuerOfPatientID, patientModel.getIssuer());
            patient.put(Tag.PatientName, patientModel.getName());
            patient.put(Tag.PatientBirthDate, patientModel.getAttributeValueAsString(Tag.PatientBirthDate));
            patient.put(Tag.PatientSex, patientModel.getSex());

            patients.put(patientModel.getIdAndIssuer(), patient);
        }
        return patient;
    }

    private void fillStudies(Map<Integer, Object> patient, List<StudyModel> list) {
        if (list != null && list.size() > 0) {
            setSelectionAtLevel(list, AbstractDicomModel.SERIES_LEVEL, true);
            Map<String, Map<Integer, Object>> studies = (Map<String, Map<Integer, Object>>) patient.get(0);
            if (studies == null) {
                patient.put(0, studies = new HashMap<String, Map<Integer, Object>>());
            }
            for (StudyModel studyModel : list) {
                List<PPSModel> ppSs = studyModel.getPPSs();
                if (ppSs.size() > 0) {
                    Map<Integer, Object> study = getStudy(studyModel, studies);
                    for (PPSModel ppsModel : ppSs) {
                        fillSeries(study, ppsModel.getSeries());
                    }
                }
            }
            resetSelectionAtLevel(list, AbstractDicomModel.SERIES_LEVEL);
        }
    }

    private void fillSeries(Map<Integer, Object> study, List<SeriesModel> list) {
        if (list != null && list.size() > 0) {
            setSelectionAtLevel(list, AbstractDicomModel.INSTANCE_LEVEL, true);
            Map<String, Map<Integer, Object>> series = (Map<String, Map<Integer, Object>>) study.get(0);
            if (series == null) {
                study.put(0, series = new HashMap<String, Map<Integer, Object>>());
            }

            for (SeriesModel seriesModel : list) {
                Map<Integer, Object> s = getSeries(seriesModel, series);
                fillInstances(s, seriesModel.getInstances());
            }
            resetSelectionAtLevel(list, AbstractDicomModel.INSTANCE_LEVEL);
        }
    }

    private void fillInstances(Map<Integer, Object> series, List<InstanceModel> list) {
        if (list != null && list.size() > 0) {
            Map<String, Map<Integer, Object>> instances = (Map<String, Map<Integer, Object>>) series.get(0);
            if (instances == null) {
                series.put(0, instances = new HashMap<String, Map<Integer, Object>>());
            }
            for (InstanceModel IntstanceModel : list) {
                Map<Integer, Object> instance = new HashMap<Integer, Object>();
                instance.put(Tag.SOPInstanceUID, IntstanceModel.getSOPInstanceUID());
                instance.put(Tag.InstanceNumber, IntstanceModel.getInstanceNumber());

                instances.put(IntstanceModel.getSOPInstanceUID(), instance);
            }
        }
    }

    public Map<String, Map<Integer, Object>> getPatients() {
        return patients;
    }
    
    public static void setSelectionAtLevel(Collection<? extends AbstractDicomModel> all, int level, boolean select) {
        for ( AbstractDicomModel m : all ) {
            if ( m.levelOfModel() == level ) {
                m.setSelected(select);
            } else if (m.levelOfModel() < level) {
                Collection<? extends AbstractDicomModel> modelsOfNextLevel = m.getDicomModelsOfNextLevel();
                if (modelsOfNextLevel.size() == 0) {
                    m.expand();
                    modelsOfNextLevel = m.getDicomModelsOfNextLevel();
                }
                setSelectionAtLevel(modelsOfNextLevel, level, select);
            }
        }
    }
    
    public static void resetSelectionAtLevel(Collection<? extends AbstractDicomModel> all, int level) {
        for ( AbstractDicomModel m : all ) {
            if ( m.levelOfModel() == level ) {
                m.setSelected(false);
            } else if (m.levelOfModel() < level) {
                Collection<? extends AbstractDicomModel> modelsOfNextLevel = m.getDicomModelsOfNextLevel();
                if (modelsOfNextLevel.size() > 0) {
                    m.collapse();
                }
            }
        }
    }
  
}
