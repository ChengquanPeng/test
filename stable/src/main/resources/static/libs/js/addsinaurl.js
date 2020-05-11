//https://finance.sina.com.cn/realstock/company/sh600793/nc.shtml
var preUrl = "https://finance.sina.com.cn/realstock/company/";
var endUrl = "/nc.shtml";
var tds = document.getElementsByClassName("aurl");
for (var i = 0; i < tds.length; i++) {
	var u = tds[i].innerText;
	var t = "sz";
	if (u.startsWith("6")) {
		t = "sh";
	}

	tds[i].innerHTML = "<a target='blank' href='" + preUrl + t + u + endUrl
			+ "'>" + u + "</a>";
}
