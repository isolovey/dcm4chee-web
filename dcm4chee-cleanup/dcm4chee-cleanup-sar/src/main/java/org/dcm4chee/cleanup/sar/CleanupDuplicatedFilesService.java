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
package org.dcm4chee.cleanup.sar;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import java.util.List;

import javax.naming.InitialContext;

import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.PrivateFile;
import org.dcm4chee.cleanup.dao.FileCleanupLocal;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Oct 3, 2012
 */
public class CleanupDuplicatedFilesService extends ServiceMBeanSupport {

    private static Logger log = LoggerFactory.getLogger(CleanupDuplicatedFilesService.class);
    
    private static final String NONE ="NONE";
    private static char[] HEX_DIGIT = { '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    
    private String onlineFsGroup;

    private FileCleanupLocal dao;
    
    public String getOnlineFsGroup() {
        return onlineFsGroup == null ? NONE : onlineFsGroup;
    }
    
    public void setOnlineFsGroup(String s) {
        onlineFsGroup = NONE.equalsIgnoreCase(s) ? NONE : s;
    }
    
    public int countDuplicates() {
        if (onlineFsGroup == null) {
            return -1;
        }
        return lookupFileCleanupLocal().countDuplicatedFiles(onlineFsGroup);
    }
    
    public int countDuplicatedTrashFiles() {
        if (onlineFsGroup == null) {
            return -1;
        }
        return lookupFileCleanupLocal().countDuplicatedTrashFiles();
    }

    public String showDuplicates() {
        if (onlineFsGroup == null) {
            return "onlineFsGroup not configured!";
        }
        List<File> files = lookupFileCleanupLocal().getDuplicatedFiles(onlineFsGroup);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (File f : files) {
            sb.append("File (").append(++i).append(") - pk:").append(f.getPk())
            .append(" Pat:").append(f.getInstance().getSeries().getStudy().getPatient())
            .append(" Study:").append(f.getInstance().getSeries().getStudy().getStudyInstanceUID())
            .append(" Series:").append(f.getInstance().getSeries().getSeriesInstanceUID())
            .append(" filePath:"+f.getFilePath()).append("\n");
        }
        return sb.toString();
    }

    public String showDuplicatedTrashFiles() {
        List<PrivateFile> files = lookupFileCleanupLocal().getDuplicatedTrashFiles();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (PrivateFile f : files) {
            sb.append("TrashFile (").append(++i).append(") - pk:").append(f.getPk());
            if (f.getInstance() == null) {
                sb.append(" REMOVED from trash\n");
            } else {
                sb.append(" Pat:").append(f.getInstance().getSeries().getStudy().getPatient())
                .append(" Study:").append(f.getInstance().getSeries().getStudy().getStudyInstanceUID())
                .append(" Series:").append(f.getInstance().getSeries().getSeriesInstanceUID())
                .append(" filePath:").append(f.getFilePath()).append("\n");
            }
        }
        return sb.toString();
    }

    public String cleanup() throws IOException {
        if (onlineFsGroup == null) {
            return "onlineFsGroup not configured!";
        }
        java.io.File logFile = new java.io.File(ServerConfigLocator.locate().getServerLogDir(), "cleanup.log");
        FileWriter w = new FileWriter(logFile, true);
        w.write("###### Cleanup duplicated file references. Called at "+new Date()+"\n");
        List<File> files = lookupFileCleanupLocal().getDuplicatedFiles(onlineFsGroup);
        int i = 0;
        String oldPath = null;
        int countFolder = 0, countMissing = 0, countError = 0;
        java.io.File dir, srcFile = null;
        try {
            for (File f : files) {
                if (f.getFilePath().equals(oldPath)) {
                    if (srcFile != null) {
                        if (copyFile(srcFile, f)) {
                            countFolder++;
                        } else {
                            countError++;
                        }
                    } else {
                        countMissing++;
                    }
                } else {
                    dir = resolve(new java.io.File(f.getFileSystem().getDirectoryPath()));
                    srcFile = new java.io.File(dir, f.getFilePath());
                    oldPath = f.getFilePath();
                    w.write("File "+srcFile+" is referenced by different instances!\n");
                    if (!srcFile.exists()) {
                        srcFile = null;
                        w.write("  FILE NOT FOUND! Duplicates will be NOT corrected!\n");
                        countMissing++;
                    }
                }
                writeLog(w, f.getInstance().getSeries().getStudy().getPatient().toString(),
                        f.getInstance().getSeries().getStudy().getStudyInstanceUID(),
                        f.getInstance().getSeries().getSeriesInstanceUID(),
                        f.getInstance().getSOPInstanceUID() );
            }
        } finally {
            w.close();
        }
        int countTrash = lookupFileCleanupLocal().deleteDuplicatedTrashFiles();
        return "Corrected: "+countFolder+" of "+(countFolder+countError)+
            " Files in folder. \nFound "+countMissing+" Instances with references to missing files!"+
            "\nRemoved: "+countTrash+" Files in trash which are also referenced in folder.";
    }
    
    private void writeLog(FileWriter w, String pat, String study, String series, String instance) throws IOException {
        w.write("   ");
        w.write(pat);
        w.write("\n     Study   :");
        w.write(study);
        w.write("\n     Series  :");
        w.write(series);
        w.write("\n     Instance:");
        w.write(instance);
        w.write("\n");
    }
    
    private boolean copyFile(java.io.File src, File f) {
        try {
            int hash = f.getInstance().getSOPInstanceUID().hashCode();
            java.io.File destFile = createNewFile(src.getParentFile(), hash);
            log.info("Copy duplicated file "+src+" to "+destFile);
            FileChannel fos = null;
            FileChannel fis = null;
            try {
                fis = new FileInputStream(src).getChannel();
                fos = new FileOutputStream(destFile).getChannel();
                fis.transferTo(0, fis.size(), fos);
                String fPath = f.getFilePath();
                int pos = fPath.lastIndexOf('/');
                fPath = fPath.substring(0,++pos) + destFile.getName();
                log.info("Update filePath in File(pk="+f.getPk()+") to "+fPath);
                lookupFileCleanupLocal().updateFilePath(f, fPath);
            } catch (Exception e) {
                log.error("Failed to copy file! "+src, e);
                destFile.delete();
            } finally {
                close(fis);
                close(fos);
            }
        } catch (Exception x) {
            log.error("Copy of "+src+" failed!", x);
            return false;
        }
        return true;
    }
    
    public java.io.File createNewFile(java.io.File dir, int hash) throws Exception {
        java.io.File f = null;
        for (int i = 0; i < 10 ; i++) { //limit 10 exceptional retries
            try {
                f = new java.io.File(dir, toHex(hash++));
                if (f.createNewFile()) {
                    return f;
                } else {
                    i--;
                }
            } catch (Exception e) {
               log.warn("failed to create file: " + dir.getCanonicalPath()+". Will retry again.", e);
            }
        }
        throw new IOException("Failed to get new File in directory "+dir.getCanonicalPath());
    }
    
    public String toHex(int val) {
        char[] ch8 = new char[8];
        for (int i = 8; --i >= 0; val >>= 4) {
            ch8[i] = HEX_DIGIT[val & 0xf];
        }
        return String.valueOf(ch8);
    }
   
    private FileCleanupLocal lookupFileCleanupLocal() {
        if ( dao == null ) {
            try {
                InitialContext jndiCtx = new InitialContext();
                dao = (FileCleanupLocal) jndiCtx.lookup(FileCleanupLocal.JNDI_NAME);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return dao;
    }

    public static java.io.File resolve(java.io.File f) {
        if (f.isAbsolute())
            return f;
        try {
            java.io.File serverHomeDir = ServerConfigLocator.locate().getServerHomeDir();
            return new java.io.File(serverHomeDir, f.getPath());
        } catch (Throwable t) {
            return f;
        }
    }
    private void close(Closeable toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException ignore) {
                log.debug("Error closing : "+toClose.getClass().getName(), ignore);
            }
        }
    }
 
}

