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
package org.dcm4chee.web.war.tc.keywords.acr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree.LinkType;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.war.common.AutoSelectInputTextBehaviour;
import org.dcm4chee.web.war.tc.TCPopupManager.AbstractTCPopup;
import org.dcm4chee.web.war.tc.TCPopupManager.TCPopupPosition;
import org.dcm4chee.web.war.tc.TCPopupManager.TCPopupPosition.PopupAlign;
import org.dcm4chee.web.war.tc.TCUtilities;
import org.dcm4chee.web.war.tc.keywords.AbstractTCKeywordInput;
import org.dcm4chee.web.war.tc.keywords.TCKeyword;
import org.dcm4chee.web.war.tc.keywords.TCKeywordNode;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since June 20, 2011
 */
public class TCKeywordACRInput extends AbstractTCKeywordInput {

    private static final long serialVersionUID = 1L;

    private ACRPopup popup;
    private WebMarkupContainer chooserBtn;
    private TextField<String> text;
    private boolean triggerInstalled;
    
    public TCKeywordACRInput(final String id, TCQueryFilterKey filterKey, 
    		boolean usedForSearch, boolean exclusive,
    		TCKeyword selectedKeyword) 
    {
    	this(id, filterKey, usedForSearch, exclusive, selectedKeyword!=null ?
    			Collections.singletonList(selectedKeyword) : null);
    }
    
    @SuppressWarnings("serial")
	public TCKeywordACRInput(final String id, TCQueryFilterKey filterKey, 
    		boolean usedForSearch, final boolean exclusive,
    		List<TCKeyword> selectedKeywords) 
    {
        super(id, filterKey, usedForSearch, exclusive);

        setDefaultModel(new ListModel<TCKeyword>(selectedKeywords) {
			private static final long serialVersionUID = 1L;
			@Override
            public void setObject(List<TCKeyword> keywords)
            {
                if (!TCUtilities.equals(getObject(),keywords))
                {
                    super.setObject(keywords);
                    
                    fireValueChanged();
                }
            }
        });
        
        final ACRChooser chooser = new ACRChooser("keyword-acr");
        final MultipleKeywordsTextModel textModel = new MultipleKeywordsTextModel(selectedKeywords);
        text = new TextField<String>("text", textModel) {
        	@Override
        	protected void onComponentTag(ComponentTag tag)
        	{
        		super.onComponentTag(tag);       		
        		if (exclusive) {
        			tag.put("readonly","readonly");
        		}
            	else
            	{
               		tag.put("onmouseover", "$(this).addClass('ui-input-hover')");
                	tag.put("onmouseout", "$(this).removeClass('ui-input-hover')");
               		tag.put("onfocus", "$(this).addClass('ui-input-focus')");
                	tag.put("onblur", "$(this).removeClass('ui-input-focus')");
            	}
        	}
        };
        text.setOutputMarkupId(true);
        text.add(new AttributeAppender("class",true,new Model<String>(
        		exclusive?"ui-input-readonly":"ui-input")," "));
        text.add(new AutoSelectInputTextBehaviour());
        text.add(new AjaxFormComponentUpdatingBehavior("onchange") {
			private static final long serialVersionUID = 1L;
			@Override
            public void onUpdate(AjaxRequestTarget target)
            {
                text.updateModel();
                getModel().setObject(textModel.getKeywordItems());
            }
        });
        
        chooserBtn = new WebMarkupContainer("chooser-button", new Model<String>("...")) {
        	@Override
        	protected void onComponentTag(ComponentTag tag)
        	{
        		super.onComponentTag(tag);
        		tag.put("onmouseover", "$(this).addClass('ui-state-hover')");
        		tag.put("onmouseout", "$(this).removeClass('ui-state-hover')");
        	}
        };

        popup = new ACRPopup(chooser);
        
        add(text);
        add(chooserBtn);
        add(popup);
    }
    
    @Override
    protected void onBeforeRender()
    {
    	super.onBeforeRender();
    	
    	if (!triggerInstalled)
    	{
	        popup.installPopupTrigger(chooserBtn, new TCPopupPosition(
	                chooserBtn.getMarkupId(),
	                popup.getMarkupId(), 
	                PopupAlign.BottomLeft, PopupAlign.TopLeft));
	        triggerInstalled = true;
    	}
    }

