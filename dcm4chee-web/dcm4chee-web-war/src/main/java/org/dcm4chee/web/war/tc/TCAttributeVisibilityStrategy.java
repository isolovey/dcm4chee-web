package org.dcm4chee.web.war.tc;

import java.io.Serializable;

import org.apache.wicket.model.IModel;

@SuppressWarnings("serial")
public class TCAttributeVisibilityStrategy implements Serializable {
	
	private IModel<Boolean> trainingModeModel;
	private IModel<Boolean> editModeModel;
	private boolean showAllIfTrainingModeIsOn;
	
	public TCAttributeVisibilityStrategy() {
		this(null);
	}
	
	public TCAttributeVisibilityStrategy(
			IModel<Boolean> trainingModeModel) {
		this(null, trainingModeModel);
	}
	
	public TCAttributeVisibilityStrategy(
			IModel<Boolean> editModeModel, 
			IModel<Boolean> trainingModeModel) {
		this.trainingModeModel = trainingModeModel;
		this.editModeModel = editModeModel;
	}

	public final boolean getShowAllIfTrainingModeIsOn() {
		return showAllIfTrainingModeIsOn;
	}
	
	public final void setShowAllIfTrainingModeIsOn(boolean showAll) {
		this.showAllIfTrainingModeIsOn = showAll;
	}
	
	public final boolean isEditModeOn() {
		Boolean enabled = editModeModel!=null ?
				editModeModel.getObject() : null;
		return enabled!=null && Boolean.TRUE.equals(enabled);
	}
	
	public final boolean isTrainingModeOn() {
		Boolean enabled = trainingModeModel!=null ?
				trainingModeModel.getObject() : null;
		return enabled!=null && Boolean.TRUE.equals(enabled);
	}

	public boolean isAttributeVisible(TCAttribute attr) {
		if (attr.isRestricted()) {
			if (!isEditModeOn()) {
				if (isTrainingModeOn()) {
					return showAllIfTrainingModeIsOn;
				}
			}
		}
		return true;
	}
}