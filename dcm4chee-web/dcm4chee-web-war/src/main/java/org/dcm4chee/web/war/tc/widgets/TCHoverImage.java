package org.dcm4chee.web.war.tc.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.resource.LocalizedImageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;


@SuppressWarnings("serial")
public class TCHoverImage extends Image {

	/** The image resource that is rendered when hovered */
	private final LocalizedImageResource hoveredImageResource = new LocalizedImageResource(this);
	
	public TCHoverImage(final String id, final ResourceReference resourceReference,
			final ResourceReference hoveredResourceReference)
	{
		super(id, resourceReference, null);
		
		if (hoveredResourceReference!=null) {
			hoveredImageResource.setResourceReference(hoveredResourceReference);
		}
	}
	
	@Override
	public Component setDefaultModel(IModel<?> model)
	{
		hoveredImageResource.setResourceReference(null);
		hoveredImageResource.setResource(null);
		return super.setDefaultModel(model);
	}
	
	protected ResourceReference getHoveredImageResourceReference()
	{
		return hoveredImageResource.getResourceReference();
	}
	
	@Override
	protected boolean getStatelessHint()
	{
		return false;
	}
	
	@Override
	protected void onComponentTag(final ComponentTag tag)
	{
		super.onComponentTag(tag);
		
		final ResourceReference resourceReference = getHoveredImageResourceReference();
		if (resourceReference != null)
		{
			hoveredImageResource.setResourceReference(resourceReference);
			
			tag.put("hover-img", RequestCycle.get().getOriginalResponse().encodeURL(
					Strings.replaceAll(RequestCycle.get().urlFor(resourceReference), "&", "&amp;")));
		
			tag.put("default-img", tag.getString("src"));
			tag.put("onmouseout", "$(this).attr('src', $(this).attr('default-img'));");
			tag.put("onmouseover", "$(this).attr('src', $(this).attr('hover-img'));");
		}
	}
}