    @Override
    public TCKeyword[] getKeywords() {
        List<TCKeyword> keywords = getKeywordsAsList();
        if (keywords!=null && !keywords.isEmpty())
        {
        	List<TCKeyword> list = new ArrayList<TCKeyword>(keywords);
        	for (Iterator<TCKeyword> it=list.iterator();it.hasNext();)
        	{
        		TCKeyword keyword = it.next();
        		if (keyword==null || keyword.isAllKeywordsPlaceholder())
        		{
        			it.remove();
        		}
        	}
        	if (!list.isEmpty())
        	{
        		return list.toArray(new TCKeyword[0]);
        	}
        }
        return null;
    }
    
	@Override
	public void setKeywords(TCKeyword...keywords)
	{
		if (keywords==null || keywords.length==0)
		{
			setKeywordsAsList(null);
		}
		else {
	    	List<TCKeyword> list = new ArrayList<TCKeyword>(3);
	    	if (keywords!=null) {
	    		for (TCKeyword keyword : keywords) {
	    			if (keyword!=null) {
	    				list.add(keyword);
	    			}
	    		}
	    	}
	    	setKeywordsAsList(list);
		}
	}
    
    @Override
    public boolean isExclusive()
    {
        return text.isEnabled();
    }
    
    public List<TCKeyword> getKeywordsAsList() {
    	List<TCKeyword> list = new ArrayList<TCKeyword>();
    	List<TCKeyword> modelList = getModel().getObject();
    	if (modelList!=null)
    	{
    		list.addAll(modelList);
    	}
    	return list;
    }
    
