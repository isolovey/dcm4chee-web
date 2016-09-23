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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.dao.tc.ITextOrCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Category;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Level;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.PatientSex;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.YesNo;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.tc.TCInput.ValueChangeListener;
import org.dcm4chee.web.war.tc.TCObject.TextOrCode;
import org.dcm4chee.web.war.tc.TCUtilities.NullDropDownItem;
import org.dcm4chee.web.war.tc.TCUtilities.SelfUpdatingTextArea;
import org.dcm4chee.web.war.tc.TCUtilities.SelfUpdatingTextField;
import org.dcm4chee.web.war.tc.TCUtilities.TCToolTipAppender;
import org.dcm4chee.web.war.tc.TCViewPanel.AbstractEditableTCViewTab;
import org.dcm4chee.web.war.tc.keywords.TCKeywordCatalogueProvider;
import org.dcm4chee.web.war.tc.widgets.TCComboBox;
import org.dcm4chee.web.war.tc.widgets.TCSpinner;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 25, 2011
 */
public class TCViewOverviewTab extends AbstractEditableTCViewTab 
{
    private static final long serialVersionUID = 1L;

    private String tcId;
    private TCInput anatomyInput;
    private TCInput pathologyInput;
    private TCInput diagnosisInput;
    private TCInput diffDiagnosisInput;
    private TCInput findingInput;
        
    @SuppressWarnings("serial")
	public TCViewOverviewTab(final String id, IModel<TCEditableObject> model,
			TCAttributeVisibilityStrategy attrVisibilityStrategy) {
        super(id, model, attrVisibilityStrategy);
        
        tcId = getTC().getId();
        
        AttributeModifier readonlyModifier = new AttributeAppender("readonly",true,new Model<String>("readonly"), " ") {
        	@Override
        	public boolean isEnabled(Component component)
        	{
        		return !isEditing();
        	}
        };
        
        // TITLE
        final WebMarkupContainer titleRow = new WebMarkupContainer("tc-view-overview-title-row");
        titleRow.add(new Label("tc-view-overview-title-label", 
                new InternalStringResourceModel("tc.title.text")));
        titleRow.add(new SelfUpdatingTextField("tc-view-overview-title-text",
        		new Model<String>() {
		        	@Override
		        	public String getObject() {
		        		if (!getAttributeVisibilityStrategy()
		        				.isAttributeVisible(TCAttribute.Title)) {
		        			return TCUtilities.getLocalizedString("tc.case.text")+" " + getTC().getId();
		        		}
		        		return getStringValue(TCQueryFilterKey.Title);
		        	}
		            @Override
		            public void setObject(String text){
		                if (isEditing()){
		                    getTC().setTitle(text);
		                }
		            }
        		}
        ).add(readonlyModifier));
        
        // ABSTRACT
        final WebMarkupContainer abstractRow = new WebMarkupContainer("tc-view-overview-abstract-row");
        abstractRow.add(new Label("tc-view-overview-abstract-label", 
                new InternalStringResourceModel("tc.abstract.text")));
        abstractRow.add(new SelfUpdatingTextArea("tc-view-overview-abstract-area", 
        		new Model<String>() {
		        	@Override
		        	public String getObject() {
		        		if (!getAttributeVisibilityStrategy()
		        				.isAttributeVisible(TCAttribute.Abstract)) {
		        			return TCUtilities.getLocalizedString("tc.obfuscation.text");
		        		}
		        		return getStringValue(TCQueryFilterKey.Abstract);
		        	}
		        	@Override
		            public void setObject(String text) {
		                if (isEditing()){
		                    getTC().setAbstract(text);
		                }
		            }
        	}
        ).add(readonlyModifier).setOutputMarkupId(true).setMarkupId("tc-view-overview-abstract-area"));
        
        // URL
        final WebMarkupContainer urlRow = new WebMarkupContainer("tc-view-overview-url-row");
        urlRow.add(new WebMarkupContainer("tc-view-overview-url-label")
        	.add(
        		new ExternalLink("tc-view-url-link", getTC().getURL())
        		.add(new Label("tc-view-url-link-title",new InternalStringResourceModel("tc.url.text")))
        		.add(new Image("tc-view-url-link-follow-image", ImageManager.IMAGE_TC_EXTERNAL))
        		.add(new TCToolTipAppender("tc.case.url.text"))
        	)
	    );
        urlRow.add(new TextArea<String>("tc-view-overview-url-text",
        		new Model<String>() {
		        	@Override
		        	public String getObject() {
		        		TCObject tc = getTC();
		        		return tc!=null ? tc.getURL() : null;
		        	}
        		}
        ).add(new AttributeAppender("readonly",true,new Model<String>("readonly"), " ")));
        
        // AUTHOR NAME
        final WebMarkupContainer authorNameRow = new WebMarkupContainer("tc-view-overview-authorname-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.AuthorName);
        	}
        };
        authorNameRow.add(new Label("tc-view-overview-authorname-label", 
                new InternalStringResourceModel("tc.author.name.text")));
        authorNameRow.add(new SelfUpdatingTextField("tc-view-overview-authorname-text", 
        		new Model<String>() {
        			@Override
        			public String getObject() {
		        		if (!getAttributeVisibilityStrategy()
		        				.isAttributeVisible(TCAttribute.AuthorName)) {
		        			return TCUtilities.getLocalizedString("tc.obfuscation.text");
		        		}
		        		return getStringValue(TCQueryFilterKey.AuthorName);
        			}
		            @Override
		            public void setObject(String text) {
		                if (isEditing()) {
		                    getTC().setAuthorName(text);
		                }
		            }
        	}
        ).add(readonlyModifier));
        
