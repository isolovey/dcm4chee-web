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
package org.dcm4chee.web.service.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.ContentHandlerAdapter;
import org.dcm4che2.io.SAXWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 14, 2010
 */
public class XSLTUtils {

    public static final SAXTransformerFactory transformerFactory =
        (SAXTransformerFactory) TransformerFactory.newInstance();

    protected static final Logger log = LoggerFactory.getLogger(XSLTUtils.class);
            
    public static void xslt(DicomObject attrs, Templates tpl, OutputStream out, Map<String,String> parameter) throws TransformerConfigurationException, SAXException, IOException {
        TransformerHandler th = tpl == null ? transformerFactory.newTransformerHandler() : 
            transformerFactory.newTransformerHandler(tpl);
        Transformer t = th.getTransformer();
        setParameter(parameter, t);
        th.setResult(new StreamResult(out));
        SAXWriter writer = new SAXWriter(th,null);
        writer.write(attrs);
    }
 
    public static void xslt(DicomObject attrs, Templates tpl, DicomObject out, Map<String,String> parameter) throws TransformerConfigurationException, SAXException, IOException {
        TransformerHandler th = tpl == null ? transformerFactory.newTransformerHandler() : 
            transformerFactory.newTransformerHandler(tpl);
        ContentHandlerAdapter cha = new ContentHandlerAdapter(out);
        Transformer t = th.getTransformer();
        setParameter(parameter, t);
        th.setResult(new SAXResult(cha));
        SAXWriter writer = new SAXWriter(th,null);
        writer.write(attrs);
    }
    public static void xslt(DicomObject attrs, Templates[] tpls, DicomObject out, Map<String,String> parameter) throws TransformerConfigurationException, SAXException, IOException {
        if(tpls!=null && tpls.length > 1) {
            TransformerHandler[] thChain = toTransformerHandlerChain(tpls);
            if (parameter != null) {
                for ( int i=0 ; i<thChain.length ; i++) {
                    setParameter(parameter, thChain[i].getTransformer());
                }
            }
            xslt(attrs, thChain, out);
        } else {
            xslt(attrs, tpls == null ? null : tpls[0], out, parameter);
        }
}

    public static void xslt(DicomObject attrs, Templates[] tpls, OutputStream out, Map<String,String> parameter) throws TransformerConfigurationException, SAXException, IOException {
        if(tpls!=null && tpls.length > 1) {
            TransformerHandler[] thChain = toTransformerHandlerChain(tpls);
            if (parameter != null) {
                for ( int i=0 ; i<thChain.length ; i++) {
                    setParameter(parameter, thChain[i].getTransformer());
                }
            }
            xslt(attrs, thChain, out);
        } else {
            xslt(attrs, tpls == null ? null : tpls[0], out, parameter);
        }
    }

    public static void xslt(DicomObject attrs, TransformerHandler[] thChain,
            OutputStream out) throws SAXException, IOException {
        thChain[thChain.length-1].setResult(new StreamResult(out));
        SAXWriter writer = new SAXWriter(thChain[0],null);
        writer.write(attrs);
    }
    
    public static void xslt(DicomObject attrs, TransformerHandler[] thChain, Result result) throws SAXException, IOException {
        thChain[thChain.length-1].setResult(result);
        SAXWriter writer = new SAXWriter(thChain[0],null);
        writer.write(attrs);
    }
    
    public static void xslt(DicomObject attrs, TransformerHandler[] thChain,
            DicomObject out) throws SAXException, IOException {
        ContentHandlerAdapter cha = new ContentHandlerAdapter(out);
        thChain[thChain.length-1].setResult(new SAXResult(cha));
        SAXWriter writer = new SAXWriter(thChain[0],null);
        writer.write(attrs);
    }


    public static TransformerHandler[] toTransformerHandlerChain(Templates[] tpls) throws TransformerConfigurationException, SAXException, IOException {
        if(tpls==null || tpls.length < 1) {
            return null;
        }
        TransformerHandler[] thChain = new TransformerHandler[tpls.length];
        thChain[0] = transformerFactory.newTransformerHandler(tpls[0]);
        for ( int i=1, prev=0 ; i<tpls.length ; i++,prev++) {
            thChain[i] = transformerFactory.newTransformerHandler(tpls[i]);
            thChain[prev].setResult(new SAXResult(thChain[i]));
        }
        return thChain;
    }

    private static void setParameter(Map<String, String> parameter,
            Transformer t) {
        if (parameter != null) {
            for ( Map.Entry<String,String> e : parameter.entrySet() ) {
                t.setParameter(e.getKey(), e.getValue());
            }
        }
    }
    
    public static void dump(DicomObject attrs, Templates tpl, String filename, boolean comment) throws TransformerConfigurationException, SAXException, IOException {
        TransformerHandler th = tpl == null ? transformerFactory.newTransformerHandler() : 
            transformerFactory.newTransformerHandler(tpl);
        FileOutputStream out = new FileOutputStream(filename);
        th.setResult(new StreamResult(out));
        SAXWriter writer = new SAXWriter(th,comment ? th : null);
        try {
            writer.write(attrs);
        } finally {
            out.close();
        }
        
    }
}

