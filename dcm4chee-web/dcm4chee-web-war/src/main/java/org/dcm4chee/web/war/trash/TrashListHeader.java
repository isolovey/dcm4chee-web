package org.dcm4chee.web.war.trash;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.war.AuthenticatedWebSession;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;

public class TrashListHeader extends Panel {

    private static final long serialVersionUID = 1L;
    
    private int headerExpandLevel = 1;
    private int expandAllLevel = 0;
    private Model<Boolean> autoExpand = new Model<Boolean>(false);
 
    private final class Row extends WebMarkupContainer {

        private static final long serialVersionUID = 1L;
        
        private final int entityLevel;

        public Row(String id, int entityLevel) {
            super(id);
            this.entityLevel = entityLevel;
        }

        @Override
        public boolean isVisible() {
            return TrashListHeader.this.headerExpandLevel >= Row.this.entityLevel;
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
                            Cell.this.entityLevel : Cell.this.entityLevel + 1;
                    if (target != null) {
                        target.addComponent(TrashListHeader.this);
                    }
                }
            }.add( new Image("expandImg", new AbstractReadOnlyModel<ResourceReference>() {

                private static final long serialVersionUID = 1L;

                @Override
                public ResourceReference getObject() {
                    return TrashListHeader.this.headerExpandLevel <= Cell.this.entityLevel ? 
                            ImageManager.IMAGE_COMMON_EXPAND : ImageManager.IMAGE_COMMON_COLLAPSE;
                }
            })
            .add(new ImageSizeBehaviour())));
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
           super.onComponentTag(tag);
           tag.put("rowspan", 1 + headerExpandLevel - entityLevel);
        }
    }

    public TrashListHeader(String id) {
        super(id);
        setOutputMarkupId(true);

        add(new AjaxCheckBox("autoExpand", autoExpand) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                headerExpandLevel = autoExpand.getObject() ? 
                        expandAllLevel : 
                            (((AuthenticatedWebSession) getSession()).getTrashViewPort().getFilter().isPatientQuery() ? 
                                    AbstractDicomModel.PATIENT_LEVEL : 
                                        AbstractDicomModel.STUDY_LEVEL);
                target.addComponent(TrashListHeader.this);
            }
        }
        .add(new TooltipBehaviour("trash.search.","autoExpand")));

        Cell patCell = new Cell("cell", 0);

        add(new Row("patient", 0).add(patCell));
        add(new Row("study", 1).add(new Cell("cell", 1)));
        add(new Row("series", 2).add(new Cell("cell", 2)));
        add(new Row("instance", 3));
    }

    /*protected void onBeforeRender() {
        super.onBeforeRender();
        headerExpandLevel = expandAllLevel;
    }*/

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
