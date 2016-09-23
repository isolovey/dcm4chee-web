package org.dcm4chee.web.war.tc.imageview;

import java.io.Serializable;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 30, 2011
 */
public final class TCWadoImageSize implements Serializable
{
    private int width;
    private int height;
    
    public TCWadoImageSize()
    {
        width = -1;
        height = -1;
    }
    
    private TCWadoImageSize(int width, int height)
    {
        this.width = width>0?width:-1;
        this.height = height>0?height:-1;
    }

    public static TCWadoImageSize createSizeInstance(int width, int height)
    {
        return new TCWadoImageSize(width, height);
    }
    
    public static TCWadoImageSize createWidthInstance(int width)
    {
        return new TCWadoImageSize(width, -1);
    }
    
    public static TCWadoImageSize createHeightInstance(int height)
    {
        return new TCWadoImageSize(-1, height);
    }
    
    public int getWidth()
    {
        return width;
    }
    
    public int getHeight()
    {
        return height;
    }
}