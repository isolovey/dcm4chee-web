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
package org.dcm4chee.web.war.tc.keywords;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree.LinkType;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.war.common.AutoSelectInputTextBehaviour;
import org.dcm4chee.web.war.tc.TCPopupManager.AbstractTCPopup;
import org.dcm4chee.web.war.tc.TCPopupManager.TCPopupPosition;
import org.dcm4chee.web.war.tc.TCPopupManager.TCPopupPosition.PopupAlign;
import org.dcm4chee.web.war.tc.TCUtilities;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since June 20, 2011
 */
public class TCKeywordTreeInput extends AbstractTCKeywordInput {

    private static final long serialVersionUID = 1L;

    private TCKeywordNode root;
    private AutoCompleteTextField<String> text;
    private KeywordTreePopup popup;
    private WebMarkupContainer trigger;
    private boolean triggerInstalled;
    
    public TCKeywordTreeInput(final String id, TCQueryFilterKey filterKey,
    		boolean usedForSearch, boolean exclusive, 
    		TCKeyword selectedKeyword, final TCKeywordNode root) 
    {
    	this(id, filterKey, usedForSearch, exclusive, selectedKeyword!=null ? 
    			Collections.singletonList(selectedKeyword) : null, root);
    }
    
    public TCKeywordTreeInput(final String id, TCQueryFilterKey filterKey, 
    		boolean usedForSearch, final boolean exclusive, 
    		List<TCKeyword> selectedKeywords, final TCKeywordNode root) 
    {
        super(id, filterKey, usedForSearch, exclusive);

        setDefaultModel(new ListModel<TCKeyword>(selectedKeywords) {
			private static final long serialVersionUID = 1L;
			@Override
            public void setObject(List<TCKeyword> keywords)
            {
				List<TCKeyword> cur = getObject();
                if (!TCUtilities.equals(cur,keywords))
                {
                    super.setObject(keywords);                    
                    fireValueChanged();
                }
            }
        });
        
        this.root = root;
        
        final MultipleKeywordsTextModel textModel = new MultipleKeywordsTextModel(selectedKeywords);
        this.text = new AutoCompleteTextField<String>(
                "text", textModel, String.class, new AutoCompleteSettings()) {
        	private static final long serialVersionUID = 1L;
            @Override
            protected Iterator<String> getChoices(String s) {
                LinkedHashMap<String, TCKeyword> keywords = new LinkedHashMap<String, TCKeyword>();
                findMatchingKeywords(root, s, keywords);
                return keywords.keySet().iterator();
            }
            @Override
            protected void onComponentTag(ComponentTag tag)
            {
            	super.onComponentTag(tag);
            	if (exclusive)
            	{
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
        
        popup = new KeywordTreePopup();
        trigger = new WebMarkupContainer("chooser-button", new Model<String>("...")) {
        	@Override
        	protected void onComponentTag(ComponentTag tag)
        	{
        		super.onComponentTag(tag);
        		tag.put("onmouseover", "$(this).addClass('ui-state-hover')");
        		tag.put("onmouseout", "$(this).removeClass('ui-state-hover')");
        	}
        };
        
//        chooserBtn.add(new Image("chooser-button-img", ImageManager.IMAGE_TC_ARROW_DOWN)
//        .setOutputMarkupId(true));

        final Tree tree = new Tree("keyword-tree", new DefaultTreeModel(root)) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void populateTreeItem(WebMarkupContainer item, int level) {
                super.populateTreeItem(item, level);
                //(WEB-429) workaround: disable browser-native drag and drop
                item.add(new AttributeModifier("onmousedown", true, 
                		new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;
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
            	if (isMultipleKeywordSearchEnabled())
            	{
            		boolean selected = getTreeState().isNodeSelected(node);
            		if (selected)
            		{
            			popup.addSelectedNode((TCKeywordNode)node);
            		}
            		else
            		{
            			popup.removeSelectedNode((TCKeywordNode)node);
            		}
            	}
            	else
            	{
            		popup.setSelectedNodes((TCKeywordNode)node);
                    popup.hide(target);
            	}
            }
        };

        tree.setOutputMarkupId(true);
        tree.setRootLess(true);
        tree.setLinkType(LinkType.AJAX);
        tree.getTreeState().setAllowSelectMultiple(isMultipleKeywordSearchEnabled());

        popup.setTree(root, tree);
        popup.add(tree);

        add(text);
        add(trigger);
        add(popup);
    }
    
    @Override
    protected void onBeforeRender()
    {
    	super.onBeforeRender();
    	
    	if (!triggerInstalled)
    	{
        	popup.installPopupTrigger(trigger, new TCPopupPosition(
	                trigger.getMarkupId(),
	                popup.getMarkupId(), 
	                PopupAlign.BottomLeft, PopupAlign.TopLeft));
        	triggerInstalled = true;
    	}
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
    
    @Override
    public void setKeywords(TCKeyword...keywords){
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ListModel<TCKeyword> getModel() {
        return (ListModel) getDefaultModel();
    }
    
    private void setKeywordsAsList(List<TCKeyword> keywords)
    {
    	getModel().setObject(keywords);
    	((MultipleKeywordsTextModel)text.getModel()).setKeywordItems(keywords);
    	if (keywords==null || keywords.isEmpty())
    	{
    		popup.setSelectedNodes();
    	}
    }

    private void ensurePathExpanded(Tree tree, TCKeywordNode node) {
        if (node != null) {
            List<TCKeywordNode> path = node.getPath();
            if (path != null) {
                for (TCKeywordNode n : path) {
                    tree.getTreeState().expandNode(n);
                }
            }
        }
    }

    private TCKeywordNode findNode(TCKeywordNode root, String s) {
        if (root != null) {
            TCKeyword keyword1 = root.getKeyword();

            if (keyword1 != null && keyword1.toString().equals(s)) {
                return root;
            } else if (root.getChildCount() > 0) {
                for (TCKeywordNode child : root.getChildren()) {
                    TCKeywordNode node = findNode(child, s);
                    if (node != null) {
                        return node;
                    }
                }
            }
        }

        return null;
    }

    private void findMatchingKeywords(TCKeywordNode root, String s,
            LinkedHashMap<String, TCKeyword> matching) {
        if (root != null) {
            TCKeyword keyword = root.getKeyword();

            if (keyword != null)
            {
            	String sk = keyword.toString();
            	if (sk.toUpperCase().contains(s.toUpperCase())) {
            		matching.put(sk, keyword);
            	}
            }

            if (root.getChildCount() > 0) {
                for (TCKeywordNode child : root.getChildren()) {
                    findMatchingKeywords(child, s, matching);
                }
            }
        }
    }

    private TCKeywordNode findNode(TCKeywordNode root, TCKeyword keyword) {
        if (root != null) {
            TCKeyword keyword1 = root.getKeyword();

            if (keyword1 != null) {
                if ((keyword == null && keyword1.isAllKeywordsPlaceholder())
                        || (keyword != null && keyword1.equals(keyword))) {
                    return root;
                }
            }

            if (root.getChildCount() > 0) {
                for (TCKeywordNode child : root.getChildren()) {
                    TCKeywordNode node = findNode(child, keyword);
                    if (node != null) {
                        return node;
                    }
                }
            }
        }

        return null;
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
                    TCKeyword keyword = null;
                    TCKeywordNode node = root!=null ? findNode(root, item) : null;
                    if (node != null) {
                        keyword = node.getKeyword();
                    } 
                    if (keyword==null && item.length()>0) {
                        keyword = new TCKeyword(item, null, false);
                    }
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

    private class KeywordTreePopup extends AbstractTCPopup 
    {
		private static final long serialVersionUID = 2897120559101620288L;
		private TCKeywordNode root;
        private List<TCKeywordNode> selectedNodes;
        private Tree tree;

        public KeywordTreePopup() 
        {
            super("tree-keyword-popup", true, false, true);
            this.selectedNodes = new ArrayList<TCKeywordNode>();
        }

        public void setTree(TCKeywordNode root, Tree tree) {
            this.root = root;
            this.tree = tree;
        }
        
        public void addSelectedNode(TCKeywordNode node)
        {
        	if (!selectedNodes.contains(node))
        	{
        		selectedNodes.add(node);
        	}
        }
        
        public void removeSelectedNode(TCKeywordNode node)
        {
        	selectedNodes.remove(node);
        }
        
        public void setSelectedNodes(TCKeywordNode...nodes)
        {
            this.selectedNodes.clear();
            if (nodes!=null)
            {
            	for (TCKeywordNode node : nodes)
            	{
            		selectedNodes.add(node);
            	}
            }
        }
        
        @Override
        public void afterShowing(AjaxRequestTarget target)
        {
        	// reset selection
        	selectedNodes.clear();
            Collection<Object> selectedNodes = tree.getTreeState().getSelectedNodes();
            if (selectedNodes != null && !selectedNodes.isEmpty()) 
            {
                for (Object node : selectedNodes) 
                {
                    tree.getTreeState().selectNode(node, false);
                }
            }
            
            // select nodes
        	List<TCKeyword> keywords = getKeywordsAsList();
        	if (keywords!=null)
        	{
        		for (TCKeyword keyword : keywords)
        		{
                    TCKeywordNode toSelect = findNode(root, keyword);
                    if (toSelect != null) 
                    {
                        addSelectedNode(toSelect);
                        tree.getTreeState().selectNode(toSelect, true);
                        ensurePathExpanded(tree, toSelect);
                    }                     
        		}
        	}

            target.addComponent(tree);
        }

        @Override
        public void beforeHiding(AjaxRequestTarget target) 
        {
        	List<TCKeyword> keywords = null;
        	
        	if (selectedNodes!=null)
        	{
        		for (TCKeywordNode node : selectedNodes)
        		{
        			TCKeyword keyword = node.getKeyword();
        			
                    if (keyword!=null && !keyword.isAllKeywordsPlaceholder()) 
                    {
                        if (keywords==null)
                        {
                        	keywords = new ArrayList<TCKeyword>();
                        }
                        keywords.add(keyword);
                    }
        		}
        	}

            setKeywordsAsList(keywords);

            target.addComponent(text);
        }
    }
}
