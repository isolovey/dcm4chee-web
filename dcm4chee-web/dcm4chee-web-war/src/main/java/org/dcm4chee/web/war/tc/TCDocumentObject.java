package org.dcm4chee.web.war.tc;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.log4j.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.Resource;
import org.apache.wicket.markup.html.DynamicWebResource;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.UrlResourceStream;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Time;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.util.DateUtils;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.dao.tc.TCQueryLocal;
import org.dcm4chee.web.war.folder.delegate.TarRetrieveDelegate;
import org.dcm4chee.web.war.folder.delegate.WADODelegate;

@SuppressWarnings("serial")
public abstract class TCDocumentObject implements Serializable {
	
	private static final Logger log = Logger.getLogger(TCDocumentObject.class);
	
	private static final String SERIES_DESCRIPTION = "File import"; //$NON-NLS-1$
	
	private MimeType mimeType;
    private DicomObject metaData;
	private TCReferencedInstance ref;
	
    protected TCDocumentObject(MimeType mimeType, TCReferencedInstance ref, DicomObject metaData) {
        this.metaData = metaData;
        this.ref = ref;
        this.mimeType = mimeType;
    }

    public static TCDocumentObject create(TCReferencedInstance ref) throws Exception {

    	Instance instance = ((TCQueryLocal) JNDIUtils.lookup(
        		TCQueryLocal.JNDI_NAME)).findInstanceByUID(
        		ref.getInstanceUID());
    	
    	if (instance==null) {
    		return null;
    	}
    	
        DicomObject attrs = instance.getAttributes(false);
        
        MimeType mimeType = null;
        String mimeTypeStr = attrs.getString(Tag.MIMETypeOfEncapsulatedDocument);
        if (mimeTypeStr!=null) {
        	mimeType = MimeType.get(mimeTypeStr);
        }
        if (mimeType==null) {
        	mimeType = getDocumentMimeTypeFromLabel(
        			attrs.getString(Tag.ContentLabel));
        }
        
        if (mimeType!=null) {
        	DocumentType docType = mimeType.getDocumentType();

        	if (TCImageDocument.DOC_TYPES.contains(docType)) {
        		return TCImageDocument.create(ref);
        	}
        	else if (TCEncapsulatedDocument.DOC_TYPES.contains(docType)) {
        		return TCEncapsulatedDocument.create(ref);
        	}
        }
        
    	throw new Exception("Unable to create TC encapsulated object: Mime type not supported (" + mimeTypeStr + ")");
    }
    
    public static boolean isDocument(TCReferencedInstance ref) {
    	return getDocumentImplementationClass(ref)!=null;
    }
    
    public static TCDocumentObject create(TCObject tc, MimeType mimeType, String filename, InputStream in) throws Exception {
    	return create(tc, mimeType, filename, in, null);
    }
    
