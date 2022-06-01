var ustr = "";
if (ustr == "") {
	var urls = window.location.href;
	var l = urls.indexOf("/web/");
	ustr = urls.substring(l + 5, l + 5 + 5);
	if (ustr == "comon") {
		ustr = getQueryVariable("ustr");
	}
}


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

function formatDate(date, fmt) {
	if (/(y+)/.test(fmt)) {
		fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '')
				.substr(4 - RegExp.$1.length))
	}
	let o = {
		'M+' : date.getMonth() + 1,
		'd+' : date.getDate(),
		'h+' : date.getHours(),
		'm+' : date.getMinutes(),
		's+' : date.getSeconds()
	}
	for ( let k in o) {
		if (new RegExp(`(${k})`).test(fmt)) {
			let str = o[k] + ''
			fmt = fmt.replace(RegExp.$1, RegExp.$1.length === 1 ? str
					: padLeftZero(str))
		}
	}
	return fmt
}

function padLeftZero(str) {
	return ('00' + str).substr(str.length);
}

function getlogout() {
	$.ajax({
		url : "/web/logout", // 访问地址--action地址
		type : "get", // 提交方式
		data : {},
		success : function(reData) { // 回调函数的处理方式
			if ("OK" == reData.status) {
				window.location = '/web/login.html';
			} else {
				alert("失败:" + reData.result);
			}
		},
		error : function(reData) { // 回调函数的处理方式
			issend = false;
			alert("登出错误");
		}
	});
}