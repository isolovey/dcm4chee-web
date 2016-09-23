package org.dcm4chee.web.war.folder;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.war.AuthenticatedWebSession;
import org.dcm4chee.web.war.common.SelectAllCheckBox;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.folder.model.PatientModel;

public class StudyListHeader extends Panel {

    private static final long serialVersionUID = 1L;
    
    private int headerExpandLevel = 1;
    private int expandAllLevel = 0;
    private IModel<Boolean> autoExpand = new Model<Boolean>(false);
    private final IModel<Boolean>hideStudyModel;
    private IModel<Boolean>hidePPSModel;
 
    private final class Row extends WebMarkupContainer {

        private static final long serialVersionUID = 1L;
        
        private final int entityLevel;
        private IModel<Boolean> hide;

        public Row(String id, int entityLevel) {
            super(id);
            this.entityLevel = entityLevel;
        }
        
        private Row setVisibleModel(IModel<Boolean> hide) {
            this.hide = hide;
            return this;
        }

        @Override
        public boolean isVisible() {
            return StudyListHeader.this.headerExpandLevel >= Row.this.entityLevel && 
            (hide == null || !hide.getObject());
        }
    }

    private final class Cell extends WebMarkupContainer {

        private static final long serialVersionUID = 1L;
        
        private final int entityLevel;

        public Cell(String id, int entityLevel) {
            super(id);
            this.entityLevel = entityLevel;
            add(new AjaxFallbackLink<Object>("expand"){

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    headerExpandLevel = headerExpandLevel > Cell.this.entityLevel ?
                            Cell.this.entityLevel : Cell.this.entityLevel + getLevelIncrement();
                    if (target != null) {
                        target.addComponent(StudyListHeader.this);
                    }
                }

                private int getLevelIncrement() {
                    int inc = 1;
                    int level = Cell.this.entityLevel;
                    if (level == AbstractDicomModel.PATIENT_LEVEL && isHideStudy()) {
                        inc++;
                    }
                    if (level == AbstractDicomModel.STUDY_LEVEL && isHidePPS()) {
                        inc++;
                    }
                    return inc;
                }
            }.add( new Image("expandImg", new AbstractReadOnlyModel<ResourceReference>() {

                private static final long serialVersionUID = 1L;

                @Override
                public ResourceReference getObject() {
                    return StudyListHeader.this.headerExpandLevel <= Cell.this.entityLevel ? 
                            ImageManager.IMAGE_COMMON_EXPAND : ImageManager.IMAGE_COMMON_COLLAPSE;
                }
            })
            .add(new ImageSizeBehaviour())));
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
           super.onComponentTag(tag);
           tag.put("rowspan", 1 + headerExpandLevel - entityLevel);
           if (entityLevel == AbstractDicomModel.PPS_LEVEL && isHideStudy() ||
                   entityLevel == AbstractDicomModel.SERIES_LEVEL && isHidePPS()) {
               tag.put("colspan", "2");
           }
        }
    }

    public StudyListHeader(String id, Component toUpd, final ViewPort viewport, IModel<Boolean> hidePPSModel) {
        super(id);
        setOutputMarkupId(true);
        this.hidePPSModel = hidePPSModel;
        hideStudyModel = new IModel<Boolean>(){
            private static final long serialVersionUID = 1L;
            public void detach() {}
            public void setObject(Boolean arg0) {}

            public Boolean getObject() {
                return viewport.getFilter().getUnconnectedMPPSSearched();
            }
        };
        
        add(new AjaxCheckBox("autoExpand", autoExpand) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                headerExpandLevel = autoExpand.getObject() ? 
                        expandAllLevel : 
                            (((AuthenticatedWebSession) getSession()).getFolderViewPort().getFilter().isPatientQuery() ? 
                                    AbstractDicomModel.PATIENT_LEVEL : 
                                        AbstractDicomModel.STUDY_LEVEL);
                target.addComponent(StudyListHeader.this);
            }
        }
        .add(new TooltipBehaviour("folder.search.","autoExpand")));

        Cell patCell = new Cell("cell", 0);
        List<PatientModel> patients = ((AuthenticatedWebSession) getSession()).getFolderViewPort().getPatients();
        toUpd.setOutputMarkupId(true);
        add(new Row("patient", PatientModel.PATIENT_LEVEL).add(patCell)
        		.add(new SelectAllCheckBox("toggleSelectAll", patients, PatientModel.PATIENT_LEVEL, toUpd, true)));
        add(new Row("study", PatientModel.STUDY_LEVEL).setVisibleModel(hideStudyModel).add(new Cell("cell", 1))
        		.add(new SelectAllCheckBox("toggleSelectAll", patients, PatientModel.STUDY_LEVEL, toUpd, true)));
        add(new Row("pps", PatientModel.PPS_LEVEL).setVisibleModel(hidePPSModel).add(new Cell("cell", 2)));
        add(new Row("series", PatientModel.SERIES_LEVEL).add(new Cell("cell", 3))
        		.add(new SelectAllCheckBox("toggleSelectAll", patients, PatientModel.SERIES_LEVEL, toUpd, true)));
        add(new Row("instance", PatientModel.INSTANCE_LEVEL).add(new Cell("cell", 4)));
        add(new Row("file", 5));
    }

    private boolean isHidePPS() {
        return hidePPSModel != null && hidePPSModel.getObject();
    }
    private boolean isHideStudy() {
        return hideStudyModel != null && hideStudyModel.getObject();
    }
    
    public void setExpandAllLevel(int expandAllLevel) {
        this.expandAllLevel = expandAllLevel;
        if (autoExpand.getObject())
            this.headerExpandLevel = expandAllLevel;
    }
    
    public int getExpandAllLevel() {
        return expandAllLevel;
    }

    public void expandToLevel(int level) {
        headerExpandLevel = level;
    }
}
