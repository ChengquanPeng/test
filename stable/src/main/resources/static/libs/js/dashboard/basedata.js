$("#menu").load("/web/dashboard/basedata/menu.html")
addActiveHeaderMen("base_data");

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
			return pair[1];
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
