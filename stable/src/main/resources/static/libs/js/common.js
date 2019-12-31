(function() {
	'use strict'

	if (navigator.userAgent.match(/IEMobile\/10\.0/)) {
		var msViewportStyle = document.createElement('style')
		msViewportStyle.appendChild(document
				.createTextNode('@-ms-viewport{width:auto!important}'))
		document.head.appendChild(msViewportStyle)
	}
}())

if (typeof String.prototype.startsWith != 'function'){
   String.prototype.startsWith = function (str){
      return this.slice(0, str.length) == str;
   }
}
　　　　  
 //判断当前字符串是否以str结束  
 if (typeof String.prototype.endsWith != 'function') {
   String.prototype.endsWith = function (str){
      return this.slice(-str.length) == str;
   }
}
     
function to_login() {
	window.location = "/login.html";
}

function to_logout() {
	$.ajax({
		url : '/ui/logout',
		type : 'GET', // GET
		async : true, // 或false,是否异步
		data : {},
		timeout : 5000, // 超时时间
		dataType : 'json', // 返回的数据格式：json/xml/html/script/jsonp/text
		success : function(data, textStatus, jqXHR) {
			
		},
		error : function(xhr, textStatus) {
		}
	});
	window.location = "/login.html";
	return false;
}

function excptionHandler(data) {
	if("biz_error_20002"==data.rtnCode){
		to_login();
	}
}
function addActiveHeaderMen(id){
	setTimeout("setHeaderMenuActive('"+id+"')", 800);
}
function setHeaderMenuActive(id){
	$("#"+id).addClass("active");
}
function addActive(id){
	setTimeout("setMenuActive('"+id+"')", 800);
}
function setMenuActive(id){
	$("#"+id).addClass("active");
}
//时间去掉时分秒
function formDate(datestr){
	var newDate=/\d{4}-\d{1,2}-\d{1,2}/g.exec(datestr);
	return newDate;
}

function getQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return unescape(r[2]);
    return null;
}
