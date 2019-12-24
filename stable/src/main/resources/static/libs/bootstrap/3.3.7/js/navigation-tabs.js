$(document).ready(function(){
	$(".nav-tabs > li").click(function(event){
		$(".nav-tabs > li").removeClass("active");
		$(this).addClass("active");

		var content = $(this).attr("data-content");
		$(".nav-content-container > .nav-content").removeClass("active");
		$(content).addClass("active");
	});
});