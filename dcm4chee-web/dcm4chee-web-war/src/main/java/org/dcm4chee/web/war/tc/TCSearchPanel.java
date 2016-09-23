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
package org.dcm4chee.web.war.tc;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converters.DateConverter;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.dao.tc.TCQueryFilter;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.YesNo;
import org.dcm4chee.web.war.common.AutoSelectInputTextBehaviour;
import org.dcm4chee.web.war.tc.TCUtilities.NullDropDownItem;
import org.dcm4chee.web.war.tc.widgets.TCAjaxComboBox;
import org.dcm4chee.web.war.tc.widgets.TCMaskingAjaxDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since April 28, 2011
 */
public abstract class TCSearchPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
	private static final SimpleDateFormat dfDE = new SimpleDateFormat("dd.MM.yyyy");
	private static final SimpleDateFormat dfDefault = new SimpleDateFormat("MM/dd/yyyy");

    private static final Logger log = LoggerFactory
            .getLogger(TCSearchPanel.class);

    private static enum Option {
        AuthorName, AuthorContact, AuthorAffiliation, History, Discussion, Title, Abstract, PatientSpecies
    }

    private boolean showSearch = true;

    private boolean showAdvancedOptions = false;

    @SuppressWarnings({ "serial" })
	public TCSearchPanel(final String id) {
        super(id, new Model<TCQueryFilter>(new TCQueryFilter()));

        setOutputMarkupId(true);
        
        final DateSpanSearchItem dateSpanItem = new DateSpanSearchItem();
        final DateSpanDialog dateSpanDialog = new DateSpanDialog(dateSpanItem);
        final List<IDateSearchItem> dateItems = new ArrayList<IDateSearchItem>();
        dateItems.addAll(Arrays.asList(NotOlderThanSearchItem.values()));
        dateItems.add(dateSpanItem);
        
        Form<?> dateSpanDialogOuterForm = new Form<Void>("date-input-dialog-outer-form");
        dateSpanDialogOuterForm.setOutputMarkupId(true);
        dateSpanDialogOuterForm.setMarkupId("tc-search-date-input-form-helper");
        dateSpanDialogOuterForm.add(dateSpanDialog);
        
        final TCInput keywordInput = TCUtilities.createInput(
                "keywordInput", TCQueryFilterKey.Keyword, getFilterValue(TCQueryFilterKey.Keyword), true);
        final TCInput anatomyInput = TCUtilities.createInput(
                "anatomyInput", TCQueryFilterKey.Anatomy, getFilterValue(TCQueryFilterKey.Anatomy), true);
        final TCInput pathologyInput = TCUtilities.createInput(
                "pathologyInput", TCQueryFilterKey.Pathology, getFilterValue(TCQueryFilterKey.Pathology), true);
        final TCInput findingInput = TCUtilities.createInput(
                "findingInput", TCQueryFilterKey.Finding, getFilterValue(TCQueryFilterKey.Finding), true);
        final TCInput diagnosisInput = TCUtilities.createInput(
                "diagnosisInput", TCQueryFilterKey.Diagnosis, getFilterValue(TCQueryFilterKey.Diagnosis), true);
        final TCInput diffDiagnosisInput = TCUtilities.createInput(
                "diffDiagnosisInput", TCQueryFilterKey.DifferentialDiagnosis, getFilterValue(TCQueryFilterKey.DifferentialDiagnosis), true);
        final TextField<String> textText = new TextField<String>("textText",
                new Model<String>(""));
        textText.add(new AutoSelectInputTextBehaviour());
        
        final DropDownChoice<TCQueryFilterValue.AcquisitionModality> modalityChoice = TCUtilities.createDropDownChoice(
                "modalityChoice",
                new Model<TCQueryFilterValue.AcquisitionModality>(),
                Arrays.asList(TCQueryFilterValue.AcquisitionModality.values()),
                NullDropDownItem.All);
        final DropDownChoice<TCQueryFilterValue.PatientSex> patientSexChoice = TCUtilities.createEnumDropDownChoice(
                "patientSexChoice", new Model<TCQueryFilterValue.PatientSex>(),
                Arrays.asList(TCQueryFilterValue.PatientSex.values()), true,
                "tc.patientsex", NullDropDownItem.All);
        final DropDownChoice<TCQueryFilterValue.Category> categoryChoice = TCUtilities.createEnumDropDownChoice(
                "categoryChoice", new Model<TCQueryFilterValue.Category>(),
                Arrays.asList(TCQueryFilterValue.Category.values()), true,
                "tc.category", NullDropDownItem.All);
        final DropDownChoice<TCQueryFilterValue.Level> levelChoice = TCUtilities.createEnumDropDownChoice(
                "levelChoice", new Model<TCQueryFilterValue.Level>(),
                Arrays.asList(TCQueryFilterValue.Level.values()), true,
                "tc.level", NullDropDownItem.All);
        final DropDownChoice<TCQueryFilterValue.YesNo> diagnosisConfirmedChoice = TCUtilities.createEnumDropDownChoice(
                "diagnosisConfirmedChoice",
                new Model<TCQueryFilterValue.YesNo>(),
                Arrays.asList(TCQueryFilterValue.YesNo.values()), true,
                "tc.yesno", NullDropDownItem.All);
		final TCAjaxComboBox<IDateSearchItem> dateBox = new TCAjaxComboBox<IDateSearchItem>(
        		"dateChoice", dateItems, new IChoiceRenderer<IDateSearchItem>() {
        			public String getIdValue(IDateSearchItem item, int index) {
        				return item.getId();
        			}
        			public String getDisplayValue(IDateSearchItem item) {
        				return item.getLabel(getSession().getLocale());
        			}
        		}) {
        	@Override
        	protected IDateSearchItem convertValue(String svalue) {
        		if (TCUtilities.equals(dateSpanItem.getLabel(getSession().getLocale()),svalue)) {
        			return dateSpanItem;
        		}
        		else {
        			return NotOlderThanSearchItem.valueForLabel(
        					svalue, getSession().getLocale());
        		}
        	}
        	@Override
        	protected boolean shallCommitValue(IDateSearchItem oldValue, IDateSearchItem newValue, AjaxRequestTarget target) {
        		if (dateSpanItem==newValue) {
        			final Component c = this;
        			dateSpanDialog.setWindowClosedCallback(new WindowClosedCallback() {
        				@Override
        				public void onClose(AjaxRequestTarget target) {
        			        target.appendJavascript(getDateBoxInitUIJavascript(
        			        		c.getMarkupId(true), dateSpanItem, false));
        				}
        			});
        			dateSpanDialog.show(target);
            		return true;
        		}

        		return super.shallCommitValue(oldValue, newValue, target);
        	}
        };
        TCUtilities.addOnDomReadyJavascript(dateBox, getDateBoxInitUIJavascript(
        		dateBox.getMarkupId(), dateSpanItem, true));
        
        final RadioGroup<Option> optionGroup = new RadioGroup<Option>(
                "optionGroup", new Model<Option>());
        optionGroup.add(new Radio<Option>("historyOption", new Model<Option>(
                Option.History)));
        optionGroup.add(new Radio<Option>("authorNameOption",
                new Model<Option>(Option.AuthorName)));
        optionGroup.add(new Radio<Option>("authorContactOption",
                new Model<Option>(Option.AuthorContact)));
        optionGroup.add(new Radio<Option>("authorOrganisationOption",
                new Model<Option>(Option.AuthorAffiliation)));
        optionGroup.add(new Radio<Option>("discussionOption",
                new Model<Option>(Option.Discussion)));
        optionGroup.add(new Radio<Option>("titleOption", new Model<Option>(
                Option.Title)));
        optionGroup.add(new Radio<Option>("abstractOption", new Model<Option>(
                Option.Abstract)));
        optionGroup.add(new Radio<Option>("patientSpeciesOption",
                new Model<Option>(Option.PatientSpecies)));

        final AjaxButton searchBtn = new AjaxButton(
                "doSearchBtn") {

            private static final long serialVersionUID = 1L;

            private IAjaxCallDecorator decorator;
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                	try {
                		findParent(TCPanel.class).getPopupManager().hidePopups(target);
                	}
                	catch (Exception e) {
                		log.error("Error while closing popups!", e);
                	}
                	
                    TCQueryFilter filter = (TCQueryFilter) TCSearchPanel.this
                            .getDefaultModelObject();
                    filter.clear();
                    
                    filter.setKeywords(keywordInput.getValues());

                    if (showAdvancedOptions)
                    {
	                    filter.setAnatomy(anatomyInput.getValue());
	                    filter.setPathology(pathologyInput.getValue());
	                    filter.setFinding(findingInput.getValue());
	                    filter.setDiagnosis(diagnosisInput.getValue());
	                    filter.setDiffDiagnosis(diffDiagnosisInput.getValue());
		                    filter.setAcquisitionModality(modalityChoice
	                            .getModelObject());
	                    filter.setPatientSex(patientSexChoice.getModelObject());
	                    filter.setCategory(categoryChoice.getModelObject());
	                    filter.setLevel(levelChoice.getModelObject());
	                    
	                    YesNo yesNo = diagnosisConfirmedChoice.getModelObject();
	                    if (YesNo.Yes.equals(yesNo))
	                    {
	                    	filter.setDiagnosisConfirmed( yesNo );
	                    }
	                    
	                    IDateSearchItem dateItem = dateBox.getModelObject();
	                    if (dateItem==null) {
	                    	filter.setCreationDate(null, null);
	                    }
	                    else {
	                    	filter.setCreationDate(dateItem.getFromDate(), 
	                    			dateItem.getUntilDate());
	                    }
	
	                    Option selectedOption = optionGroup.getModelObject();
	                    if (selectedOption != null) {
	                        if (Option.History.equals(selectedOption)) {
	                            filter.setHistory(textText
	                                    .getDefaultModelObjectAsString());
	                        } else if (Option.AuthorName.equals(selectedOption)) {
	                            filter.setAuthorName(textText
	                                    .getDefaultModelObjectAsString());
	                        } else if (Option.AuthorContact.equals(selectedOption)) {
	                            filter.setAuthorContact(textText
	                                    .getDefaultModelObjectAsString());
	                        } else if (Option.AuthorAffiliation
	                                .equals(selectedOption)) {
	                            filter.setAuthorAffiliation(textText
	                                    .getDefaultModelObjectAsString());
	                        } else if (Option.Title.equals(selectedOption)) {
	                            filter.setTitle(textText
	                                    .getDefaultModelObjectAsString());
	                        } else if (Option.Abstract.equals(selectedOption)) {
	                            filter.setAbstract(textText
	                                    .getDefaultModelObjectAsString());
	                        } else if (Option.PatientSpecies.equals(selectedOption)) {
	                            filter.setPatientSpecies(textText
	                                    .getDefaultModelObjectAsString());
	                        } else if (Option.Discussion.equals(selectedOption)) {
	                            filter.setDiscussion(textText
	                                    .getDefaultModelObjectAsString());
	                        }
	                    }
                    }
                    
                    Component[] toUpdate = doSearch(filter);

                    if (toUpdate != null && target != null) {
                        for (Component c : toUpdate) {
                            target.addComponent(c);
                        }
                    }
                } catch (Throwable t) {
                    log.error("Searching for teaching-files failed!", t);
                }
            }

            @Override
            public void onError(AjaxRequestTarget target, Form<?> form) {
                BaseForm.addInvalidComponentsToAjaxRequestTarget(target, form);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
            	if (decorator==null)
            	{
            		decorator = new TCMaskingAjaxDecorator(false, true);
            	}
            	return decorator;
            }
        };

        searchBtn.setOutputMarkupId(true);
        searchBtn
                .add(new Image("doSearchImg", ImageManager.IMAGE_COMMON_SEARCH)
                        .add(new ImageSizeBehaviour("vertical-align: middle;")));
        searchBtn.add(new Label("doSearchText", new ResourceModel(
                "tc.search.dosearch.text")).add(
                new AttributeModifier("style", true, new Model<String>(
                        "vertical-align: middle;"))).setOutputMarkupId(true));

        AjaxButton resetBtn = new AjaxButton("resetSearchBtn") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                TCQueryFilter filter = (TCQueryFilter) TCSearchPanel.this
                        .getDefaultModelObject();
                filter.clear();

                keywordInput.setValues();
                anatomyInput.setValues();
                pathologyInput.setValues();
                findingInput.setValues();
                diagnosisInput.setValues();
                diffDiagnosisInput.setValues();
                modalityChoice.setModelObject(null);
                levelChoice.setModelObject(null);
                patientSexChoice.setModelObject(null);
                categoryChoice.setModelObject(null);
                diagnosisConfirmedChoice.setModelObject(null);
                dateBox.setModelObject(null);
                textText.setModelObject(null);
                optionGroup.setModelObject(null);

                target.addComponent(form);
                target.appendJavascript("initUI($('#" + TCSearchPanel.this.getMarkupId(true) + "'));");
            }

            @Override
            public void onError(AjaxRequestTarget target, Form<?> form) {
                BaseForm.addInvalidComponentsToAjaxRequestTarget(target, form);
            }
        };
        resetBtn.add(new Image("resetSearchImg",
                ImageManager.IMAGE_COMMON_RESET).add(new ImageSizeBehaviour(
                "vertical-align: middle;")));
        resetBtn.add(new Label("resetSearchText", new ResourceModel(
                "tc.search.reset.text")).add(new AttributeModifier("style",
                true, new Model<String>("vertical-align: middle;"))));

        final WebMarkupContainer wmc = new WebMarkupContainer("advancedOptions");
        wmc.setOutputMarkupPlaceholderTag(true);
        wmc.setOutputMarkupId(true);
        wmc.setVisible(false);
        
        wmc.add(anatomyInput.getComponent());
        wmc.add(pathologyInput.getComponent());
        wmc.add(findingInput.getComponent());
        wmc.add(diagnosisInput.getComponent());
        wmc.add(diffDiagnosisInput.getComponent());
        wmc.add(modalityChoice);
        wmc.add(patientSexChoice);
        wmc.add(categoryChoice);
        wmc.add(levelChoice);
        wmc.add(diagnosisConfirmedChoice);
        wmc.add(dateBox);
        wmc.add(optionGroup);
        wmc.add(textText);
        wmc.add(resetBtn);

        final MarkupContainer advancedOptionsToggleLink = new AjaxFallbackLink<String>(
                "advancedOptionsToggle") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showAdvancedOptions = !showAdvancedOptions;

                wmc.setVisible(showAdvancedOptions);

                target.addComponent(wmc);
                target.addComponent(this);
                
                if (showAdvancedOptions)
                {
                    target.appendJavascript("initUI($('#" + wmc.getMarkupId(true) + "'));");
                }
            }
        }.add(new Label("advancedOptionsToggleText",
                new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = -7928173606391768738L;
					@Override
                    public String getObject() {
                        return showAdvancedOptions ? getString("tc.search.advancedOptions.hide.Text")
                                : getString("tc.search.advancedOptions.show.Text");
                    }
                })).add(
                (new Image("advancedOptionsToggleImg",
                        new AbstractReadOnlyModel<ResourceReference>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public ResourceReference getObject() {
                                return showAdvancedOptions ? ImageManager.IMAGE_COMMON_COLLAPSE
                                        : ImageManager.IMAGE_COMMON_EXPAND;
                            }
                        })).add(new ImageSizeBehaviour()));
        advancedOptionsToggleLink.setOutputMarkupId(true);

        final Form<?> form = new Form<Object>("searchForm");
        form.add(keywordInput.getComponent());
        form.add(wmc);
        form.add(searchBtn);
        form.setDefaultButton(searchBtn);
        form.setOutputMarkupPlaceholderTag(true);

        form.add(advancedOptionsToggleLink);

        add(dateSpanDialogOuterForm);
        add(form);

        add(new AjaxFallbackLink<Object>("searchToggle") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showSearch = !showSearch;

                form.setVisible(showSearch);

                target.addComponent(TCSearchPanel.this);
                
                if (showSearch)
                {
                	target.appendJavascript("initUI($('#" + TCSearchPanel.this.getMarkupId(true) + "'));");
                }
            }
        }.add((new Image("searchToggleImg",
                new AbstractReadOnlyModel<ResourceReference>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public ResourceReference getObject() {
                        return showSearch ? ImageManager.IMAGE_COMMON_COLLAPSE
                                : ImageManager.IMAGE_COMMON_EXPAND;
                    }
                })).add(new ImageSizeBehaviour())));
    }
    
    public void redoSearch(AjaxRequestTarget target)
    {
        redoSearch(target, null);
    }
    
    public void redoSearch(AjaxRequestTarget target, String iuid)
    {
        Component[] toUpdate = doSearch((TCQueryFilter)getDefaultModel().getObject());

        if (toUpdate != null && target != null) {
            for (Component c : toUpdate) {
                target.addComponent(c);
            }
        }
    }

    protected abstract Component[] doSearch(TCQueryFilter filter);

    private Object getFilterValue(TCQueryFilterKey key)
    {
        TCQueryFilter filter = (TCQueryFilter) getDefaultModelObject();
        return filter != null ? filter.getValue(key) : null;
    }
    
	private String getDateBoxInitUIJavascript(String markupId, DateSpanSearchItem dateSpanItem, boolean initUI) {
		StringBuilder js = new StringBuilder();
		if (initUI) {
			js.append("$('#").append(markupId).append("').combobox();");
		}
        js.append("if ($('#").append(markupId).append(" option:selected').text()==='").append(
        		dateSpanItem.getLabel(getSession().getLocale())).append("') {");
        js.append("$('#").append(markupId).append("').siblings('span').children('input').val('").append(
        		dateSpanItem.getFromUntilString(getSession().getLocale())).append("');");
        js.append("};");
        return js.toString();
	}
    
    public static interface IDateSearchItem extends Serializable {
    	String getId();
    	String getLabel(Locale locale);
    	Date getFromDate();
    	Date getUntilDate();
    }
    
    public static enum NotOlderThanSearchItem implements IDateSearchItem {
    	DAYS7(Calendar.DATE,-7), DAYS14(Calendar.DATE,-14), 
    	MONTHS1(Calendar.MONTH,-1), MONTHS3(Calendar.MONTH,-3), MONTHS6(Calendar.MONTH,-6),
    	YEARS1(Calendar.YEAR,-1), YEARS5(Calendar.YEAR,-5), YEARS10(Calendar.YEAR,-10);
    	private static final transient Date now = new Date();
    	private transient Calendar cal = Calendar.getInstance();
    	private int calField;
    	private int amount;
    	private NotOlderThanSearchItem(int calField, int amount) {
    		this.calField = calField;
    		this.amount = amount;
    	}
    	public static NotOlderThanSearchItem valueForLabel(String label, Locale locale) {
    		for (NotOlderThanSearchItem item : values()) {
    			if (item.getLabel(locale).equals(label)) {
    				return item;
    			}
    		}
    		return null;
    	}
    	@Override
    	public String getId() {
    		return name();
    	}
    	@Override
    	public String getLabel(Locale locale) {
    		return toString();
    	}
    	@Override
    	public Date getFromDate() {
    		cal.setTime(now);
    		cal.add(calField, amount);
    		return cal.getTime();
    	}
    	@Override
    	public Date getUntilDate() {
    		return now;
    	}
    	@Override
    	public String toString() {
    		return TCUtilities.getLocalizedString("tc.search."+name().toLowerCase()+".text");
    	}
    }
    
    @SuppressWarnings("serial")
	public static class DateSpanSearchItem implements IDateSearchItem {
    	public static final String ID = "USER_DEFINED";
    	private Date now = new Date();
    	private Date fromDate = now;
    	private Date untilDate = now;
    	@Override
    	public Date getFromDate() {
    		return fromDate;
    	}
    	@Override
    	public Date getUntilDate() {
    		return untilDate;
    	}
    	@Override
    	public String getId() {
    		return ID;
    	}
    	@Override
    	public String getLabel(Locale locale) {
    		return TCUtilities.getLocalizedString("tc.search.datespan.text");
    	}
    	public void setFromDate(Date date) {
    		fromDate = date==null ? now : date;
    	}
    	public void setUntilDate(Date date) {
    		untilDate = date==null ? now : date;
    	}
    	public String getFromString() {
    		return dfDefault.format(fromDate);
    	}
    	public String getLocalizedFromString(Locale locale) {
			String lang = locale.getLanguage();
			if (Locale.GERMAN.getLanguage().equalsIgnoreCase(lang)) {
	    		return dfDE.format(fromDate);
			}
			else {
	    		return dfDefault.format(fromDate);
			}
    	}
    	public String getUntilString() {
    		return dfDefault.format(untilDate);
    	}
    	public String getLocalizedUntilString(Locale locale) {
			String lang = locale.getLanguage();
			if (Locale.GERMAN.getLanguage().equalsIgnoreCase(lang)) {
	    		return dfDE.format(untilDate);
			}
			else {
	    		return dfDefault.format(untilDate);
			}
    	}
    	public String getFromUntilString(Locale locale) {
			StringBuilder s = new StringBuilder();
			s.append(getLocalizedFromString(locale));
			s.append("-");
			s.append(getLocalizedUntilString(locale));
    		return s.toString();
    	}
    	@Override
    	public String toString() {
    		return getId();
    	}
    }
    
    
    @SuppressWarnings("serial")
	private class DateSpanDialog extends ModalWindow {
    	private DateSpanSearchItem item;
    	private DateSpanContentFragment fragment;
    	public DateSpanDialog(DateSpanSearchItem item)
    	{
    		super("date-input-dialog");
    		this.item = item;
    		setTitle(TCUtilities.getLocalizedString(
    				"tc.search.datespan.dialog.title"));
    		setInitialHeight(140);
    		setInitialWidth(350);
    		setContent(fragment = 
    				new DateSpanContentFragment(getContentId()));
    	}
    	
    	@Override
    	public void close(AjaxRequestTarget target) {
			item.setFromDate(fragment.getFromDate());
			item.setUntilDate(fragment.getUntilDate());
			super.close(target);
    	}
    	
    	@Override
    	public void show(AjaxRequestTarget target) {
    		fragment.initDatePicker(target);
    		setTitle(TCUtilities.getLocalizedString(
    				"tc.search.datespan.dialog.title"));
    		super.show(target);
    	}
    	
    	@Override
    	public String getCssClassName()
    	{
    		return super.getCssClassName() + " tc-dialog";
    	}

    	private class DateSpanContentFragment extends Fragment
    	{
			private DateConverter converter = new DateConverter() {
				@Override
				public DateFormat getDateFormat(Locale locale)
				{
					if (locale == null) {
						locale = Locale.getDefault();
					}

					String lang = locale.getLanguage();
					if (Locale.GERMAN.getLanguage().equalsIgnoreCase(lang)) {
			    		return dfDE;
					}

			    	return dfDefault;
				}
			};
			
    		private Model<Date> fromModel = new Model<Date>(item.getFromDate());
    		private Model<Date> untilModel = new Model<Date>(item.getUntilDate());
    		
    		private TextField<Date> fromField = new TextField<Date>(
					"date-input-dialog-from-input", fromModel, Date.class) {
				@Override
				public IConverter getConverter(Class<?> type) {
					if (type!=null) {
						if (type.equals(Date.class)) {
							return converter;
						}
					}
					return super.getConverter(type);
				}
			};
			
    		private TextField<Date> untilField = new TextField<Date>(
					"date-input-dialog-until-input", untilModel, Date.class) {
				@Override
				public IConverter getConverter(Class<?> type) {
					if (type!=null) {
						if (type.equals(Date.class)) {
							return converter;
						}
					}
					return super.getConverter(type);
				}
			};

    		
			public DateSpanContentFragment(final String id)
    		{
    			super(id, "date-input-dialog-content", TCSearchPanel.this);

    			fromField.setOutputMarkupId(true);
    			untilField.setOutputMarkupId(true);
    			
    			Form<Void> form = new Form<Void>("date-input-dialog-inner-form");
    			form.setOutputMarkupId(true);
    			form.add(new Label("date-input-dialog-from-label", new Model<String>() {
    				public String getObject() {
    					return TCUtilities.getLocalizedString("tc.search.fromdate.text");
    				}
    			}));
    			form.add(fromField);
    			form.add(new Label("date-input-dialog-until-label", new Model<String>() {
    				public String getObject() {
    					return TCUtilities.getLocalizedString("tc.search.untildate.text");
    				}
    			}));
    			form.add(untilField);
    			form.add(new AjaxSubmitLink("date-input-dialog-ok-btn") {
						@Override
						public void onSubmit(AjaxRequestTarget target, Form<?> form)
	    				{
	    					close(target);
	    				}
	    			}
    				.add(new Label("date-input-dialog-ok-btn-text",
    						TCUtilities.getLocalizedString("tc.dialog.ok.text")))
    			);
    			
    			add(form);

    			TCUtilities.addInitUIOnDomReadyJavascript(form);
    		}
			
			public Date getFromDate() {
				return fromModel.getObject();
			}
			
			public Date getUntilDate() {
				return untilModel.getObject();
			}
			
			public void initDatePicker(AjaxRequestTarget target) {
    			target.appendJavascript(getInitDatePickerJavascript(fromField.getMarkupId(true)));
    			target.appendJavascript(getInitDatePickerJavascript(untilField.getMarkupId(true)));
			}
			
			private String getDatePickerFormatOfCurrentLocale() {
				String lang = getSession().getLocale().getLanguage();
				if (Locale.GERMAN.getLanguage().equalsIgnoreCase(lang)) {
					return "dd.mm.yy";
				}
				else {
					return "mm/dd/yy";
				}
			}
			
			private String getDatePickerRegionOfCurrentLocale() {
				String lang = getSession().getLocale().getLanguage();
				if (Locale.GERMAN.getLanguage().equalsIgnoreCase(lang)) {
					return "de";
				}
				else {
					return "";
				}
			}
			
			private String getInitDatePickerJavascript(String markupId) {
    			StringBuilder js = new StringBuilder();
    			js.append("$('#").append(markupId).append("').datepicker(");
    			js.append("$.datepicker.regional['");
    			js.append(getDatePickerRegionOfCurrentLocale()).append("']);");
    			js.append("$('#").append(markupId).append("').datepicker('option', {");
    			js.append("showOn: 'button',");
    			js.append("buttonImageOnly: false,");
    			js.append("showOtherMonths: true,");
    			js.append("selectOtherMonths: true,");
    			js.append("showButtonPanel: true,");
    			js.append("changeMonth: true,");
    			js.append("changeYear: true,");
    			js.append("yearRange: '-20:+0',");
    			js.append("dateFormat: '").append(getDatePickerFormatOfCurrentLocale()).append("',");
    			js.append("})");
    			js.append(".next('button').text('').button({icons:{primary : 'ui-icon-calendar'}, text: false}).addClass('ui-combobox-toggle');");
    			js.append("$('#").append(markupId).append("').select('.hasDatePicker').removeClass('ui-corner-all').addClass('ui-corner-left');");
    			js.append("$('#").append(markupId).append("').select('.hasDatePicker').siblings('button').removeClass('ui-corner-all').addClass('ui-corner-right');");
    			return js.toString();
			}
    	}
    }
}
