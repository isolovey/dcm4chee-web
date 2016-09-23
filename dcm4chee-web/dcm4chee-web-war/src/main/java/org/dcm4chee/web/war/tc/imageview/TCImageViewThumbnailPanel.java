package org.dcm4chee.web.war.tc.imageview;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Jan 16, 2012
 */
public class TCImageViewThumbnailPanel extends Panel {

    private static final TCWadoImageSize thumbnailSize = TCWadoImageSize.createSizeInstance(-1, 64);
    
    private IModel<TCImageViewImage> instanceModel = new Model<TCImageViewImage>();

    private RepeatingView list;
    
    public TCImageViewThumbnailPanel(final String id, TCImageViewSeries series)
    {
        super(id);
        
        setOutputMarkupId(true);

        list = new RepeatingView("thumbnail");

        setDefaultModel(new Model<TCImageViewSeries>(series) {
            @Override
            public void setObject(TCImageViewSeries value)
            {
                TCImageViewSeries current = getObject();
                
                super.setObject(value);

                if (isOtherSeries(current, value))
                {
                    initImages(value);
                }
            }
        });
        
        add(list);
    }
    
    public TCImageViewSeries getSeries()
    {
        return (TCImageViewSeries) getDefaultModel().getObject();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean setSeries(TCImageViewSeries series, AjaxRequestTarget target)
    {
        ((Model)getDefaultModel()).setObject(series);
        
        if (getSeries()==series)
        {
        	if (series!=null && series.getImageCount()>0) {
        		instanceModel.setObject(series.getImages().get(0));
        	}
        	else {
        		instanceModel.setObject(null);
        	}
        	
            if (target!=null)
            {
                target.addComponent(this);
                target.appendJavascript(getCheckToLoadThumbnailsJavascript());
                target.appendJavascript(getScrollToImageJavascript());
                target.appendJavascript(getUpdateImageSelectionJavascript());
            }
            
            return true;
        }
        
        return false;
    }
    
    public int getImageCount()
    {
        TCImageViewSeries series = getSeries();
        return series!=null?series.getImageCount():0;
    }
    
    public int getCurrentImageIndex()
    {
        TCImageViewSeries series = getSeries();
        return series!=null ? series.getImages().indexOf(getCurrentImage()):-1;
    }
    
    public int getPreviousImageIndex()
    {
        return getCurrentImageIndex()-1;
    }
    
    public int getNextImageIndex()
    {
        return getCurrentImageIndex()+1;
    }
    
    public TCImageViewImage getPreviousImage()
    {
        TCImageViewSeries series = getSeries();
        if (series!=null)
        {
            int index = getPreviousImageIndex();
            if (index>=0)
            {
                return series.getImages().get(index);
            }
        }
        return null;
    }
    
    public TCImageViewImage getNextImage()
    {
        TCImageViewSeries series = getSeries();
        if (series!=null)
        {
            int index = getNextImageIndex();
            if (index<getImageCount())
            {
                return series.getImages().get(index);
            }
        }
        return null;
    }
    
    public TCImageViewImage getCurrentImage()
    {
        return instanceModel.getObject();
    }
        
    public void setCurrentImage(TCImageViewImage ref, AjaxRequestTarget target)
    {
        instanceModel.setObject(ref);
        
        if (target!=null)
        {
            target.appendJavascript(getScrollToImageJavascript());
            target.appendJavascript(getUpdateImageSelectionJavascript());
        }
    }
    
    public IModel<TCImageViewImage> getCurrentImageModel()
    {
        return instanceModel;
    }
    
    private boolean isOtherSeries(TCImageViewSeries s1, TCImageViewSeries s2)
    {
        return (s1==null && s2!=null) || 
                (s1!=null && s2==null) ||
                (s2!=null && s1!=null && 
                 s1.getSeriesUID()!=s2.getSeriesUID());
    }
    
    private void initImages(TCImageViewSeries series)
    {
        list.removeAll();
        instanceModel.setObject(null);
    
        List<? extends TCImageViewImage> images = series!=null?series.getImages():null;
        if (images!=null && !images.isEmpty())
        {
            instanceModel.setObject(images.get(0));
            
            int i=1;
            for (final TCImageViewImage image : images)
            {
                TCLazyWadoImage img = new TCLazyWadoImage("Thumbnail " + i, 
                            new Model<TCImageViewImage>(image), thumbnailSize, null);
                
                img.setOutputMarkupId(true);
                
                img.add(new AjaxEventBehavior("onclick") {
                    @Override
                    public void onEvent(AjaxRequestTarget target)
                    {
                        if (getCurrentImage()==null ||
                            !getCurrentImage().equals(image))
                        {
                            setCurrentImage(image, target);
                            
                            imageClicked(image, target);
                        }
                    }
                });
                
                if (instanceModel.getObject().equals(image))
                {
                    img.add(new AttributeModifier("class", true, new Model<String>("tc-view-images-thumbnail-selected")) {
                        @Override
                        protected String newValue(final String currentValue, final String replacementValue)
                        {
                            if (currentValue!=null)
                            {
                                return currentValue + " " + replacementValue;
                            }
                            
                            return super.newValue(currentValue, replacementValue);
                        }
                    });
                }
            
                img.add(new AttributeModifier("onmouseover", true, new Model<String>("if (!$(this).hasClass('tc-view-images-thumbnail-selected')) $(this).addClass('tc-view-images-thumbnail-hover')")));
                img.add(new AttributeModifier("onmouseout", true, new Model<String>("$(this).removeClass('tc-view-images-thumbnail-hover')")));
            
                list.add(img);
                
                i++;
            }
        }
    }
        
    protected void imageClicked(TCImageViewImage image, AjaxRequestTarget target) {}
    
    private String getCheckToLoadThumbnailsJavascript()
    {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("checkToLoadThumbnails();");
        return sbuf.toString();
    }
    
    private String getUpdateImageSelectionJavascript()
    {
        StringBuffer sbuf = new StringBuffer();
        
        sbuf.append("$('#tc-view-images-thumbnail-container > img')");
        sbuf.append(".removeClass('tc-view-images-thumbnail-selected tc-view-images-thumbnail-hover');");

        int index = getCurrentImageIndex();
        if (index>=0)
        {
            sbuf.append("$('#tc-view-images-thumbnail-container > img:nth-child(");
            sbuf.append(index+1); //jquery's nth-child() selector is 1 based!!!
            sbuf.append(")').addClass('tc-view-images-thumbnail-selected');");
        }
      
        return sbuf.toString();
    }
        
    private String getScrollToImageJavascript()
    {
        int index = getCurrentImageIndex();
        
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("var thumbnail = $('#").append(getMarkupId(true)).append("').children('img:nth-child(").append(index+1).append(")');\n");
        sbuf.append("checkToScrollToThumbnail(thumbnail);\n");
        return sbuf.toString();
    }
}
