var ustr = "";
if (ustr == "") {
	var urls = window.location.href;
	var l = urls.indexOf("/web/");
	ustr = urls.substring(l + 5, l + 5 + 5);
	if (ustr == "comon") {
		ustr = getQueryVariable("ustr");
	}
}
$("#header").load("/web/" + ustr + "/dashboard/header.html");
// $("#menu").load("/web/dashboard/menu.html")

function SetCookie(name, value) {
	var Days = 30 * 12; // cookie 将被保存一年
	var exp = new Date(); // 获得当前时间
	exp.setTime(exp.getTime() + Days * 24 * 60 * 60 * 1000); // 换成毫秒
	document.cookie = name + "=" + escape(value);
}
function getCookie(name) {
	var arr = document.cookie
			.match(new RegExp("(^| )" + name + "=([^;]*)(;|$)"));
	if (arr != null) {
		return unescape(arr[2]);
	} else {
		return null;
	}
}

Date.prototype.format = function(formatStr) {
	var str = formatStr;
	// var Week = ['日','一','二','三','四','五','六'];
	str = str.replace(/yyyy|YYYY/, this.getFullYear());
	str = str.replace(/MM/, (this.getMonth() + 1) > 9 ? (this.getMonth() + 1)
			.toString() : '0' + (this.getMonth() + 1));
	str = str.replace(/dd|DD/, this.getDate() > 9 ? this.getDate().toString()
			: '0' + this.getDate());
	return str;
}
// alert(new Date().format("yyyy-MM-dd"));

function getQueryVariable(variable) {
	var query = window.location.search.substring(1);
	var vars = query.split("&");
	for (var i = 0; i < vars.length; i++) {
		var pair = vars[i].split("=");
		if (pair[0] == variable) {
			var r = pair[1];
			document.title = document.title + " " + r;
			return r;
		}
	}
	return (false);
}

function getDfCode(code) {
	var q = code.substring(0, 1);
	if (q == '6') {
		return "SH";
	}
	return "SZ";
}
