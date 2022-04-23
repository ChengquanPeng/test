/*!
 * IE10 viewport hack for Surface/desktop Windows 8 bug
 * Copyright 2014-2017 The Bootstrap Authors
 * Copyright 2014-2017 Twitter, Inc.
 * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)
 */

// See the Getting Started docs for more information:
// https://getbootstrap.com/getting-started/#support-ie10-width
function login() {
	$("#divMsg").hide();
	$.ajax({
		url : '/ui/login',
		type : 'POST', // GET
		async : true, // 或false,是否异步
		data : {
			userName : $("#inputEmail").val(),
			password : $("#inputPassword").val()
		},
		timeout : 5000, // 超时时间
		dataType : 'json', // 返回的数据格式：json/xml/html/script/jsonp/text
		success : function(data, textStatus, jqXHR) {
			if("00000000"==data.rtnCode){
				window.location = "/index.html";
			}else{
				if("biz_error_20001"==data.rtnCode){
					$("#divMsg").show();
					$("#divMsgc").html(data.msg);
					$("#inputEmail").val("");
					$("#inputPassword").val("");
				}else{
					$("#divMsg").show();
					$("#divMsgc").html(data.msg);
				}
			}
		},
		error : function(xhr, textStatus) {
			console.log(xhr)
			console.log(textStatus)
			alert("系统异常1");
		}
	});
	return false;
}