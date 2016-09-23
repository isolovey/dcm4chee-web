/*
 * Sets the position of an element (i.e popup) relative to another element (i.e parent)
 * For both, you need to specify the points that should be aligned. 
 * Further you are able to specify an additional offset that is going 
 * to be added to the calculated position.
 */
function setPositionRelativeToParent(parentId, elementId, parentAlign, elementAlign, offsetX, offsetY)
{
	var options = {
		of: $("[id='"+parentId+"']"),
		my: elementAlign,
		at: parentAlign,
		offset: offsetX + ' ' + offsetY,
		collision: 'fit fit'
	};
	
	$("[id='"+elementId+"']").position(options);
}

function selectTC(elementId, cssClassSelected, cssClassEvenUnselected, cssClassOddUnselected)
{
	$(".tc-content-table tr").each(function(index) {
		itemId = $(this).attr('id');
		
		if (itemId)
		{
			$(this).removeClass();
			$(this).removeAttr('selected');
			
			if (elementId==itemId)
			{
				$(this).addClass(cssClassSelected);
				$(this).attr('selected','selected');
			}
			else if (index%2==0)
			{
				$(this).addClass(cssClassEvenUnselected);
			}
			else
			{
				$(this).addClass(cssClassOddUnselected);
			}
		}
	});
}

function setDisabledTCDetailsTabs(indices)
{
	$('.details-tabs').tabs('option','disabled',false);
	$('.details-tabs').tabs('option','disabled',indices);
}

function setHiddenTCDetailsTabs(indices)
{
	$('.details-tabs .ui-tabs-nav li').each(function(i) {
		if ($.inArray(i,indices)>=0) {
			$(this).css('display','none');
		}
		else {
			$(this).css('display','');
		}
	});
}

function setPopupResizeable(popupId)
{
	$("[id='"+popupId+"']").resizable();
}

function hidePopup(popupId)
{
	hidePopup(popupId, null);
}

function hidePopup(popupId, hideOnOutsideClickCallbackUrl)
{	
	$("[id='" + popupId + "']").fadeOut('slow', function() {
		$("[id='" + popupId + "']").css({'left':'0','top':'0'});
	});
}

function showPopup(popupId)
{
	showPopup(popupId, null);
}

function showPopup(popupId, hideOnOutsideClickCallbackUrl)
{
	if (hideOnOutsideClickCallbackUrl!=null)	
	{		
		$(document).on('click.tc-popup', {url:hideOnOutsideClickCallbackUrl}, handlePopupOutsideClick);
	}
	
	$("[id='" + popupId + "']").fadeIn('slow');
}

function isPopupShown(popupId)
{
	return $("[id='"+popupId+"']").css('display')!='none';
}

function handlePopupOutsideClick(event)
{
	if (shouldHandlePopupOutsideClick(event))
	{
		wicketAjaxGet(event.data.url,function(){},function(){});
	}
}

function shouldHandlePopupOutsideClick(event)
{
	var popupShown = false;
	var mouseOverPopup = false;
	$('.tc-popup').each(function() {
		var popupId = $(this).attr('id');
		if (popupId!=null) {
			if (isPopupShown(popupId)) {
				popupShown = true;
				
				var popupPosition = $(this).offset();
				var popupWidth = $(this).width();
				var popupHeight = $(this).height();

				if (popupPosition!=null && 
						popupWidth!=null && popupHeight!=null)
				{
					if (event.pageX>popupPosition.left-1 && event.pageX<popupPosition.left+popupWidth+1 &&
						event.pageY>popupPosition.top-1 && event.pageY<popupPosition.top+popupHeight+1)
					{
						mouseOverPopup=true;
						return false;
					}
				}
			}
		}
	});
	
	return popupShown && !mouseOverPopup;
}

function shouldHandlePopupMouseOut(event, popupId)
{
	var popupPosition = null;
	var popupWidth = null;
	var popupHeight = null;
	var popup = $("[id='"+popupId+"']");
	
	popup.each(function() {
		popupPosition = $(this).offset();
		popupWidth = $(this).width();
		popupHeight = $(this).height();
	});

	if (popupId!=null &&
			popupPosition!=null && 
			popupWidth!=null && popupHeight!=null)
	{
		if (event.pageX<popupPosition.left-1 || event.pageX>popupPosition.left+popupWidth+5 ||
			event.pageY<popupPosition.top-1 || event.pageY>popupPosition.top+popupHeight+20)
		{
			if (!popup.hasClass('ui-resizable-resizing'))
			{
				return true;
			}
		}
	}
	
	return false;
}

Mask = { };

/**
Shows a mask that blocks user input
*/
Mask.show = function(parentId, showWaitCursor)
{
	Mask.showImpl(parentId, false, showWaitCursor);
};

/**
Shows a transparent mask that blocks user input
*/
Mask.showTransparent = function(parentId, showWaitCursor)
{
	Mask.showImpl(parentId, true, showWaitCursor);
};

/**
 Shows a mask
*/
Mask.showImpl = function(parentId, showTransparentMask, showWaitCursor)
{
	var target=null;

	if (parentId==null)
	{
		target = document.getElementsByTagName("body")[0];
	}
	else
	{
		target = document.getElementById(parentId);
	}

	var mask=document.createElement("div");
	mask.id="wicket_mask_";
	mask.innerHTML="&nbsp;";
	mask.style.position="absolute";
	mask.style.top="0";
	mask.style.left="0";
	mask.style.width="100%";
	mask.style.height="100%";
	mask.style.zIndex="21000"; // wicket mask for modal dialogs has zIndex=20000

	if (showTransparentMask)
	{
		if (showWaitCursor)
		{
			mask.className="wicket-mask-transparent wicket-mask-cursor";
		}
		else
		{
			mask.className="wicket-mask-transparent";
		}
	}
	else
	{
		if (showWaitCursor)
		{
			mask.className="wicket-mask wicket-mask-cursor";
		}
		else
		{
			mask.className="wicket-mask";
		}
	}

	target.appendChild(mask);

	Mask.offsetMask(mask);
};

/**
 Hides the mask
*/
Mask.hide = function()
{
  var mask=document.getElementById("wicket_mask_");
  if (mask!=null) {
	  mask.style.display="none";
	  mask.parentNode.removeChild(mask);
  }
};

/**
 * Offsets the mask to the scroll position.
 */ 
Mask.offsetMask = function(mask)
{
  var offsetX =   document.body.scrollLeft;
  var offsetY =  document.body.scrollTop;
  
  mask.style.left = offsetX + "px";
  mask.style.top = offsetY + "px";
};
