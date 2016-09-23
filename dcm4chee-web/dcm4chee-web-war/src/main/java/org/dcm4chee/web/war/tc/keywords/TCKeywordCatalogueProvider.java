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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.wicket.resource.loader.PackageStringResourceLoader;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.dao.tc.TCDicomCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate.KeywordCatalogue;
import org.dcm4chee.web.war.tc.TCPanel;
import org.dcm4chee.web.war.tc.keywords.acr.ACRCatalogue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 30, 2011
 */
public class TCKeywordCatalogueProvider {

    private final static Logger log = LoggerFactory
            .getLogger(TCKeywordCatalogueProvider.class);

    private final static String ID_DELIMITER = "$";

    private static TCKeywordCatalogueProvider instance;
    
    private static FilenameFilter xmlFilefilter = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return name.endsWith(".xml");
        }
        
    };

    private final WebCfgDelegate configDelegate;
    
    private String customCataloguePath;
    
    private Map<String, TCKeywordCatalogue> idsToCatalogues;

    private Map<TCQueryFilterKey, TCKeywordCatalogue> keysToCatalogues;

    private Map<String, String> keysToCatalogueIds;

    private Map<File, Long> lastModified = new HashMap<File, Long>();

    private long nextCheckCatalogueTimestamp;
    private static final long RECHECK_DELAY = 3000;
    
    private TCKeywordCatalogueProvider() {
        configDelegate = WebCfgDelegate.getInstance();
        customCataloguePath = configDelegate.getTCKeywordCataloguesPath();
        idsToCatalogues = readAllCatalogues(customCataloguePath);
        keysToCatalogues = assignCatalogues(idsToCatalogues);
    }

    public static synchronized TCKeywordCatalogueProvider getInstance() {
        if (instance == null) {
            instance = new TCKeywordCatalogueProvider();
        }

        return instance;
    }

    public boolean hasCatalogue(TCQueryFilterKey key) {
        checkAndInitCatalogues();
        
        return keysToCatalogues.containsKey(key);
    }
    
    public boolean isCatalogueExclusive(TCQueryFilterKey key) {
        return hasCatalogue(key) &&
            configDelegate.isTCKeywordCatalogueExclusive(key.name());
    }

    public TCKeywordCatalogue getCatalogue(TCQueryFilterKey key) {
        checkAndInitCatalogues();
        
        return keysToCatalogues.get(key);
    }
    
    private void checkAndInitCatalogues()
    {
        if (System.currentTimeMillis() < nextCheckCatalogueTimestamp) 
            return;
        nextCheckCatalogueTimestamp = System.currentTimeMillis() + RECHECK_DELAY;
        // if the custom catalogue path OR the number of catalogues (i.e files) has changed
        // -> need to read all catalogues again
        String confPath = configDelegate.getTCKeywordCataloguesPath();
        Boolean needReloadAll = checkOutdated(confPath);
        if (needReloadAll == Boolean.TRUE){
            customCataloguePath = confPath;
            idsToCatalogues = readAllCatalogues(confPath);
            keysToCatalogues = assignCatalogues(idsToCatalogues);
            log.info("Full update of tc keyword catalogues and settings");
        } else if (needReloadAll == Boolean.FALSE) {
            // check, if the one of the custom catalogue's source file has changed
            // -> if so, need to re-read the catalogue
            Map<String, File> toRead = null;
            for (Map.Entry<String, TCKeywordCatalogue> me : idsToCatalogues.entrySet())
            {
                TCKeywordCatalogue c = me.getValue();
                if (c instanceof ICustomTCKeywordCatalogue)
                {
                    ICustomTCKeywordCatalogue custom = (ICustomTCKeywordCatalogue) c;
                    File sourceFile = custom.getSourceFile();
                    
                    if (sourceFile!=null && sourceFile.lastModified()!=custom.getLastModified())
                    {
                        if (toRead==null)
                        {
                            toRead = new HashMap<String, File>(2);
                        }
                        
                        if (!toRead.containsKey(me.getKey()))
                        {
                            toRead.put(me.getKey(), sourceFile);
                        }
                    }
                }
            }
            
            if (toRead!=null)
            {
                for (Map.Entry<String, File> me : toRead.entrySet())
                {
                    try 
                    {
                        TCKeywordCatalogue cat = readCustomCatalogue(me.getValue());
                        
                        if (cat!=null)
                        {
                            idsToCatalogues.remove(me.getKey());
                            idsToCatalogues.put(cat.getDesignatorId() + ID_DELIMITER + cat.getId(), cat);

                            log.info("Updated teaching-file keyword catalogue: "
                                    + cat);
                        }
                    } 
                    catch (Exception e) {
                        log.error(
                                "Parsing teaching-file keyword catalogue failed! Invalid syntax in file "
                                        + me.getValue().getAbsolutePath(), e);
                    }
                }
            }
        }
        // and finally check, if the assignment of tc keys -> catalogues has changed
        // if so, re-assign catalogues again
        if (needReloadAll == Boolean.FALSE || !keysToCatalogueIds.equals(configDelegate.getTCKeywordCataloguesAsString()))
        {
            keysToCatalogues = assignCatalogues(idsToCatalogues);
            
            log.info("Updated teaching-file keyword catalogue assignments");
        }

    }
        
    
    private Map<TCQueryFilterKey, TCKeywordCatalogue> assignCatalogues(Map<String, TCKeywordCatalogue> idsToCatalogues)
    {
        Map<TCQueryFilterKey, TCKeywordCatalogue> assignedCatalogues = new HashMap<TCQueryFilterKey, TCKeywordCatalogue>();
        
        if (idsToCatalogues!=null && !idsToCatalogues.isEmpty())
        {
            keysToCatalogueIds = configDelegate.getTCKeywordCataloguesAsString();
            
            Map<String, KeywordCatalogue> configuredCatalogues = configDelegate.getTCKeywordCatalogues();
            
            for (Map.Entry<String, KeywordCatalogue> me : configuredCatalogues.entrySet()) {
                try {
                    TCQueryFilterKey key = TCQueryFilterKey.valueOf(me.getKey());
    
                    if (key != null) {
                        TCKeywordCatalogue catalogue = idsToCatalogues.get(me
                                .getValue().getDesignator()
                                + ID_DELIMITER
                                + me.getValue().getId());
                        if (catalogue != null) {
                            assignedCatalogues.put(key, catalogue);
                        } else {
                            log.warn("Configured keyword catalogue not supported: "
                                    + me.getValue().toString());
                        }
                    } else {
                        log.warn("Configured keyword catalogue attribute not supported: "
                                + me.getKey());
                    }
                } catch (Exception e) {
                    log.error("Initializing TC keyword provider failed!", e);
                }
            }
        }
        
        return assignedCatalogues;
    }
    
    
    private Map<String, TCKeywordCatalogue> readAllCatalogues(String customCataloguePath)
    {
        Map<String, TCKeywordCatalogue> catalogues = new HashMap<String, TCKeywordCatalogue>();
        lastModified.clear();
        
        // read in ACR keyword catalogue
        ACRCatalogue acr = ACRCatalogue.getInstance();
        catalogues.put(acr.getDesignatorId() + ID_DELIMITER + acr.getId(),
                acr);

        log.info("Added teaching-file keyword catalogue: " + acr);

        // read in custom keyword catalogues
        if (customCataloguePath!=null && !customCataloguePath.isEmpty())
        {
            catalogues.putAll(readCustomCatalogues(customCataloguePath));
        }
        
        return catalogues;
    }
    

    private Map<String, TCKeywordCatalogue> readCustomCatalogues(String path) {
        if (path != null) {
            File file = FileUtils.resolve(new File(path));

            if (file.exists()) {
                Map<String, TCKeywordCatalogue> catalogues = new HashMap<String, TCKeywordCatalogue>();

                File[] candidates = file.isDirectory() ? file.listFiles(xmlFilefilter) :
                        file.getName().endsWith(".xml") ? new File[] { file } : null;
                if (candidates != null) {
                    for (File f : candidates) {
                        try 
                        {
                            TCKeywordCatalogue cat = readCustomCatalogue(f);
                            
                            if (cat!=null)
                            {
                                catalogues.put(cat.getDesignatorId() + ID_DELIMITER + cat.getId(), cat);

                                log.info("Added teaching-file keyword catalogue: "
                                        + cat);
                            }
                        } 
                        catch (Exception e) {
                            log.error(
                                    "Parsing teaching-file keyword catalogue failed! Invalid syntax in file "
                                            + file.getAbsolutePath(), e);
                        }
                    }
                }
                return catalogues;
            }
        }

        return Collections.emptyMap();
    }
    
    
    private TCKeywordCatalogue readCustomCatalogue(File f) throws Exception
    {
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(f);

            DocumentBuilderFactory dbf = DocumentBuilderFactory
                    .newInstance();

            dbf.setValidating(false);
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setNamespaceAware(true);

            Document doc = dbf.newDocumentBuilder().parse(
                    fis);

            lastModified.put(f, f.lastModified());
            if (doc.getElementsByTagName("simple-list").getLength() > 0) //$NON-NLS-1$
            {
                TCKeywordCatalogueXMLList c = TCKeywordCatalogueXMLList.createInstance(doc);
                c.setSourceFile(f);
                
                return c;
            } 
            else if (doc.getElementsByTagName(
                    "simple-tree").getLength() > 0) //$NON-NLS-1$
            {
                TCKeywordCatalogueXMLTree c = TCKeywordCatalogueXMLTree.createInstance(doc);
                c.setSourceFile(f);
                
                return c;
            } 
            else {
                throw new UnsupportedOperationException(
                        "Unsupported XML format!"); //$NON-NLS-1$
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    log.error(null, e);
                }
            }
        }
    }
    
    /**
     * null... all uptodate
     * true... Need full reload
     * false.. one ore more files are outdated
     * @param path
     * @return
     */
    private Boolean checkOutdated(String path) {
        if (!customCataloguePath.equals(path))
            return Boolean.TRUE;
        if (path == null || path.trim().isEmpty())
            return null;
        
        File file = FileUtils.resolve(new File(path));

        Boolean result = null;
        if (file.exists()) {
            File[] candidates = file.isDirectory() ? file.listFiles(xmlFilefilter) :
                    file.getName().endsWith(".xml") ? new File[] { file } :null;
            if (candidates != null) {
                if (lastModified.size() != candidates.length)
                    return Boolean.TRUE;
                Long lm;
                for (File f : candidates) {
                    lm =lastModified.get(f);
                    if (lm == null)
                        return Boolean.TRUE;
                    if (f.lastModified() > lm)
                        result = Boolean.FALSE;
                }
            }
        }
        return result;
    }
    
    private static interface ICustomTCKeywordCatalogue
    {
        void setSourceFile(File f);
        File getSourceFile();
        long getLastModified();
    }
    

    private static class TCKeywordCatalogueXMLList extends TCKeywordCatalogue implements ICustomTCKeywordCatalogue {
        private List<TCKeyword> keywords;
        
        private File f;
        private long lastModified;
        
        private TCKeywordCatalogueXMLList(String id, String designatorId,
                String designatorName, List<TCKeyword> keywords) {
            super(id, designatorId, designatorName);

            this.keywords = keywords;
        }

        public static TCKeywordCatalogueXMLList createInstance(Document doc) {
            Node rootNode = doc.getElementsByTagName("coding-system").item(0); //$NON-NLS-1$
            NamedNodeMap rootAttrs = rootNode.getAttributes();

            String systemId = rootAttrs.getNamedItem("id").getTextContent(); //$NON-NLS-1$
            String designatorId = rootAttrs
                    .getNamedItem("designator-id").getTextContent(); //$NON-NLS-1$
            String designatorName = rootAttrs
                    .getNamedItem("designator-name").getTextContent(); //$NON-NLS-1$
            String id = designatorId + ID_DELIMITER + systemId; //$NON-NLS-1$

            List<TCKeyword> values = new ArrayList<TCKeyword>();

            NodeList nodes = doc.getElementsByTagName("simple-list"); //$NON-NLS-1$
            if (nodes != null && nodes.getLength() > 0) {
                NodeList valueNodes = nodes.item(0).getChildNodes();

                for (int i = 0; i < valueNodes.getLength(); i++) {
                    Node node = valueNodes.item(i);

                    if ("code".equals(node.getNodeName())) //$NON-NLS-1$
                    {
                        NamedNodeMap attrs = valueNodes.item(i).getAttributes();

                        String value = attrs.getNamedItem("value")
                                .getTextContent();
                        String meaning = attrs.getNamedItem("meaning")
                                .getTextContent();

                        values.add(new TCKeyword(new TCDicomCode(id, value,
                                meaning)));
                    }
                }
            }

            return new TCKeywordCatalogueXMLList(systemId, designatorId,
                    designatorName, values);
        }

        public List<TCKeyword> getKeywords() {
            return Collections.unmodifiableList(keywords);
        }

        @Override
        public TCKeywordInput createInput(final String id, TCQueryFilterKey filterKey, 
        		boolean usedForSearch, boolean exclusive,
                TCKeyword...selectedKeywords) 
        {
            return new TCKeywordListInput(id, filterKey, usedForSearch, exclusive,
            		selectedKeywords!=null?Arrays.asList(selectedKeywords):null, getKeywords());
        }

        @Override
        public TCKeyword findKeyword(String value) {
            if (value != null) {
                for (TCKeyword keyword : keywords) {
                	TCDicomCode code = keyword.getCode();
                    if (value.equals(code.getValue())) {
                        return keyword;
                    }
                }
            }

            return null;
        }
        
        @Override
        public void setSourceFile(File f)
        {
            this.f = f;
            this.lastModified = f.lastModified();
        }
        
        @Override
        public File getSourceFile()
        {
            return f;
        }
        
        @Override
        public long getLastModified()
        {
            return lastModified;
        }
    }

    private static class TCKeywordCatalogueXMLTree extends TCKeywordCatalogue implements ICustomTCKeywordCatalogue{
        private TCKeywordNode root;
        
        private File f;
        private long lastModified;
        
        private TCKeywordCatalogueXMLTree(String id, String designatorId,
                String designatorName, TCKeywordNode root) {
            super(id, designatorId, designatorName);
            this.root = root;
        }

        public static TCKeywordCatalogueXMLTree createInstance(Document doc) {
            Node rootNode = doc.getElementsByTagName("coding-system").item(0); //$NON-NLS-1$
            NamedNodeMap rootAttrs = rootNode.getAttributes();

            String systemId = rootAttrs.getNamedItem("id").getTextContent(); //$NON-NLS-1$
            String designatorId = rootAttrs
                    .getNamedItem("designator-id").getTextContent(); //$NON-NLS-1$
            String designatorName = rootAttrs
                    .getNamedItem("designator-name").getTextContent(); //$NON-NLS-1$
            String id = designatorId + ID_DELIMITER + systemId; //$NON-NLS-1$

            TCKeywordNode root = new TCKeywordNode();

            root.addChildren(new TCKeywordNode(
                    TCKeyword
                            .createAllKeywordsPlaceholder(new PackageStringResourceLoader()
                                    .loadStringResource(TCPanel.class,
                                            "tc.search.null.text", null, null))));

            NodeList nodes = doc.getElementsByTagName("simple-tree"); //$NON-NLS-1$
            if (nodes != null && nodes.getLength() > 0) {
                NodeList codeNodes = nodes.item(0).getChildNodes();

                for (int i = 0; i < codeNodes.getLength(); i++) {
                    TCKeywordNode node = createTree(id, codeNodes.item(i));
                    if (node != null) {
                        root.addChildren(node);
                    }
                }
            }

            return new TCKeywordCatalogueXMLTree(systemId, designatorId,
                    designatorName, root);
        }

        public TCKeywordNode getRoot() {
            return root;
        }

        @Override
        public TCKeywordInput createInput(final String id,
        		TCQueryFilterKey filterKey, boolean usedForSearch, boolean exclusive,
                TCKeyword...selectedKeywords) {
            return new TCKeywordTreeInput(id, filterKey, usedForSearch, exclusive,
            		selectedKeywords!=null?Arrays.asList(selectedKeywords):null, getRoot());
        }

        @Override
        public TCKeyword findKeyword(String value) {
            return findKeyword(value, root);
        }
        
        @Override
        public void setSourceFile(File f)
        {
            this.f = f;
            this.lastModified = f.lastModified();
        }
        
        @Override
        public File getSourceFile()
        {
            return f;
        }
        
        @Override
        public long getLastModified()
        {
            return lastModified;
        }

        private TCKeyword findKeyword(String value, TCKeywordNode node) {
            if (value != null && node != null) {
                TCKeyword keyword = node.getKeyword();
                if (keyword!=null)
                {
                	TCDicomCode code = keyword.getCode();
                    if (code!=null && value.equals(code.getValue())) {
                        return node.getKeyword();
                    }
                }
                
                if (node.getChildCount() > 0) {
                    for (TCKeywordNode child : node.getChildren()) {
                        keyword = findKeyword(value, child);
                        if (keyword != null) {
                            return keyword;
                        }
                    }
                }
            }

            return null;
        }

        private static TCKeywordNode createTree(String id, Node node) {
            TCKeywordNode parent = createNode(id, node);

            if (parent != null) {
                NodeList nodes = node.getChildNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        TCKeywordNode child = createTree(id, nodes.item(i));
                        if (child != null) {
                            parent.addChildren(child);
                        }
                    }
                }

                return parent;
            }

            return null;
        }

        private static TCKeywordNode createNode(String id, Node node) {
            if ("code".equals(node.getNodeName())) //$NON-NLS-1$
            {
                NamedNodeMap attrs = node.getAttributes();

                String value = attrs.getNamedItem("value").getTextContent();
                String meaning = attrs.getNamedItem("meaning").getTextContent();

                return new TCKeywordNode(new TCKeyword(new TCDicomCode(id, value,
                        meaning)));
            }

            return null;
        }
    }

}
