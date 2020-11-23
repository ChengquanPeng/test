//https://finance.sina.com.cn/realstock/company/sh600793/nc.shtml
var preUrl = "/web/realtime/sortv4sign?code=";
var endUrl = "/nc.shtml";
var tds = document.getElementsByClassName("myrt");
for (var i = 0; i < tds.length; i++) {
	var code = tds[i].innerText;
	tds[i].innerHTML = "<a target='blank' href='" + preUrl + code +"&sg=1>1111</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
	+"<a target='blank' href='" + preUrl + code +"&sg=2>2222</a>";
}
