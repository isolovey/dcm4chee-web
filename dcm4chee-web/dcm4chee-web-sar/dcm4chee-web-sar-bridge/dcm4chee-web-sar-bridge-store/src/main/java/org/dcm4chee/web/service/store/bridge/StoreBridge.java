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
package org.dcm4chee.web.service.store.bridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.management.ObjectName;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.web.service.common.FileImportOrder;
import org.dcm4chee.web.service.common.FileImportOrder.FileAndHeader;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since May 04, 2010
 */
public class StoreBridge extends ServiceMBeanSupport {
    
    private int bufsize = 65536;
    public StoreBridge() {
        super();
    }

    private ObjectName storeScpServiceName;
  
    public ObjectName getStoreScpServiceName() {
        return storeScpServiceName;
    }

    public void setStoreScpServiceName(ObjectName name) {
        this.storeScpServiceName = name;
    }
    
    public void importFile(FileImportOrder order) throws Exception {
        List<FileAndHeader> files = order.getFiles();
        int len = files.size();
        String prevSeriesIuid = null;
        DicomObject attrs;
        for (FileAndHeader file : files) {
            attrs = file.getHeaderAttributes();
            importFile(toFileDTO(file.getFile()), toDataset(attrs), prevSeriesIuid, --len == 0);
            prevSeriesIuid = attrs.getString(Tag.SeriesInstanceUID);
        }
    }
    
    private FileDTO toFileDTO(File f) {
        FileDTO dto = new FileDTO();
        dto.setDirectoryPath(f.getFileSystem().getDirectoryPath());
        dto.setFilePath(f.getFilePath());
        dto.setFileSize(f.getFileSize());
        dto.setFileMd5(md5StringtoBytes(f.getMD5Sum()));
        dto.setAvailability(f.getFileSystem().getAvailability().ordinal());
        dto.setFileStatus(f.getFileStatus());
        dto.setFileSystemGroupID(f.getFileSystem().getGroupID());
        dto.setFileSystemPk(f.getFileSystem().getPk());
        dto.setFileTsuid(f.getTransferSyntaxUID());
        dto.setPk(f.getPk());
        dto.setRetrieveAET(f.getFileSystem().getRetrieveAET());
        dto.setSopClassUID(f.getInstance().getSOPClassUID());
        dto.setSopInstanceUID(f.getInstance().getSOPInstanceUID());
        dto.setUserInfo(f.getFileSystem().getUserInfo());
        return dto;
    }

    private Dataset toDataset(DicomObject attrs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bufsize);
        DicomOutputStream dos = new DicomOutputStream(baos);
        dos.writeDataset(attrs, TransferSyntax.ExplicitVRLittleEndian);
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        String srcAet = attrs.getString(attrs.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID));
        ds.readDataset(new ByteArrayInputStream(baos.toByteArray()), DcmDecodeParam.EVR_LE, -1);
        ds.setPrivateCreatorID(PrivateTag.CreatorID);
        ds.putAE(PrivateTag.CallingAET, srcAet);
        ds.setPrivateCreatorID(null);
        return ds;
    }

    public static byte[] md5StringtoBytes(String s) {
        if (s == null)
            return null;
        char[] md5Hex = s.toCharArray();
        byte[] md5 = new byte[16];
        for (int i = 0; i < md5.length; i++) {
            md5[i] = (byte) ((Character.digit(md5Hex[i << 1], 16) << 4)
                    + Character.digit(md5Hex[(i << 1) + 1], 16));
        }
        return md5;        
    }

    public void importFile(FileDTO fileDTO, Dataset ds, String prevseriuid,
            boolean last) throws Exception {
        server.invoke(storeScpServiceName, "importFile", 
                new Object[] {fileDTO, ds, prevseriuid, last, true},
                new String[] {FileDTO.class.getName(), Dataset.class.getName(), String.class.getName(), boolean.class.getName(), boolean.class.getName()});
    }
 }

