package org.dcm4chee.dashboard.ui.common;

import org.apache.wicket.Resource;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.image.resource.DynamicImageResource;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebResponse;
import org.jfree.chart.JFreeChart;

public class JFreeChartImage extends NonCachingImage {
        
    private static final long serialVersionUID = 1L;
    
    private int width;
    private int height;

    public JFreeChartImage(String id, JFreeChart chart, int width, int height) {
        super(id, new Model<JFreeChart>(chart));
        this.width = width;
        this.height = height;
    }

    @Override
    protected Resource getImageResource() {
        
        return new DynamicImageResource(){

            private static final long serialVersionUID = 1L;

            @Override
            protected byte[] getImageData() {
                return toImageData(((JFreeChart) getDefaultModelObject()).createBufferedImage(width, height));
            }

            @Override
            protected void setHeaders(WebResponse response) {
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Cache-Control", "no-cache");
                response.setDateHeader("Expires", 0);
            }
        };
    }
}