        // AUTHOR AFFILIATION
        final WebMarkupContainer authorAffiliationRow = new WebMarkupContainer("tc-view-overview-authoraffiliation-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.AuthorAffiliation);
        	}
        };
        authorAffiliationRow.add(new Label("tc-view-overview-authoraffiliation-label", 
                new InternalStringResourceModel("tc.author.affiliation.text")));
        authorAffiliationRow.add(new SelfUpdatingTextField("tc-view-overview-authoraffiliation-text", 
        		new Model<String>() {
        			@Override
        			public String getObject() {
		        		if (!getAttributeVisibilityStrategy()
		        				.isAttributeVisible(TCAttribute.AuthorAffiliation)) {
		        			return TCUtilities.getLocalizedString("tc.obfuscation.text");
		        		}
		        		return getStringValue(TCQueryFilterKey.AuthorAffiliation);
        			}
		            @Override
		            public void setObject(String text) {
		                if (isEditing()) {
		                    getTC().setAuthorAffiliation(text);
		                }
		            }
        	}
        ).add(readonlyModifier));
        
        // AUTHOR CONTACT
        final WebMarkupContainer authorContactRow = new WebMarkupContainer("tc-view-overview-authorcontact-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.AuthorContact);
        	}
        };
        authorContactRow.add(new Label("tc-view-overview-authorcontact-label", 
                new InternalStringResourceModel("tc.author.contact.text")));
        authorContactRow.add(new SelfUpdatingTextArea("tc-view-overview-authorcontact-area", 
        		new Model<String>() {
		        	@Override
		        	public String getObject() {
		        		if (!getAttributeVisibilityStrategy()
		        				.isAttributeVisible(TCAttribute.AuthorContact)) {
		        			return TCUtilities.getLocalizedString("tc.obfuscation.text");
		        		}
		        		return getStringValue(TCQueryFilterKey.AuthorContact);
		        	}
		        	@Override
		            public void setObject(String text) {
		                if (isEditing()){
		                    getTC().setAuthorContact(text);
		                }
		            }
        	}
        ).add(readonlyModifier));
                
        // KEYWORDS
        final boolean keywordCodeInput = isEditing() && TCKeywordCatalogueProvider.
                getInstance().hasCatalogue(TCQueryFilterKey.Keyword);
        final KeywordsListModel keywordsModel = new KeywordsListModel();
        final WebMarkupContainer keywordCodesContainer = new WebMarkupContainer("tc-view-overview-keyword-input-container");
        final ListView<ITextOrCode> keywordCodesView = new ListView<ITextOrCode>(
                "tc-view-overview-keyword-input-view", keywordsModel) {            
            @Override
            protected void populateItem(final ListItem<ITextOrCode> item) {
                final int index = item.getIndex();

                AjaxLink<String> addBtn = new AjaxLink<String>("tc-view-overview-keyword-input-add") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        keywordsModel.addKeyword();
                        target.addComponent(keywordCodesContainer);
                    }
                };
                addBtn.add(new Image("tc-view-overview-keyword-input-add-img",
                        ImageManager.IMAGE_COMMON_ADD).add(
                                new ImageSizeBehaviour("vertical-align: middle;")));
                addBtn.add(new TooltipBehaviour("tc.view.overview.keyword.","add"));
                addBtn.setOutputMarkupId(true);
                addBtn.setVisible(index==0);
                
                AjaxLink<String> removeBtn = new AjaxLink<String>("tc-view-overview-keyword-input-remove") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        keywordsModel.removeKeyword(item.getModelObject());
                        target.addComponent(keywordCodesContainer);
                    }
                };
                removeBtn.add(new Image("tc-view-overview-keyword-input-remove-img",
                        ImageManager.IMAGE_TC_CANCEL).add(
                                new ImageSizeBehaviour("vertical-align: middle;")));
                removeBtn.add(new TooltipBehaviour("tc.view.overview.keyword.","remove"));
                removeBtn.setOutputMarkupId(true);
                removeBtn.setVisible(index>0);
                
                TCInput keywordInput = TCUtilities.createInput("tc-view-overview-keyword-input", 
                        TCQueryFilterKey.Keyword, item.getModelObject(), false);
                keywordInput.addChangeListener(
                        new ValueChangeListener() {
                            @Override
                            public void valueChanged(ITextOrCode[] values)
                            {
                                keywordsModel.setKeywordAt(index, values!=null&&values.length>0?values[0]:null);
                            }
                        }
                );
                
                item.setOutputMarkupId(true);
                item.add(keywordInput.getComponent());
                item.add(addBtn);
                item.add(removeBtn);
                
                if (index>0) {
                    item.add(new AttributeModifier("style",true,new Model<String>("border-top: 4px solid transparent")) {
                        @Override
                        protected String newValue(String currentValue, String newValue) {
                            if (currentValue==null) {
                                return newValue;
                            }
                            else if (newValue==null) {
                                return currentValue;
                            }
                            else {
                                return currentValue + ";" + newValue;
                            }
                        }
                    });
                }
            }
        };
        keywordCodesView.setOutputMarkupId(true);    
        keywordCodesContainer.setOutputMarkupId(true);
        keywordCodesContainer.setVisible(isEditing());
        keywordCodesContainer.add(keywordCodesView);
        final WebMarkupContainer keywordRow = new WebMarkupContainer("tc-view-overview-keyword-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.Keyword);
        	}
        };
        keywordRow.add(new Label("tc-view-overview-keyword-label", 
                new InternalStringResourceModel("tc.keyword.text")));
        keywordRow.add(keywordCodesContainer);
        keywordRow.add(new SelfUpdatingTextArea("tc-view-overview-keyword-area", 
        		new Model<String>() {
		        	@Override
		        	public String getObject() {
		        		if (!getAttributeVisibilityStrategy()
		        				.isAttributeVisible(TCAttribute.Keyword)) {
		        			return TCUtilities.getLocalizedString("tc.obfuscation.text");
		        		}
		        		return getShortStringValue(TCQueryFilterKey.Keyword);
		        	}
		        	@Override
		            public void setObject(String text) {
		                if (isEditing()) {
		                    String[] strings = text!=null?text.trim().split(";"):null;
		                    List<ITextOrCode> keywords = null;
		                    
		                    if (strings!=null && strings.length>0) {
		                        keywords = new ArrayList<ITextOrCode>(strings.length);
		                        for (String s : strings) {
		                            keywords.add(TextOrCode.text(s));
		                        }
		                    }
		                    
		                    getTC().setKeywords(keywords);
		                }
		            }
        	}
        ) {
            @Override
            public boolean isVisible() {
            	return !keywordCodeInput;
            }
            @Override
            protected void onComponentTag(ComponentTag tag)
            {
            	tag.put("title", getStringValue(TCQueryFilterKey.Keyword)); //$NON-NLS-1$
            }
        }.add(readonlyModifier).setOutputMarkupId(true).setMarkupId("tc-view-overview-keyword-area"));

        
        // ANATOMY
        anatomyInput = TCUtilities.createInput("tc-view-overview-anatomy-input", 
                TCQueryFilterKey.Anatomy, getTC().getValue(TCQueryFilterKey.Anatomy),true);
        anatomyInput.getComponent().setVisible(isEditing());
        anatomyInput.addChangeListener(
                new ValueChangeListener() {
                    @Override
                    public void valueChanged(ITextOrCode[] values)
                    {
                        getTC().setAnatomy(values!=null&&values.length>0?values[0]:null);
                    }
                }
        );
        final WebMarkupContainer anatomyRow = new WebMarkupContainer("tc-view-overview-anatomy-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.Anatomy);
        	}
        };
        anatomyRow.add(new Label("tc-view-overview-anatomy-label", 
                new InternalStringResourceModel("tc.anatomy.text")));
        anatomyRow.add(anatomyInput.getComponent());
        anatomyRow.add(new TextField<String>("tc-view-overview-anatomy-value-label", new Model<String>() {
        		public String getObject() {
        			return getShortStringValue(TCQueryFilterKey.Anatomy);
        		}
        	}   
        ) {
			private static final long serialVersionUID = 3465370488528419531L;
			@Override
            protected void onComponentTag(ComponentTag tag)
            {
				super.onComponentTag(tag);
            	tag.put("title", getStringValue(TCQueryFilterKey.Anatomy)); //$NON-NLS-1$
            }
			@Override
			public boolean isVisible() {
				return !isEditing();
			}
        }.add(readonlyModifier));
        
        // PATHOLOGY
        pathologyInput = TCUtilities.createInput("tc-view-overview-pathology-input", 
                TCQueryFilterKey.Pathology, getTC().getValue(TCQueryFilterKey.Pathology),true);
        pathologyInput.getComponent().setVisible(isEditing());
        pathologyInput.addChangeListener(
                new ValueChangeListener() {
                    @Override
                    public void valueChanged(ITextOrCode[] values)
                    {
                        getTC().setPathology(values!=null&&values.length>0?values[0]:null);
                    }
                }
        );
        final WebMarkupContainer pathologyRow = new WebMarkupContainer("tc-view-overview-pathology-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.Pathology);
        	}
        };
        pathologyRow.add(new Label("tc-view-overview-pathology-label", 
                new InternalStringResourceModel("tc.pathology.text")));
        pathologyRow.add(pathologyInput.getComponent());
        pathologyRow.add(new TextField<String>("tc-view-overview-pathology-value-label", 
        		new Model<String>() {
        			public String getObject() {
        				return getShortStringValue(TCQueryFilterKey.Pathology);
        			}
        	}  
        ) {
			private static final long serialVersionUID = 3465370488528419531L;
			@Override
            protected void onComponentTag(ComponentTag tag)
            {
				super.onComponentTag(tag);
            	tag.put("title", getStringValue(TCQueryFilterKey.Pathology)); //$NON-NLS-1$
            }
			@Override
			public boolean isVisible() {
				return !isEditing();
			}
        }.add(readonlyModifier));

        
        // CATEGORY
        final TCComboBox<TCQueryFilterValue.Category> categoryCBox = TCUtilities.createEnumComboBox(
                "tc-view-overview-category-select", new Model<Category>() {
                	@Override
                	public Category getObject() {
                		return getTC().getCategory();
                	}
                	@Override
                	public void setObject(Category value) {
                		getTC().setCategory(value);
                	}
                },
                Arrays.asList(TCQueryFilterValue.Category.values()), true,
                "tc.category", NullDropDownItem.Undefined, null);
        final WebMarkupContainer categoryRow = new WebMarkupContainer("tc-view-overview-category-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.Category);
        	}
        };
        categoryCBox.setVisible(isEditing());
        categoryRow.add(new Label("tc-view-overview-category-label", 
                new InternalStringResourceModel("tc.category.text")));
        categoryRow.add(categoryCBox);
        categoryRow.add(new TextField<String>("tc-view-overview-category-value-label", new Model<String>() {
        	@Override
        	public String getObject() {
        		return getStringValue(TCQueryFilterKey.Category);
        	}
        }).add(readonlyModifier).setVisible(!isEditing()));
        
        // LEVEL
        final TCComboBox<TCQueryFilterValue.Level> levelCBox = TCUtilities.createEnumComboBox(
                "tc-view-overview-level-select", new Model<Level>() {
                	@Override
                	public Level getObject() {
                		return getTC().getLevel();
                	}
                	@Override
                	public void setObject(Level level) {
                		getTC().setLevel(level);
                	}
                },
                Arrays.asList(TCQueryFilterValue.Level.values()), true,
                "tc.level", NullDropDownItem.Undefined, null);
        final WebMarkupContainer levelRow = new WebMarkupContainer("tc-view-overview-level-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.Level);
        	}
        };
        levelCBox.setVisible(isEditing());
        levelRow.add(new Label("tc-view-overview-level-label", 
                new InternalStringResourceModel("tc.level.text")));
        levelRow.add(new TextField<String>("tc-view-overview-level-value-label", new Model<String>() {
        		@Override
        		public String getObject() {
        			return getStringValue(TCQueryFilterKey.Level);
        		}
        }).add(readonlyModifier).setVisible(!isEditing()));
        levelRow.add(levelCBox);
        
        // PATIENT SEX
        final TCComboBox<TCQueryFilterValue.PatientSex> patientSexCBox = TCUtilities.createEnumComboBox(
                "tc-view-overview-patientsex-select", new Model<PatientSex>() {
                	@Override
                	public PatientSex getObject() {
                		return getTC().getPatientSex();
                	}
                	@Override
                	public void setObject(PatientSex value) {
                		getTC().setPatientSex(value);
                	}
                },
                Arrays.asList(TCQueryFilterValue.PatientSex.values()), true,
                "tc.patientsex", NullDropDownItem.Undefined, null);
        final WebMarkupContainer patientSexRow = new WebMarkupContainer("tc-view-overview-patientsex-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.PatientSex);
        	}
        };
        patientSexCBox.setVisible(isEditing());
        patientSexRow.add(new Label("tc-view-overview-patientsex-label", 
                new InternalStringResourceModel("tc.patient.sex.text")));
        patientSexRow.add(new TextField<String>("tc-view-overview-patientsex-value-label", new Model<String>() {
        	public String getObject() {
        		return getStringValue(TCQueryFilterKey.PatientSex);
        	}
        }).add(readonlyModifier).setVisible(!isEditing()));
        patientSexRow.add(patientSexCBox);

        // PATIENT AGE
        final TCSpinner<Integer> patientAgeYearSpinner = TCSpinner.createYearSpinner("tc-view-overview-patientage-years-input", 
        		new Model<Integer>() {
        			@Override
        			public Integer getObject() {
        				return TCPatientAgeUtilities.toYears(getTC().getPatientAge());
        			}
        			@Override
        			public void setObject(Integer years) {
        				getTC().setPatientAge(TCPatientAgeUtilities.toDays(years,
        						TCPatientAgeUtilities.toRemainingMonths(getTC().getPatientAge())));
        			}
        		}, null
        );
        final TCSpinner<Integer> patientAgeMonthSpinner = TCSpinner.createMonthSpinner("tc-view-overview-patientage-months-input", 
        		new Model<Integer>() {
					@Override
					public Integer getObject() {
						return TCPatientAgeUtilities.toRemainingMonths(getTC().getPatientAge());
					}
					@Override
					public void setObject(Integer months) {
						getTC().setPatientAge(TCPatientAgeUtilities.toDays(
								TCPatientAgeUtilities.toYears(getTC().getPatientAge()),months));
					}
		}, null
        );
        final WebMarkupContainer patientAgeRow = new WebMarkupContainer("tc-view-overview-patientage-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.PatientAge);
        	}
        };
        patientAgeYearSpinner.setVisible(isEditing());
        patientAgeMonthSpinner.setVisible(isEditing());
        patientAgeRow.add(new Label("tc-view-overview-patientage-label", 
                new InternalStringResourceModel("tc.patient.age.text")));
        patientAgeRow.add(new TextField<String>("tc-view-overview-patientage-value-label", new Model<String>() {
        	public String getObject() {
        		return TCPatientAgeUtilities.format(getTC().getPatientAge());
        	}
        }).add(readonlyModifier).setVisible(!isEditing()));
        patientAgeRow.add(patientAgeYearSpinner);
        patientAgeRow.add(patientAgeMonthSpinner);
        
        // PATIENT SPECIES
        List<String> ethnicGroups = WebCfgDelegate.getInstance().getTCEthnicGroups();
        boolean ethnicGroupsAvailable = ethnicGroups!=null && !ethnicGroups.isEmpty();
        SelfUpdatingTextField patientSpeciesField = new SelfUpdatingTextField("tc-view-overview-patientrace-value-label", 
        		new Model<String>() {
		        	@Override
		        	public String getObject() {
		        		return getStringValue(TCQueryFilterKey.PatientSpecies);
		        	}
		            @Override
		            public void setObject(String text) {
		            	if (isEditing()) {
		            		getTC().setPatientSpecies(text);
		            	}
		            }
				}     
        );
        final WebMarkupContainer patientSpeciesRow = new WebMarkupContainer("tc-view-overview-patientrace-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.PatientSpecies);
        	}
        };
        patientSpeciesRow.add(new Label("tc-view-overview-patientrace-label", 
                new InternalStringResourceModel("tc.patient.species.text")));
        patientSpeciesRow.add(patientSpeciesField);        
        patientSpeciesRow.add(
        	TCUtilities.createEditableComboBox( "tc-view-overview-patientrace-select", 
        				new Model<String>() {
		                	@Override
		                	public String getObject() {
		                		return getTC().getValueAsLocalizedString(
		                				TCQueryFilterKey.PatientSpecies, TCViewOverviewTab.this);
		                	}
		                	@Override
		                	public void setObject(String value) {
		                		getTC().setValue(TCQueryFilterKey.PatientSpecies, value);
		                	}
		                },
		                ethnicGroups, NullDropDownItem.Undefined, null)
		    .add(readonlyModifier).setVisible(isEditing() && ethnicGroupsAvailable)
		);
        
        patientSpeciesField.setVisible( !isEditing() || !ethnicGroupsAvailable );
        if (!isEditing()) {
        	patientSpeciesField.add( readonlyModifier );
        }

        // MODALITIES
        final WebMarkupContainer modalitiesRow = new WebMarkupContainer("tc-view-overview-modalities-row") {
        	@Override
        	public boolean isVisible() {
        		return getAttributeVisibilityStrategy()
        				.isAttributeVisible(TCAttribute.AcquisitionModality);
        	}
        };
        modalitiesRow.add(new Label("tc-view-overview-modalities-label", 
                new InternalStringResourceModel("tc.modalities.text")));
        modalitiesRow.add(new SelfUpdatingTextField("tc-view-overview-modalities-text", 
        		new Model<String>() {
        			@Override 
        			public String getObject() { 
        				return getStringValue(
        						TCQueryFilterKey.AcquisitionModality);
        			}
        			@Override
        			public void setObject(String value) {
                        if (isEditing())
                        {
                            String[] modalities = value!=null?value.trim().split(";"):null;
                            getTC().setValue(TCQueryFilterKey.AcquisitionModality, modalities!=null?
                                    Arrays.asList(modalities):null);
                        }
        			}
        }).add(readonlyModifier));
        
        // FINDING
        findingInput = TCUtilities.createInput("tc-view-overview-finding-input", 
                TCQueryFilterKey.Finding, getTC().getValue(TCQueryFilterKey.Finding),true);
        findingInput.getComponent().setVisible(isEditing());
        findingInput.addChangeListener(
                new ValueChangeListener() {
                    @Override
                    public void valueChanged(ITextOrCode[] values)
                    {
                        getTC().setFinding(values!=null&&values.length>0?values[0]:null);
                    }
                }
        );
        final WebMarkupContainer findingRow = new WebMarkupContainer("tc-view-overview-finding-row") {
        	@Override
        	public boolean isVisible() {
        		return TCKeywordCatalogueProvider.getInstance().hasCatalogue(TCQueryFilterKey.Finding) &&
        				getAttributeVisibilityStrategy()
        					.isAttributeVisible(TCAttribute.Finding);
        	}
        };
        findingRow.add(new Label("tc-view-overview-finding-label", 
                new InternalStringResourceModel("tc.finding.text")));
        findingRow.add(findingInput.getComponent());
        findingRow.add(new TextField<String>("tc-view-overview-finding-value-label", new Model<String>() {
        	@Override
        	public String getObject() {
        		return getShortStringValue(TCQueryFilterKey.Finding);
        	}
        }) {
			@Override
            protected void onComponentTag(ComponentTag tag)
            {
				super.onComponentTag(tag);
            	tag.put("title", getStringValue(TCQueryFilterKey.Finding)); //$NON-NLS-1$
            }
			@Override
			public boolean isVisible() {
				return !isEditing();
			}
        });
                
        // DIAGNOSIS
        diagnosisInput = TCUtilities.createInput("tc-view-overview-diag-input", 
                TCQueryFilterKey.Diagnosis, getTC().getValue(TCQueryFilterKey.Diagnosis),true);
        diagnosisInput.getComponent().setVisible(isEditing());
        diagnosisInput.addChangeListener(
                new ValueChangeListener() {
                    @Override
                    public void valueChanged(ITextOrCode[] values)
                    {
                        getTC().setDiagnosis(values!=null&&values.length>0?values[0]:null);
                    }
                }
        );
        final WebMarkupContainer diagRow = new WebMarkupContainer("tc-view-overview-diag-row") {
        	@Override
        	public boolean isVisible() {
        		return TCKeywordCatalogueProvider.getInstance().hasCatalogue(TCQueryFilterKey.Diagnosis) &&
        				getAttributeVisibilityStrategy()
        					.isAttributeVisible(TCAttribute.Diagnosis);
        	}
        };
        diagRow.add(new Label("tc-view-overview-diag-label", 
                new InternalStringResourceModel("tc.diagnosis.text")));
        diagRow.add(diagnosisInput.getComponent());
        diagRow.add(new TextField<String>("tc-view-overview-diag-value-label", new Model<String>() {
        	@Override
        	public String getObject() {
        		return getShortStringValue(TCQueryFilterKey.Diagnosis);
        	}
        }) {
			@Override
            protected void onComponentTag(ComponentTag tag)
            {
				super.onComponentTag(tag);
            	tag.put("title", getStringValue(TCQueryFilterKey.Diagnosis)); //$NON-NLS-1$
            }
			@Override
			public boolean isVisible() {
				return !isEditing();
			}
        });
        
        // DIAGNOSIS CONFIRMED
        final WebMarkupContainer diagConfirmedRow = new WebMarkupContainer("tc-view-overview-diagconfirmed-row") {
        	@Override
        	public boolean isVisible() {
        		return TCKeywordCatalogueProvider.getInstance().hasCatalogue(TCQueryFilterKey.Diagnosis) &&
        				getAttributeVisibilityStrategy()
        					.isAttributeVisible(TCAttribute.Diagnosis);
        	}
        };
        diagConfirmedRow.add(new Label("tc-view-overview-diagconfirmed-label", 
                new InternalStringResourceModel("tc.diagnosis.confirmed.text")));
        diagConfirmedRow.add(new CheckBox("tc-view-overview-diagconfirmed-input", new Model<Boolean>() {
        	@Override
        	public Boolean getObject() {
        		YesNo yesno = getTC().getDiagnosisConfirmed();
        		if (yesno!=null && YesNo.Yes.equals(yesno)) {
        			return true;
        		}
        		else {
        			return false;
        		}
        	}
        	@Override
        	public void setObject(Boolean value) {
        		if (value!=null && value==true) {
        			getTC().setDiagnosisConfirmed(YesNo.Yes);
        		}
        		else {
        			getTC().setDiagnosisConfirmed(null);
        		}
        	}
        }).setEnabled(isEditing()));
        
        // DIFFERENTIAL DIAGNOSIS
        diffDiagnosisInput = TCUtilities.createInput("tc-view-overview-diffdiag-input", 
                TCQueryFilterKey.DifferentialDiagnosis, getTC().getValue(TCQueryFilterKey.DifferentialDiagnosis),true);
        diffDiagnosisInput.getComponent().setVisible(isEditing());
        diffDiagnosisInput.addChangeListener(
                new ValueChangeListener() {
                    @Override
                    public void valueChanged(ITextOrCode[] values)
                    {
                        getTC().setDiffDiagnosis(values!=null&&values.length>0?values[0]:null);
                    }
                }
        );
        final WebMarkupContainer diffDiagRow = new WebMarkupContainer("tc-view-overview-diffdiag-row") {
        	@Override
        	public boolean isVisible() {
        		return TCKeywordCatalogueProvider.getInstance().hasCatalogue(TCQueryFilterKey.DifferentialDiagnosis) &&
        				getAttributeVisibilityStrategy()
        					.isAttributeVisible(TCAttribute.DifferentialDiagnosis);
        	}
        };
        diffDiagRow.add(new Label("tc-view-overview-diffdiag-label", 
                new InternalStringResourceModel("tc.diffdiagnosis.text")));
        diffDiagRow.add(diffDiagnosisInput.getComponent());
        diffDiagRow.add(new TextField<String>("tc-view-overview-diffdiag-value-label", new Model<String>() {
        	@Override
        	public String getObject() {
        		return getShortStringValue(TCQueryFilterKey.DifferentialDiagnosis);
        	}
        }) {
			@Override
            protected void onComponentTag(ComponentTag tag)
            {
				super.onComponentTag(tag);
            	tag.put("title", getStringValue(TCQueryFilterKey.DifferentialDiagnosis)); //$NON-NLS-1$
            }
			@Override
			public boolean isVisible() {
				return !isEditing();
			}
        });

        
        // IMAGE COUNT
        final WebMarkupContainer imageCountRow = new WebMarkupContainer("tc-view-overview-imagecount-row");
        imageCountRow.add(new Label("tc-view-overview-imagecount-label", 
                new InternalStringResourceModel("tc.view.images.count.text")));
        imageCountRow.add(new TextField<String>("tc-view-overview-imagecount-value-label", new Model<String>() {
        	public String getObject() {
        		return getTC().getReferencedImages()!=null ? 
        				Integer.toString(getTC().getReferencedImages().size()) : "0";
        	}
        }).add(new AttributeAppender("readonly",true,new Model<String>("readonly"), " ")));

        add(titleRow);
        add(abstractRow);
        add(urlRow);
        add(authorNameRow);
        add(authorAffiliationRow);
        add(authorContactRow);
        add(keywordRow);
        add(anatomyRow);
        add(pathologyRow);
        add(findingRow);
        add(diffDiagRow);
        add(diagRow);
        add(diagConfirmedRow);
        add(categoryRow);
        add(levelRow);
        add(patientSexRow);
        add(patientAgeRow);
        add(patientSpeciesRow);
        add(modalitiesRow);
        add(imageCountRow);
    }
    
    @Override
    public String getTabTitle()
    {
        return getString("tc.view.overview.tab.title");
    }
    
    @Override
    public boolean hasContent()
    {
        return getTC()!=null;
    }
    
    @Override
    protected void saveImpl()
    {
    }
    
    @Override
	protected void onBeforeRender()
	{
    	String curId = getTC().getId();
    	if (tcId!=curId) {
    		// the TC object has changed -> change keyword input values appropriately
    		anatomyInput.setValues(getTC().getAnatomy());
    		pathologyInput.setValues(getTC().getPathology());
    		findingInput.setValues(getTC().getFinding());
    		diagnosisInput.setValues(getTC().getDiagnosis());
    		diffDiagnosisInput.setValues(getTC().getDiffDiagnosis());
    	}
    	super.onBeforeRender();
	}
    
    @SuppressWarnings("serial")
	private class KeywordsListModel extends ListModel<ITextOrCode>
    {
        public KeywordsListModel() {
            if (getSize()==0) {
                getTC().addKeyword(createTemplateKeyword());
            }
        }
        @Override
        public List<ITextOrCode> getObject()
        {
            return getTC().getKeywords();
        }
        
        @Override
        public void setObject(List<ITextOrCode> keywords) {
            getTC().setKeywords(keywords);
        }
        
        public int getSize() {
            return getTC().getKeywordCount();
        }
        
        public void addKeyword() {
            getTC().addKeyword(createTemplateKeyword());
        }
        
        public void removeKeyword(ITextOrCode keyword) {
            getTC().removeKeyword(keyword);
        }
        
        public void setKeywordAt(int index, ITextOrCode keyword) {
            getTC().setKeywordAt(index, keyword);
        }
        
        private ITextOrCode createTemplateKeyword() {
            return TextOrCode.text(null);
        }
    }
    
    @SuppressWarnings("serial")
	private class InternalStringResourceModel extends AbstractReadOnlyModel<String>
    {
        private String key;
        private String value;
        
        public InternalStringResourceModel(String key)
        {
            this.key = key;
        }
        
        @Override
        public String getObject()
        {
            if (value==null)
            {
                value = getString(key);
                if (value!=null && !value.endsWith(":"))
                {
                    value = value + ":";
                }
            }
            return value;
        }
    }

}