    private void setKeywordsAsList(List<TCKeyword> keywords)
    {
    	getModel().setObject(keywords);
    	((MultipleKeywordsTextModel)text.getModel()).setKeywordItems(keywords);
    	if (keywords==null || keywords.isEmpty())
    	{
    		popup.resetSelection();
    	}
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ListModel<TCKeyword> getModel() {
        return (ListModel) getDefaultModel();
    }
    
    private class MultipleKeywordsTextModel extends MultipleItemsTextModel
    {
		private static final long serialVersionUID = 4098272902061698377L;
		
		public MultipleKeywordsTextModel(List<TCKeyword> selectedKeywords)
    	{
    		setKeywordItems(selectedKeywords);
    	}
    	
    	public void setKeywordItems(List<TCKeyword> keywords)
    	{
    		setObject(toString(keywords));
    	}
    	
    	public List<TCKeyword> getKeywordItems()
    	{
    		List<String> items = getStringItems();
    		if (items!=null && !items.isEmpty())
    		{
    			List<TCKeyword> keywords = new ArrayList<TCKeyword>();
    			for (String item : items)
    			{
                    TCKeyword keyword = new TCKeyword(item, null, false);
                    if (keyword!=null)
                    {
                    	keywords.add(keyword);
                    }
    			}
    			return keywords;
    		}
    		return null;
    	}
    }
    
    public class ACRChooser extends Fragment {
		private static final long serialVersionUID = -4128242521402923293L;

		private TCKeyword anatomyKeyword;

        private TCKeyword pathologyKeyword;

        private String curPathologyTreeId;
        
        private Map<ACRKeywordNode, Tree> pathologyTrees;

        public ACRChooser(String id) {
            super(id, "acr-chooser", TCKeywordACRInput.this);

            setOutputMarkupId(true);

            ACRKeywordNode[] pathologyRoots = ACRCatalogue.getInstance()
                    .getPathologyRoots();
            pathologyTrees = new HashMap<ACRKeywordNode, Tree>(
                    pathologyRoots.length);
            for (int i = 0; i < pathologyRoots.length; i++) {
                final ACRKeywordNode pathologyRoot = pathologyRoots[i];
                final Tree pathologyTree = new Tree("pathology-tree-" + i,
                        new DefaultTreeModel(pathologyRoot)) {
                    @Override
                    protected void populateTreeItem(WebMarkupContainer item, int level) {
                        super.populateTreeItem(item, level);
                        
                        //(WEB-429) workaround: disable browser-native drag and drop
                        item.add(new AttributeModifier("onmousedown", true, new AbstractReadOnlyModel<String>() {
                            @Override
                            public String getObject()
                            {
                                return "return false;";
                            }
                        }));
                    }
                    @Override
                    public void onNodeLinkClicked(AjaxRequestTarget target,
                            TreeNode node) {
                        boolean shouldSelect = node != null
                                && node instanceof ACRKeywordNode
                                && getTreeState().isNodeSelected(node);

                        if (shouldSelect) {
                            TCKeyword keyword = ((ACRKeywordNode) node)
                                    .getKeyword();

                            if (keyword != null
                                    && keyword.isAllKeywordsPlaceholder()) {
                                keyword = null;
                            }

                            pathologyKeyword = keyword;
                        } else {
                            pathologyKeyword = null;
                        }
                    }
                };
                pathologyTree.setOutputMarkupId(true);
                pathologyTree.setOutputMarkupPlaceholderTag(true);
                pathologyTree.setRootLess(true);
                pathologyTree.setLinkType(LinkType.AJAX);
                pathologyTree.getTreeState().setAllowSelectMultiple(false);
                pathologyTree.setVisible(false);

                pathologyTrees.put(pathologyRoots[i], pathologyTree);

                ACRKeywordNode node = pathologyRoots[i]
                        .findNode(pathologyKeyword);

                Tree tree = getCurrentPathologyTree();

                if (tree == null || node != null) {
                    setPathologyTreeVisible(pathologyTree);
                }

                add(pathologyTree);
            }

            final Tree anatomyTree = new Tree("anatomy-tree",
                    new DefaultTreeModel(ACRCatalogue.getInstance()
                            .getAnatomyRoot())) {
                @Override
                protected void populateTreeItem(WebMarkupContainer item, int level) {
                    super.populateTreeItem(item, level);
                    
                    //(WEB-429) workaround: disable browser-native drag and drop
                    item.add(new AttributeModifier("onmousedown", true, new AbstractReadOnlyModel<String>() {
                        @Override
                        public String getObject()
                        {
                            return "return false;";
                        }
                    }));
                }
                @Override
                public void onNodeLinkClicked(AjaxRequestTarget target,
                        TreeNode node) {
                    boolean shouldSelect = node != null
                            && node instanceof ACRKeywordNode
                            && getTreeState().isNodeSelected(node);

                    if (shouldSelect) {
                        TCKeyword keyword = ((ACRKeywordNode) node)
                                .getKeyword();

                        if (keyword != null
                                && keyword.isAllKeywordsPlaceholder()) {
                            keyword = null;
                        }

                        anatomyKeyword = keyword;
                        pathologyKeyword = null;

                        ACRKeywordNode pathologyRoot = ACRCatalogue
                                .getInstance().getPathologyRoot(
                                        (ACRKeywordNode) node);
                        if (pathologyRoot != null) {
                            Tree pathologyTree = pathologyTrees
                                    .get(pathologyRoot);
                            Tree curPathologyTree = getCurrentPathologyTree();
                            if (pathologyTree!=null && 
                                    pathologyTree != curPathologyTree) {
                                setPathologyTreeVisible(pathologyTree);
                                setNodeSelected(pathologyTree, null);

                                target.addComponent(curPathologyTree);
                                target.addComponent(pathologyTree);
                            }
                        }
                    } else {
                        anatomyKeyword = null;
                    }
                }
            };
            anatomyTree.setOutputMarkupId(true);
            anatomyTree.setLinkType(LinkType.AJAX);
            anatomyTree.setRootLess(true);
            anatomyTree.getTreeState().setAllowSelectMultiple(false);

            add(anatomyTree);
        }

        public List<? extends TCKeyword> getKeywords() {

            if (anatomyKeyword != null && pathologyKeyword != null
                    && anatomyKeyword.getCode() != null
                    && pathologyKeyword.getCode() != null) {
                return Collections.singletonList(new ACRKeyword(anatomyKeyword, pathologyKeyword));
            } else if (pathologyKeyword != null) {
                return Collections.singletonList(pathologyKeyword);
            } else if (anatomyKeyword != null) {
                return Collections.singletonList(anatomyKeyword);
            }

            return null;
        }

        public Component[] setKeyword(TCKeyword keyword) {
            anatomyKeyword = null;
            pathologyKeyword = null;

            if (keyword != null) {
                if (keyword.getCode() == null) {
                    anatomyKeyword = keyword;
                } else if (ACRCatalogue.getInstance().isCompositeKeyword(
                        keyword)) {
                    if (keyword instanceof ACRKeyword) {
                        anatomyKeyword = ((ACRKeyword) keyword)
                                .getAnatomyKeyword();
                        pathologyKeyword = ((ACRKeyword) keyword)
                                .getPathologyKeyword();
                    }
                } else {
                    if (ACRCatalogue.getInstance().isAnatomyKeyword(keyword)) {
                        anatomyKeyword = keyword;
                    } else if (ACRCatalogue.getInstance().isPathologyKeyword(
                            keyword)) {
                        pathologyKeyword = keyword;
                    }
                }
            }

            Tree curPathologyTree = getCurrentPathologyTree();
            Tree anatomyTree = (Tree) get("anatomy-tree");
            
            ACRKeywordNode anatomyRoot = (ACRKeywordNode) anatomyTree.getModelObject().getRoot();
            ACRKeywordNode pathologyRoot = anatomyKeyword!=null ?
            		ACRCatalogue.getInstance().getPathologyRoot(anatomyRoot.findNode(anatomyKeyword)) :
            			curPathologyTree!=null ? (ACRKeywordNode) curPathologyTree.getModelObject().getRoot() :
            				ACRCatalogue.getInstance().getPathologyRoot( 
            						(ACRKeywordNode) ACRCatalogue.getInstance().getAnatomyRoot().getChildAt(0) );

            setNodeSelected( anatomyTree, null );
            if (anatomyKeyword != null) {
                setNodeSelected(anatomyTree, anatomyRoot.findNode(anatomyKeyword));
            }

            for (Tree tree : pathologyTrees.values()) {
            	if (tree.getModelObject().getRoot().equals( pathologyRoot ) ) {
            		if ( pathologyKeyword!=null ) {
            			setNodeSelected( tree, pathologyRoot.findNode( pathologyKeyword ) );
            		}
            		else {
            			setNodeSelected( tree, null );
            		}
            		setPathologyTreeVisible( tree );
            	}
            	else {
            		setNodeSelected( tree, null );
            	}
            }

            return new Component[] { get("anatomy-tree"),
                    get(curPathologyTreeId) };
        }

        private void setPathologyTreeVisible(Tree tree) {
            Tree curTree = getCurrentPathologyTree();

            if (curTree != null && curTree != tree) {
                curTree.setVisible(false);
            }

            curPathologyTreeId = tree.getId();

            tree.setVisible(true);
        }

        private Tree getCurrentPathologyTree() {
            return curPathologyTreeId != null ? (Tree) get(curPathologyTreeId)
                    : null;
        }

        private void ensurePathExpanded(Tree tree, ACRKeywordNode node) {
            if (node != null) {
                List<TCKeywordNode> path = node.getPath();
                if (path != null) {
                    for (TCKeywordNode n : path) {
                        tree.getTreeState().expandNode(n);
                    }
                }
            }
        }

        private void setNodeSelected(Tree tree, ACRKeywordNode node) {
            if (node != null) {
                tree.getTreeState().selectNode(node, true);

                ensurePathExpanded(tree, node);
            } else {
                Collection<Object> selectedNodes = tree.getTreeState() != null ? tree
                        .getTreeState().getSelectedNodes() : Collections
                        .emptyList();
                if (selectedNodes != null && !selectedNodes.isEmpty()) {
                    for (Object n : selectedNodes) {
                        tree.getTreeState().selectNode(n, false);
                    }
                }
            }
        }
    }

    private class ACRPopup extends AbstractTCPopup
    {
		private static final long serialVersionUID = -8132148064066349246L;
		private ACRChooser chooser;

        public ACRPopup(ACRChooser chooser) {
            super("acr-keyword-popup", true, false, true);

            this.chooser = chooser;
            
            add(chooser);
        }
        
        public void resetSelection()
        {
        	chooser.setKeyword(null);
        }
        
        @Override
        public void afterShowing(AjaxRequestTarget target)
        {
        	if (isMultipleKeywordSearchEnabled())
        	{
        		chooser.setKeyword(null);
        	}
        	else
        	{
        		chooser.setKeyword(getKeyword());
        	}
        	
            target.addComponent(chooser);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
        public void beforeHiding(AjaxRequestTarget target) 
        {
        	List<? extends TCKeyword> selectedKeywords = chooser.getKeywords();

        	if (isMultipleKeywordSearchEnabled())
        	{
        		if (selectedKeywords!=null)
        		{
                	List<TCKeyword> keywords = new ArrayList<TCKeyword>();
        			keywords.addAll(getKeywordsAsList());
        			for (TCKeyword keyword : selectedKeywords) {
        				if (!keywords.contains(keyword)) {
        					keywords.add(keyword);
        				}
        			}
        			setKeywordsAsList(keywords);
        		}
        	}
        	else
        	{
        		setKeywordsAsList((List)selectedKeywords);
        	}

            target.addComponent(text);
        }
    }

}