    public static TCDocumentObject create(TCObject tc, MimeType mimeType, String filename, InputStream in, String description) throws Exception {
    	if (mimeType!=null) {
    		DocumentType docType = mimeType.getDocumentType();
    		
    		if (TCImageDocument.DOC_TYPES.contains(docType)) {
    			return TCImageDocument.create(tc, mimeType, filename, in, description);
    		}
    		else if (TCEncapsulatedDocument.DOC_TYPES.contains(docType)) {
    			return TCEncapsulatedDocument.create(tc, mimeType, filename, in, description);
    		}
    	}
        
    	throw new Exception("Unable to create TC encapsulated object: MimeType not supported (" + mimeType + ")"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    public TCReferencedInstance getAsReferencedInstance() {
    	return ref;
    }
    
    public String getSOPClassUID() {
    	return metaData.getString(Tag.SOPClassUID);
    }
    
    public String getSOPInstanceUID()
    {
        return metaData.getString(Tag.SOPInstanceUID);
    }
    
    public String getSeriesInstanceUID()
    {
    	return metaData.getString(Tag.SeriesInstanceUID);
    }
    
    public String getStudyInstanceUID()
    {
    	return metaData.getString(Tag.StudyInstanceUID);
    }
    
    public MimeType getMimeType() {
    	return mimeType;
    }
    
    public String getDocumentFileName() {
    	return getDocumentNameFromLabel(metaData.getString(Tag.ContentLabel), true);
    }
    
    public String getDocumentName() {
    	return getDocumentNameFromLabel(metaData.getString(Tag.ContentLabel), false);
    }
    
    public Date getDocumentAddedDate() {
    	Date date = metaData.getDate(Tag.ContentDate);
    	Date time = metaData.getDate(Tag.ContentTime);
    	
    	if (date!=null && time!=null) {
    		return DateUtils.toDateTime(date, time);
    	}
    	else if (date!=null) {
    		return date;
    	}
    	
    	return null;
    }
    
    public String getDocumentDescription() {
    	return metaData.containsValue(Tag.DocumentTitle) ?
        		metaData.getString(Tag.DocumentTitle) : 
        			metaData.getString(Tag.ContentDescription);
    }

    public DicomObject toDataset() throws Exception{
    	return metaData;
    }
        
    public abstract Resource getDocumentContent(boolean forDownloading);
    
    public abstract Resource getDocumentThumbnail();
    
    protected abstract boolean isContentAvailable();
            
    protected void ensureContentAvailable() {
    	if (!isContentAvailable()) {            
            DicomInputStream dis = null;
            try {
        		List<org.dcm4chee.archive.entity.File> files = ((TCQueryLocal) JNDIUtils.lookup(
                		TCQueryLocal.JNDI_NAME)).findInstanceByUID(
                		getSOPInstanceUID()).getFiles();
                String fsId = files.get(0).getFileSystem().getDirectoryPath();
                String fileId = files.get(0).getFilePath();
            	dis = new DicomInputStream(fsId.startsWith("tar:") ? 
            			TarRetrieveDelegate.getInstance().retrieveFileFromTar(fsId, fileId) :
            				FileUtils.resolve(new File(fsId, fileId)));
                metaData = dis.readDicomObject();
            }
            catch (Exception e) {
            	log.error("Unable to retrieve instance!", e); //$NON-NLS-1$
            } finally {
                if (dis != null) {
                    try {
                    	dis.close();
                    }
                    catch (Exception e2) {
                    }
                }
            }
    	}
    }
    
    protected static String getFileExtension(String filename) {
		if (filename!=null) {
	    	int index = filename!=null ? filename.lastIndexOf(".") : -1; //$NON-NLS-1$
	    	if (index>=0 && index<filename.length()-1) {
	    		return filename.substring(index+1);
	    	}
		}
		return null;
	}

	protected static String getFileName(String filename) {
		String ext = getFileExtension(filename);
		if (ext!=null) {
	    	return filename.substring(0, filename.indexOf("."+ext));
		}
		return null;
	}

	private static MimeType getDocumentMimeTypeFromLabel(String label) {
    	if (label!=null) {
	    	int index = label!=null ? label.lastIndexOf("(") : -1; //$NON-NLS-1$
	    	String filename = index>0 ? label.substring(0, index-1) : label;
	    	return MimeType.guessFromFileExtension(
	    			getFileExtension(filename));
    	}
    	return null;
    }
    
    private static String getDocumentNameFromLabel(String label, boolean withFileExtension) {
    	if (label!=null) {
	    	int index = label!=null ? label.lastIndexOf("(") : -1; //$NON-NLS-1$
	    	String filename = index>0 ? label.substring(0, index-1) : label;
	    	if (withFileExtension) {
	    		return filename;
	    	}
	    	else {
	    		return getFileName(filename);
	    	}
    	}
    	return null;
    }
    
    protected static DicomObject createDicomMetaData(MimeType mimeType, String filename, String description, TCReferencedInstance ref, 
    		String patId, String issuerOfPatId, String patName, String modality) {
    	Date now = new Date();
    	
    	DicomObject attrs = new BasicDicomObject();

    	// patient level
    	if (patId!=null) {
    		attrs.putString(Tag.PatientID, VR.LO, patId);
    	}
    	if (issuerOfPatId!=null) {
    		attrs.putString(Tag.IssuerOfPatientID, VR.LO, issuerOfPatId);
    	}
    	if (patName!=null) {
    		attrs.putString(Tag.PatientName, VR.PN, patName);
    	}
    	
    	// study level
    	attrs.putString(Tag.StudyInstanceUID, VR.UI, ref.getStudyUID());
    	
    	// series level
    	attrs.putString(Tag.SeriesInstanceUID, VR.UI, ref.getSeriesUID());
    	attrs.putString(Tag.SeriesDescription, VR.LO, SERIES_DESCRIPTION);
    	attrs.putString(Tag.Modality, VR.CS, modality);
    	
    	// instance level
    	attrs.putString(Tag.SOPInstanceUID, VR.UI, ref.getInstanceUID());
    	attrs.putString(Tag.SOPClassUID, VR.UI, ref.getClassUID());
    	attrs.putInt(Tag.InstanceNumber, VR.IS, ref.getInstanceNumber());
    	attrs.putDate(Tag.InstanceCreationDate, VR.DA, now);
    	attrs.putDate(Tag.InstanceCreationTime, VR.TM, now);
    	attrs.putDate(Tag.ContentDate, VR.DA, now);
    	attrs.putDate(Tag.ContentTime, VR.TM, now);
    	attrs.putString(Tag.ContentLabel, VR.CS, filename);
    	attrs.putString(Tag.ContentDescription, VR.CS, description!=null && !description.isEmpty() ?
    			description : getFileName(filename));
    	
    	return attrs;
    }
    
    public static Class<?> getDocumentImplementationClass(TCReferencedInstance ref) {
    	String cuid = ref.getClassUID();
    	if (UID.MultiFrameTrueColorSecondaryCaptureImageStorage.equals(cuid)) {
    		TCReferencedSeries series = ref.getSeries();
    		String description = series!=null ? series.getSeriesDescription() : null;
    		if (description!=null && description.equalsIgnoreCase(SERIES_DESCRIPTION)) {
    			return TCImageDocument.class;
    		}
    	}
    	else if (UID.EncapsulatedPDFStorage.equals(cuid)) {
    		return TCEncapsulatedDocument.class;
    	}
    	return null;
    }
    
    private static final TCReferencedInstance createReferencedInstance(TCObject tc, String cuid, String modality) {
    	TCReferencedStudy study = getDocumentStudy(tc);
    	if (study==null) {
    		study = new TCReferencedStudy(tc.getStudyInstanceUID());
    	}
    	TCReferencedSeries series = getDocumentSeries(study, modality);
    	if (series==null) {
    		study.addSeries(series=new TCReferencedSeries(
    				UIDUtils.createUID(),study,SERIES_DESCRIPTION));
    	}
    	Integer instanceNumber = -1;
    	if (series!=null) {
    		try {
    			String nr = series.getSeriesValue(
						Tag.NumberOfSeriesRelatedInstances);
    			if (nr!=null) {
    				instanceNumber = Integer.parseInt(nr)+1;
    			}
    		}
    		catch (Exception e) {
    		}
    		
        	if (instanceNumber<0) {
        		instanceNumber = series.getInstanceCount()+1;
        	}
    	}

    	if (instanceNumber<0) {
    		instanceNumber = 1;
    	}
    	
    	return new TCReferencedInstance(series,
    			UIDUtils.createUID(), cuid, instanceNumber);
    }
    
    private static final TCReferencedStudy getDocumentStudy(TCObject tc) {
    	String stuid = tc.getStudyInstanceUID();
    	for (TCReferencedStudy study : tc.getReferencedStudies()) {
    		if (stuid.equals(study.getStudyUID())) {
    			return study;
    		}
    	}
    	return null;
    }
    
    private static final TCReferencedSeries getDocumentSeries(TCReferencedStudy study, String modality) {
    	for (TCReferencedSeries series : study.getSeries()) {
    		String series_modality = series.getSeriesValue(Tag.Modality);
    		String series_description = series.getSeriesDescription();
    		if (series_modality!=null && series_modality.equalsIgnoreCase(modality) &&
    				series_description!=null && series_description.equalsIgnoreCase(SERIES_DESCRIPTION)) {
    			return series;
    		}
    	}

    	return null;
    }
        

    /*
     * ENCAPSULATED IMAGE
     */
    private static class TCImageDocument extends TCDocumentObject {
    	public static final String MODALITY = "OT"; //$NON-NLS-1$
    	public static final List<DocumentType> DOC_TYPES = Arrays.asList(
    			new DocumentType[] {DocumentType.JPEG, DocumentType.BMP, 
    					DocumentType.GIF, DocumentType.PNG});
    	private static final boolean dicomReaderAvailable = 
    			isImageIODicomReaderAvailable();
    	
    	private transient URL contentURL;
    	private transient BufferedImage contentImage;
    	private transient BufferedImage thumbnailImage;
    	
    	private static boolean isImageIODicomReaderAvailable() {
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("dicom");
			 return readers!=null && readers.hasNext();
    	}
    	
    	private TCImageDocument(MimeType mimeType, TCReferencedInstance ref, DicomObject o) {
    		super(mimeType, ref, o);
    	}
    	
    	private TCImageDocument(MimeType mimeType, TCReferencedInstance ref, DicomObject o, BufferedImage image) {
    		super(mimeType, ref, o);
    		this.contentURL = createWadoURL(mimeType, -1);
    		this.contentImage = image;
    		this.thumbnailImage = toThumbnailImage(image);
    	}
    	
    	@Override
		protected boolean isContentAvailable() {
    		return contentURL!=null || contentImage!=null;
    	}
    	
    	@Override
        protected void ensureContentAvailable() {
        	if (!isContentAvailable()) { 
        		try {
        			contentURL = createWadoURL(getMimeType(),-1);
        		}
        		catch (Exception e) {
        			log.error("Unable to get image document WADO URL!", e);
        		}
        		try {
					Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("dicom");
					if (readers!=null && readers.hasNext()) {
		        		List<org.dcm4chee.archive.entity.File> files = ((TCQueryLocal) JNDIUtils.lookup(
		                		TCQueryLocal.JNDI_NAME)).findInstanceByUID(
		                		getSOPInstanceUID()).getFiles();
		                String fsId = files.get(0).getFileSystem().getDirectoryPath();
		                String fileId = files.get(0).getFilePath();
		            
		                ImageReader reader = readers.next();
						reader.setInput(ImageIO.createImageInputStream(fsId.startsWith("tar:") ? 
		            			TarRetrieveDelegate.getInstance().retrieveFileFromTar(fsId, fileId) :
		            				FileUtils.resolve(new File(fsId, fileId))), true);
						contentImage = reader.read(0);
						thumbnailImage = toThumbnailImage(contentImage);
					}
        		}
				catch (Exception e) {
					log.error("Unable to convert encapsulated image to image!", e);
				}
        	}
        }

		@Override
        public Resource getDocumentThumbnail() {
			Resource res = toImageResource(thumbnailImage, false);
			if (res!=null) {
				return res;
			}
			else {
				try {
					final IResourceStream stream =  new UrlResourceStream(
							createWadoURL(null,64));
	    			return new WebResource() {
	    				@Override
	    				public IResourceStream getResourceStream() {
	   						return stream;
	    				}
	    			};
				}
				catch (Exception e) {
					log.error("Creating WADO URL failed!", e); //$NON-NLS-1$
				}
			
				ImageManager.IMAGE_TC_IMAGE.bind(Application.get());
				return ImageManager.IMAGE_TC_IMAGE.getResource();
			}
        }

		@Override
    	public Resource getDocumentContent(final boolean forDownloading) {
    		ensureContentAvailable();
    		if (isContentAvailable()) {
    			try {    				
    				Resource res = toImageResource(contentImage, forDownloading);
    				if (res!=null) {
    					return res;
    				}
    				else {
		    			return new WebResource() {
		    				@Override
		    				public IResourceStream getResourceStream() {
		   						return new UrlResourceStream(contentURL);
		    				}
		    				@Override
		    				protected void setHeaders(WebResponse response)
		    				{
		    					super.setHeaders(response);
		    					
		    					String filename = getDocumentFileName();
		    					if (forDownloading) {
		    						response.setAttachmentHeader(filename);
		    					}
		    					else {
		    						response.setHeader("Content-Disposition", "inline" +
		    								((!Strings.isEmpty(filename)) ? ("; filename=\"" + filename + "\"") : ""));
		    					}
		    				}
		    			};
    				}
    			}
    			catch (Exception e) {
    				log.error("Unable to get document content!", e);
    			}
    		}
    		return null;
    	}
		
		@Override
		public DicomObject toDataset() throws Exception {
			DicomObject attrs = super.toDataset();
			if (contentImage!=null) {
				toDicom(contentImage).copyTo(attrs);
			}
            return attrs;
		}
    	
    	public static TCImageDocument create(TCReferencedInstance ref) throws Exception {
    		DicomObject attrs = ((TCQueryLocal) JNDIUtils.lookup(
            		TCQueryLocal.JNDI_NAME)).findInstanceByUID(
            		ref.getInstanceUID()).getAttributes(false);
            attrs.remove(Tag.PixelData);
            
    		return new TCImageDocument(checkMimeType(getDocumentMimeTypeFromLabel(
                			attrs.getString(Tag.ContentLabel))), ref, attrs);
    	}
    	
    	@SuppressWarnings("restriction")
		public static TCImageDocument create(TCObject tc, MimeType mimeType, String filename, InputStream in, 
    			String description) throws Exception {
    		
    		mimeType = checkMimeType(mimeType);

            ByteArrayOutputStream out = null;
            
            try {
            	BufferedImage image = null;
            	
            	if (MimeType.ImageJPEG.equals(mimeType)) {
            		try {
	            		com.sun.image.codec.jpeg.JPEGImageDecoder decoder = 
	            				com.sun.image.codec.jpeg.JPEGCodec.createJPEGDecoder(in);
	            		image = decoder.decodeAsBufferedImage();
            		}
            		catch (Exception e) {
            			log.warn(null, e);
            		}
            	}
            	
            	if (image==null) {
            		image = ImageIO.read(in);
            	}

        		TCReferencedInstance ref = createReferencedInstance(
        				tc, UID.MultiFrameTrueColorSecondaryCaptureImageStorage, MODALITY);
                
        		DicomObject metaData = TCDocumentObject.createDicomMetaData(mimeType,
        				filename, description,  ref, 
        				tc.getPatientId(), tc.getPatientIdIssuer(), tc.getPatientName(), 
        				MODALITY);
        		
                metaData.putInt( Tag.NumberOfFrames, VR.IS, 1 );
                
                return new TCImageDocument(
                		mimeType, ref, metaData, image);
            }
            finally {
            	if (in!=null) {
            		try {
            			in.close();
            		}
            		catch (Exception e) {
            		}
            	}
            	if (out!=null) {
            		try {
            			out.close();
            		}
            		catch (Exception e) {
            		}
            	}
            }
    	}
    	
    	private BufferedImage toThumbnailImage(BufferedImage contentImage) {
    		if (contentImage!=null) {
    			final int thumbnailSize = 64;
    			int width = contentImage.getWidth();
    			int height = contentImage.getHeight();
    			int maxSize = Math.max(width, height);
    			if (maxSize<=thumbnailSize) {
    				return contentImage;
    			}
    			else {
    				double scale = (double) thumbnailSize / (double) maxSize;
    				int scaledWidth = (int) (width * scale);
    				int scaledHeight = (int) (height * scale);
    				
    				Image thumbnailImage = contentImage.getScaledInstance(
    						scaledWidth, scaledHeight, Image.SCALE_DEFAULT);
    				
    				if (thumbnailImage instanceof BufferedImage) {
    					return (BufferedImage) thumbnailImage;
    				}
    				else {
    					BufferedImage image = new BufferedImage(scaledWidth, scaledHeight,
    							BufferedImage.TYPE_INT_RGB);
    					image.getGraphics().drawImage(thumbnailImage, 0, 0, null);
    					return image;
    				}
    			}

    		}
    		return null;
    	}
    	
    	private Resource toImageResource(BufferedImage image, final boolean forDownloading) {
			if (image!=null) {
				MimeType mimeType = getMimeType();
				BufferedDynamicImageResource res = new BufferedDynamicImageResource() {
    				@Override
    				protected void setHeaders(WebResponse response)
    				{
    					super.setHeaders(response);
    					
    					String filename = getDocumentFileName();
    					if (forDownloading) {
    						response.setAttachmentHeader(filename);
    					}
    					else {
    						response.setHeader("Content-Disposition", "inline" +
    								((!Strings.isEmpty(filename)) ? ("; filename=\"" + filename + "\"") : ""));
    					}
    				}
				};
				
				if (MimeType.ImageJPEG.equals(mimeType)) {
					res.setFormat("jpeg");
				}
				else if (MimeType.ImagePNG.equals(mimeType)) {
					res.setFormat("png");
				}
				else if (MimeType.ImageGIF.equals(mimeType)) {
					res.setFormat("gif");
				}
				else if (MimeType.ImageBMP.equals(mimeType)) {
					res.setFormat("bmp");
				}
				
				res.setImage(image);
				
				return res;
			}
			return null;
    	}
    	
    	private static MimeType checkMimeType(MimeType mimeType) throws Exception {
    		if (mimeType==null || !DOC_TYPES.contains(mimeType.getDocumentType())) {
    			throw new Exception("Mime-Type is not supported ("+mimeType+")");
    		}
    		
    		// if there's an ImageIO Dicom reader plugin available, we are able
    		// to read/restore all mime types (jpeg, png, gif, bmp). If not,
    		// we need to use the WADO service to convert dicom->image. However
    		// the WADO service just supports dicom->jpeg and in newer version
    		// dicom->png conversions.
    		if (!dicomReaderAvailable) {
    			if (!MimeType.ImagePNG.equals(mimeType)) {
    				mimeType = MimeType.ImageJPEG;
    			}
    		}
    		return mimeType;
    	}
    	
        private static DicomObject toDicom( BufferedImage image ) throws Exception
        {
            int numSamples = image.getColorModel().getNumComponents();
            if (numSamples == 4) {
            	numSamples = 3;
            }
            String pmi = numSamples>1 ? "RGB" : "MONOCHROME2";
            int bits = image.getColorModel().getComponentSize(0);
            int allocated = 8;
            if (bits > 8) {
            	allocated = 16;
            }
            
            DicomObject attrs = new BasicDicomObject();
            attrs.putString( Tag.TransferSyntaxUID, VR.UI, UID.ImplicitVRLittleEndian );
            attrs.putInt( Tag.Rows, VR.US, image.getHeight() );
            attrs.putInt( Tag.Columns, VR.US, image.getWidth() );
            attrs.putInt(Tag.SamplesPerPixel, VR.US, numSamples);
            attrs.putInt(Tag.BitsStored, VR.US, bits);
            attrs.putInt(Tag.BitsAllocated, VR.US, allocated);
            attrs.putInt(Tag.PixelRepresentation, VR.US, 0);
            attrs.putString(Tag.PhotometricInterpretation, VR.CS, pmi);

            if (numSamples>1) {
            	attrs.putInt(Tag.PlanarConfiguration, VR.US, 0);
            }
            
            int biType = numSamples==3 ? BufferedImage.TYPE_INT_RGB :
            	allocated>8 ? BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY;
            
            BufferedImage tmpImage = image;
            if (image.getType()!=biType) {
            	tmpImage = new BufferedImage(image.getWidth(), image.getHeight(), biType);
            	tmpImage.getGraphics().drawImage(image, 0, 0, null);
            }
            
            byte[] pixelData = null;
            DataBuffer dataBuffer = tmpImage.getRaster().getDataBuffer();
    		if (dataBuffer instanceof DataBufferInt) {
				final int[] data = (int[]) ((DataBufferInt)dataBuffer).getData();
				pixelData = new byte[data.length*3];
				int index = 0;
				for (final int i : data)
				{
				    pixelData[index++] = (byte)((i >>>16) & 0xFF); 
				    pixelData[index++] = (byte)((i >>>8 ) & 0xFF);
				    pixelData[index++] = (byte)(i & 0xFF);							
				}
        	}
    		else if (dataBuffer instanceof DataBufferUShort) {
				final short[] data = (short[]) ((DataBufferUShort)dataBuffer).getData();
				pixelData = new byte[data.length*2];
				int index = 0;
				for (final int i : data)
				{
				    pixelData[index++] = (byte)((i >>>8 ) & 0xFF);
				    pixelData[index++] = (byte)(i & 0xFF);							
				}
        	}
    		else if (dataBuffer instanceof DataBufferByte) {
    			pixelData = ((DataBufferByte)dataBuffer).getData();
    		}
            
           	attrs.putBytes(Tag.PixelData, allocated>8 ? 
           			VR.OW : VR.OB, pixelData);

            return attrs;
        }
                
        private URL createWadoURL(MimeType mimeType, int size) {
        	StringBuilder sbuilder = new StringBuilder();
        	sbuilder.append(WADODelegate.getInstance().getWadoBaseUrl());
        	sbuilder.append("&studyUID=").append(getStudyInstanceUID());
        	sbuilder.append("&seriesUID=").append(getSeriesInstanceUID());
        	sbuilder.append("&objectUID=").append(getSOPInstanceUID());
        	if (mimeType!=null && MimeType.ImagePNG.equals(mimeType)) {
        		sbuilder.append("&contentType=").append(
        				mimeType.getMimeTypeString());
        	}
        	if (size>0) {
        		sbuilder.append("&rows=").append(size);
        	}
        	
        	try {
        		return new URL(sbuilder.toString());
        	}
        	catch (Exception e) {
        		log.error("Malformed WADO URL!", e);
        		return null;
        	}
        }
    }
    
    /*
     * GENERAL ENCAPSULATED DOCUMENT
     */
    private static class TCEncapsulatedDocument extends TCDocumentObject {
    	public static final String MODALITY = "OT"; //$NON-NLS-1$
    	public static final List<DocumentType> DOC_TYPES = Arrays.asList(
    			new DocumentType[] {DocumentType.ZIP,
    					DocumentType.PDF, DocumentType.MSWORD, DocumentType.MSPOWERPOINT,
    					DocumentType.MSEXCEL, DocumentType.TEXT, DocumentType.CSV});
    	
    	private File dicomFile;
    	private transient byte[] data;
    	
    	private TCEncapsulatedDocument(MimeType mimeType, 
    			TCReferencedInstance ref, DicomObject o, File dicomFile) {
    		super(mimeType, ref, o);
    		this.dicomFile = dicomFile;
    	}
    	
    	private TCEncapsulatedDocument(MimeType mimeType, 
    			TCReferencedInstance ref, DicomObject o, byte[] data) {
    		super(mimeType, ref, o);
    		this.data = data;
    	}
    	    	
    	@Override
		protected boolean isContentAvailable() {
    		return dicomFile!=null || data!=null;
    	}
    	
    	@Override
        protected void ensureContentAvailable() {
        	if (!isContentAvailable()) {            
                try {
            		List<org.dcm4chee.archive.entity.File> files = ((TCQueryLocal) JNDIUtils.lookup(
                    		TCQueryLocal.JNDI_NAME)).findInstanceByUID(
                    		getSOPInstanceUID()).getFiles();
                    String fsId = files.get(0).getFileSystem().getDirectoryPath();
                    String fileId = files.get(0).getFilePath();
                	dicomFile = fsId.startsWith("tar:") ? 
                			TarRetrieveDelegate.getInstance().retrieveFileFromTar(fsId, fileId) :
                				FileUtils.resolve(new File(fsId, fileId));
                }
                catch (Exception e) {
                	log.error("Unable to retrieve encapsulated document file!", e); //$NON-NLS-1$
                }
        	}
        }
    	
    	@Override
    	public DicomObject toDataset() throws Exception {
    		DicomObject attrs = super.toDataset();
            attrs.putBytes( Tag.EncapsulatedDocument, VR.OB, 
            		getDocumentContent());
            return attrs;
    	}
        
    	@Override
        public Resource getDocumentThumbnail() {
    		DocumentType docType = getMimeType().getDocumentType();
    		if (DocumentType.PDF.equals(docType)) {
    			ImageManager.IMAGE_TC_PDF.bind(Application.get());
    			return ImageManager.IMAGE_TC_PDF.getResource();
    		}
    		else if (DocumentType.ZIP.equals(docType)) {
    			ImageManager.IMAGE_TC_ZIP.bind(Application.get());
    			return ImageManager.IMAGE_TC_ZIP.getResource();
    		}
    		else if (DocumentType.MSEXCEL.equals(docType)) {
    			ImageManager.IMAGE_TC_EXCEL.bind(Application.get());
    			return ImageManager.IMAGE_TC_EXCEL.getResource();
    		}
    		else if (DocumentType.MSPOWERPOINT.equals(docType)) {
    			ImageManager.IMAGE_TC_POWERPOINT.bind(Application.get());
    			return ImageManager.IMAGE_TC_POWERPOINT.getResource();
    		}
    		else if (DocumentType.MSWORD.equals(docType)) {
    			ImageManager.IMAGE_TC_WORD.bind(Application.get());
    			return ImageManager.IMAGE_TC_WORD.getResource();
    		}
    		else if (DocumentType.TEXT.equals(docType)) {
    			ImageManager.IMAGE_TC_TEXT.bind(Application.get());
    			return ImageManager.IMAGE_TC_TEXT.getResource();
    		}
    		else if (DocumentType.CSV.equals(docType)) {
    			ImageManager.IMAGE_TC_TEXT.bind(Application.get());
    			return ImageManager.IMAGE_TC_TEXT.getResource();
    		}
    		return null;
        }
    	    	
    	@Override
    	public Resource getDocumentContent(final boolean forDownloading) {
    		ensureContentAvailable();
    		if (isContentAvailable()) {
    			return new DynamicWebResource() {
    				@Override
    				protected void setHeaders(WebResponse response) {
    					super.setHeaders(response);
    					
    					String filename = getDocumentFileName();
    					if (forDownloading) {
    						response.setAttachmentHeader(filename);
    					}
    					else {
    						response.setHeader("Content-Disposition", "inline" +
    								((!Strings.isEmpty(filename)) ? ("; filename=\"" + filename + "\"") : ""));
    					}
    				}
    				@Override
    				protected ResourceState getResourceState() {
    					return new ResourceState() {
    						@Override
    						public String getContentType() {
    							return getMimeType().getMimeTypeString();
    						}
    						@Override
    						public Time lastModifiedTime()
    						{
    							return Time.valueOf(
    									getDocumentAddedDate());
    						}
    						@Override
    						public byte[] getData() {
    							if (data!=null) {
    								return data;
    							}
    							else {
	    							try {
		    	    	            	return getDocumentContent();
	    							}
	    							catch (Exception e) {
	    								log.error("Unable to read encapsulated document content!", e);
	    								return null;
	    							}
    							}
    						}
    					};
    				}
    			};
    		}
    		return null;
    	}
    	
    	public static TCEncapsulatedDocument create(TCReferencedInstance ref) throws Exception {
            Instance instance = ((TCQueryLocal) JNDIUtils.lookup(
            		TCQueryLocal.JNDI_NAME)).findInstanceByUID(
            		ref.getInstanceUID());
            
            List<org.dcm4chee.archive.entity.File> files = instance.getFiles();
    		DicomObject attrs = instance.getAttributes(false);
            
            attrs.remove(Tag.EncapsulatedDocument);
            
            MimeType mimeType = null;
            String mimeTypeStr = attrs.getString(Tag.MIMETypeOfEncapsulatedDocument);
            if (mimeTypeStr!=null) {
            	mimeType = MimeType.get(mimeTypeStr);
            }
            if (mimeType==null) {
            	mimeType = getDocumentMimeTypeFromLabel(
            			attrs.getString(Tag.ContentLabel));
            }
                		
            String fsId = files.get(0).getFileSystem().getDirectoryPath();
            String fileId = files.get(0).getFilePath();
        	File file = fsId.startsWith("tar:") ? 
        			TarRetrieveDelegate.getInstance().retrieveFileFromTar(fsId, fileId) :
        				FileUtils.resolve(new File(fsId, fileId));
            
    		return new TCEncapsulatedDocument(
    				mimeType ,ref, attrs, file);
    	}
    	
    	public static TCEncapsulatedDocument create(TCObject tc, MimeType mimeType, String filename, InputStream in, 
    			String description) throws Exception {

    		if (mimeType==null || !DOC_TYPES.contains(mimeType.getDocumentType())) {
    			throw new Exception("Mime-Type is not supported ("+mimeType+")");
    		}
    		            
    		TCReferencedInstance ref = createReferencedInstance(
    				tc, UID.EncapsulatedPDFStorage, MODALITY);
    		
    		DicomObject metaData = TCDocumentObject.createDicomMetaData(mimeType,    				
    				filename, description,  ref, 
    				tc.getPatientId(), tc.getPatientIdIssuer(), tc.getPatientName(), 
    				MODALITY);
            metaData.putString( Tag.TransferSyntaxUID, VR.UI, UID.ExplicitVRLittleEndian );
            metaData.putString( Tag.DocumentTitle, VR.ST, description!=null && !description.isEmpty() ?
            		description : getFileName(filename) );
            metaData.putString( Tag.MIMETypeOfEncapsulatedDocument, VR.LO, mimeType.getMimeTypeString());
            metaData.putString( Tag.BurnedInAnnotation, VR.CS, "NO"); //$NON-NLS-1$
            metaData.putSequence( Tag.ConceptNameCodeSequence, 0 );

            
            try {
	            return new TCEncapsulatedDocument(
	            		mimeType, ref, metaData, IOUtils.toByteArray(in));
            }
            finally {
            	if (in!=null) {
           			in.close();
            	}
            }	
    	}
    	
    	private byte[] getDocumentContent() {
    		if (data!=null) {
    			return data;
    		}
    		else if (dicomFile!=null) {
    			DicomInputStream dis = null;
    			try {
    				dis = new DicomInputStream(dicomFile);
    				return dis.readDicomObject().getBytes(
	            		Tag.EncapsulatedDocument);
    			}
    			catch (Exception e) {
    				log.error("Unable to get content of encapsulated document!", e);
    			}
    			finally {
    				if (dis!=null) {
    					try {
    						dis.close();
    					}
    					catch (Exception e) {
    						log.error(null, e);
    					}
    				}
    			}
    		}
    		
    		return null;
    	}
    }
    
    /*
     * SUPPORTED MIME-TYPES
     */
    public static enum MimeType {
    	ApplicationZIP2(DocumentType.ZIP, 
    			"application/x-zip-compressed", "zip"),
    	ApplicationZIP(DocumentType.ZIP, 
    			"application/zip", "zip"),
    	ApplicationPDF(DocumentType.PDF, 
    			"application/pdf", "pdf"),
    	ApplicationMSWord2(DocumentType.MSWORD, 
    	    	"application/vnd.ms-word", "doc", "dot"),		
    	ApplicationMSWord(DocumentType.MSWORD, 
    			"application/msword", "doc", "dot"),
    	ApplicationMSWordXDoc(DocumentType.MSWORD, 
    			"application/vnd.openxmlformats-officedocument.wordprocessingml.document","docx"),
    	ApplicationMSWordXTemp(DocumentType.MSWORD, 
    			"application/vnd.openxmlformats-officedocument.wordprocessingml.template","dotx"),
    	ApplicationMSWordXTemp2(DocumentType.MSWORD, 
    	    	"application/vnd.ms-word.template.12","dotx"),
    	ApplicationMSPowerpoint2(DocumentType.MSPOWERPOINT, 
    	        "application/vnd.ms-powerpoint", "ppt", "pps", "pot"),	
    	ApplicationMSPowerpoint(DocumentType.MSPOWERPOINT, 
    			"application/mspowerpoint", "ppt", "pps", "pot"),
    	ApplicationMSPowerpointXDoc(DocumentType.MSPOWERPOINT, 
    			"application/vnd.openxmlformats-officedocument.presentationml.presentation","pptx"),
    	ApplicationMSPowerpointXTemp(DocumentType.MSPOWERPOINT, 
    			"application/vnd.openxmlformats-officedocument.presentationml.template","potx"),
    	ApplicationMSPowerpointXTemp2(DocumentType.MSPOWERPOINT, 
    			"application/vnd.ms-powerpoint.template.12","potx"),
    	ApplicationMSPowerpointXShow(DocumentType.MSPOWERPOINT, 
    			"application/vnd.openxmlformats-officedocument.presentationml.slideshow","ppsx"),
    	ApplicationMSExcel2(DocumentType.MSEXCEL, 
    	        "application/vnd.ms-excel", "xls", "xla", "xlt"),       
    	ApplicationMSExcel(DocumentType.MSEXCEL,
        		"application/msexcel", "xls", "xla", "xlt"),
        ApplicationMSExcelXDoc(DocumentType.MSEXCEL, 
        		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx"),
        ApplicationMSExcelXTemp(DocumentType.MSEXCEL, 
        		"application/vnd.openxmlformats-officedocument.spreadsheetml.template","xltx"),
        ApplicationRTF(DocumentType.TEXT, 
        		"application/rtf", "rtf"),
        TextRTF(DocumentType.TEXT, 
        		"text/rtf", "rtf"),
    	TextCSV(DocumentType.CSV, 
    			"text/csv", "csv"),
    	TextCSV2(DocumentType.CSV, 
    			"text/comma-separated-values", "csv"),
    	TextPlain(DocumentType.TEXT, 
    			"text/plain", "txt"),
    	ImageJPEG(DocumentType.JPEG, 
    			"image/jpeg", "jpg", "jpeg", "jpe"),
    	ImageBMP(DocumentType.BMP, 
    			"image/bmp", "bmp"),
    	ImageGIF(DocumentType.GIF, 
    			"image/gif", "gif"),
    	ImagePNG(DocumentType.PNG, 
    			"image/png", "png");
    	
    	private DocumentType docType;
    	private String mimeType;
    	private String[] fileExtensions;
    	
    	private MimeType(DocumentType docType, String mimeType, String...fileExtensions) {
    		this.docType = docType;
    		this.mimeType = mimeType;
    		this.fileExtensions = fileExtensions;
    	}
    	
    	public DocumentType getDocumentType() {
    		return docType;
    	}
    	
    	public String getMimeTypeString() {
    		return mimeType;
    	}

    	public String[] getSupportedFileExtensions() {
    		return fileExtensions;
    	}
    	
    	public boolean isFileExtensionSupported(String fileExtension) {
    		if (fileExtensions!=null) {
    			for (String ext : fileExtensions) {
    				if (fileExtension.equalsIgnoreCase(ext)) {
    					return true;
    				}
    			}
    		}
    		return false;
    	}
    	
    	public static MimeType get(String mimeType) {
    		for (MimeType type : values()) {
    			if (type.getMimeTypeString().startsWith(mimeType)) {
    				return type;
    			}
    		}
    		return null;
    	}
    	
    	public static MimeType guessFromFileExtension(String fileExtension) {
    		for (MimeType type : values()) {
    			if (type.isFileExtensionSupported(fileExtension)) {
    				return type;
    			}
    		}
    		return null;
    	}
    }
    
    public static enum DocumentType {
    	ZIP(MimeType.ApplicationZIP),
    	PDF(MimeType.ApplicationPDF),
    	JPEG(MimeType.ImageJPEG),
    	PNG(MimeType.ImagePNG),
    	BMP(MimeType.ImageBMP),
    	GIF(MimeType.ImageGIF),
    	TEXT(MimeType.ApplicationRTF, MimeType.TextRTF, MimeType.TextPlain),
    	CSV(MimeType.TextCSV, MimeType.TextCSV2),
    	MSWORD(MimeType.ApplicationMSWord, MimeType.ApplicationMSWord2, 
    			MimeType.ApplicationMSWordXDoc, MimeType.ApplicationMSWordXTemp),
    	MSEXCEL(MimeType.ApplicationMSExcel, MimeType.ApplicationMSExcel2,
    			MimeType.ApplicationMSExcelXDoc, MimeType.ApplicationMSExcelXTemp),
    	MSPOWERPOINT(MimeType.ApplicationMSPowerpoint, MimeType.ApplicationMSPowerpoint2,
    			MimeType.ApplicationMSPowerpointXDoc, MimeType.ApplicationMSPowerpointXShow,
    			MimeType.ApplicationMSPowerpointXTemp);

    	private MimeType[] mimeTypes;
    	private String humanReadableName;
    	
    	private DocumentType(MimeType...mimeTypes) {
    		this.mimeTypes = mimeTypes;
    		this.humanReadableName = TCUtilities.getLocalizedString(
    				"tc.documents.type."+name().toLowerCase()+".text");
    	}
    	
    	public MimeType[] getMimeTypes() {
    		return mimeTypes;
    	}
    	
    	public String getHumanReadableName() {
    		return humanReadableName;
    	}
    }
}
