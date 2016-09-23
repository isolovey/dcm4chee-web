function storeScrollPosition() {

	var doc = document.documentElement
	var body = document.body;
	var offsetX = ((doc && doc.scrollLeft) || (body && body.scrollLeft || 0)) - (doc.clientLeft || 0);
	var offsetY = ((doc && doc.scrollTop) || (body && body.scrollTop || 0)) - (doc.clientTop || 0);
	
	document.cookie = "offsetX=" + offsetX + "; path=/";
	document.cookie = "offsetY=" + offsetY + "; path=/";
}

function retrieveScrollX() {
	return getCookie("offsetX");
}

function retrieveScrollY() {
	return getCookie("offsetY");
}

function getCookie(c_name) {
	if (document.cookie.length > 0) {
		c_start = document.cookie.indexOf(c_name + "=");
		if (c_start != -1) {
			c_start = c_start + c_name.length + 1;
			c_end = document.cookie.indexOf(";", c_start);
			if (c_end == -1) {
				c_end = document.cookie.length;
			}
			return unescape(document.cookie.substring(c_start, c_end));
		}
	}
	return "";
}