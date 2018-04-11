//Loads the correct sidebar on window load,
//collapses the sidebar on window resize.
// Sets the min-height of #page-wrapper to window size
$(function() {
	$(window).bind("load resize", function() {
		var topOffset = 50;
		var width = (this.window.innerWidth > 0) ? this.window.innerWidth : this.screen.width;
		if(width < 768) {
			$('div.navbar-collapse').addClass('collapse');
			topOffset = 100; // 2-row-menu
		} else {
			$('div.navbar-collapse').removeClass('collapse');
		}

		var height = ((this.window.innerHeight > 0) ? this.window.innerHeight : this.screen.height) - 1;
		height = height - topOffset;
		if(height < 1) height = 1;
		if(height > topOffset) {
			$("#page-wrapper").css("min-height", (height) + "px");
		}
	});

	var url = window.location;
	// var element = $('ul.nav a').filter(function() {
	//     return this.href == url;
	// }).addClass('active').parent().parent().addClass('in').parent();
	var element = $('ul.nav a').filter(function() {
		return this.href == url;
	}).addClass('active').parent();

	while(true) {
		if(element.is('li')) {
			element = element.parent().addClass('in').parent();
		} else {
			break;
		}
	}
});

$(function() {
	initMenu();
});

function initMenu() {
	initMenuTabs();
}

var menuData = null;

function initMenuTabs() {
	$.getJSON("menu.json", function(data) {
		menuData = data;
		$.each(data.tabs, function(index, item) {
			$("#tabs").append('<li><a href="#" data-index=' + index + ' title="' + item.title + '">' + item.title + '</a></li>')
		});

		initMenuTabsEvents();
		initTabMenus(0);
	});
}

function initMenuTabsEvents() {
	$("#tabs li a").click(function() {
		initTabMenus($(this).attr('data-index'));
	});
}

function initTabMenus(tabIndex) {
	var tabData = menuData.tabs[tabIndex];
	var html = '';
	$.each(tabData.pages, function(pageIndex, pageItem) {
		html += '<li>';
		html += '	<a href="#">' + pageItem.title + '<span class="fa arrow"></span></a>';
		html += '	<ul class="nav nav-second-level">';
		$.each(pageItem.menus, function(menuIndex, menuItem) {
			html += '		<li>';
			html += '			<a href="#" data-url="' + menuItem.url + '">' + menuItem.title + '</a>';
			html += '		</li>';
		})
		html += '	</ul>';
		html += '</li>';
	});
	$('#side-menu-content').html(html);

	initTabMenusEvents();
}

function initTabMenusEvents() {
	$('#side-menu-content').metisMenu();
	$('#side-menu-content li a[data-url]').click(function() {
		initTabMenuContent($(this).text(), $(this).attr('data-url'));
	});
}

function initTabMenuContent(title, dataUrl) {
	$.getJSON(dataUrl, function(result) {
		$('#page-wrapper .page-header').text(title);
		$('#request-url').text(result.url);
		$('#request-action').text(result.action);
		$('#request-params').html(initTabMenuContentField(result.request, ""))
		$('#response-params').html(initTabMenuContentField(result.response, ""))
		$('#page-wrapper').removeClass('hidden');
	});
}

function initTabMenuContentField(data, fillStr) {
	var html = '';
	$.each(data, function(i, item) {
		html += '<tr>';
		html += '    <td>' + fillStr + item.name + '</td>';
		html += '    <td>' + item.type + '</td>';
		html += '    <td>' + item.required + '</td>';
		html += '    <td>' + item.desc + '</td>';
		html += '</tr>';

		if(item.type === 'object' || item.type === 'list') {
			html += initTabMenuContentField(item.childs, fillStr + '&emsp;');
		}
	});

	return html;
}