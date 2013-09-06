

function rightsLinkPopup(){
	var publisher = body_publisher,
	rightsLinkUrl = ("IBM" === publisher) ? 'http://www.research.ibm.com/journal/rdauth.html' : body_rightsLink;  
	//rightsLinkUrl = encodeURI(rightsLinkUrl);    

	window.open(rightsLinkUrl,  'Rightslink', 'location=no,toolbar=no,directories=no,status=no, menubar=no,scrollbars=yes,resizable=yes,width=650,height=550');
	return false;
}
var intCounter=1;
var intHandler;
function setCrossRefNote() {
		intCounter++;
		if(intCounter>120 || j$('#abstractCitations #crossRefNote').length>0){
			window.clearInterval(intHandler);
		}
		if(j$('#abstractCitations #crossRefNote').length>0){
			j$('#abstractCitations #crossRefNote').html("Citations to this article as recorded by CrossRef");
		}
}

function showCitationsButterFly()
{
	var arnumber=j$(this).attr('href');
	Modal.show('/xpl/mwCitations.jsp?arnumber='+arnumber);
	return false;
}

function windowPrint() {
	window.print();
	return false;
}

function writeIndexTermLink(authorTerm) {
	var authorTerm = j$(this).attr("data-keyword");
	var filteredTerm = getFilteredTerms('"' + authorTerm + '"');
	var link = '/search/searchresult.jsp?'
		+ searchPropertiesParamSearchWithin + '=' + searchInterfaceArticleIndexTermReference + ':'
		+ filteredTerm + '& '+ searchPropertiesParamNewSearch +'=partialPref';
	window.location = link;
}

function removeParameter(queryString, paramKey) {
	var start = queryString.indexOf(paramKey + '=');
	if (start > -1) {
		var end = queryString.indexOf('&', start);
		
		// Ignore if & is last character.
		if ((end+1) == queryString.length) {
			end = -1;
		} 
		queryString = (end > start) ? queryString.substring(0, start) + queryString.substring(end+1) : queryString.substring(0, start);
	}
	return queryString;
}

function showAbstract(articleNumber) {
	var oqs = document.getElementById("oqs").value;
	oqs = removeParameter(oqs, 'arnumber');
	oqs = removeParameter(oqs, 'contentType');
	oqs += '&'+ searchPropertiesParamArticleNumber+'='+articleNumber;
	var  url_string = '/xpl/articleDetails.jsp?' + oqs;
	if (url_string.indexOf('&resultAction=ABSTRACT') < 0) {
		url_string += '&resultAction=ABSTRACT';
	}
	//url_string = encodeURI(url_string);
	window.location = url_string;	
}

function BackToSearchresults(articleNumber) {
	var oqs = document.getElementById("oqs").value;
	oqs = removeParameter(oqs, 'arnumber');
	var url_string = '/search/searchresult.jsp?' + oqs;
	url_string += '&resultAction=ABSTRACT';
	//url_string = encodeURI(url_string);
	window.location = url_string;
}

function mapSize(obj)
{
	var L=0;
    j$.each(obj, function(i, elem) {
        L++;
    });
    return L;
}

j$(document).ready(function() {
		
	j$('#action-download-document-citations').find('a').doOnce(function(){
		    this.actionBar({
		      layout : 'vertical',
		      actionId : 'action-download-document-citations'
		    });
		  
	});
	

	
	j$('#action-document-email').find('a').doOnce(function(){
	    this.actionBar({
	      layout : 'vertical',
	      actionId : 'action-document-email'
	    });
	});
	
	j$('#save-to-project').bind('click',function(evt){
		Modal.show(ADD_DOCUMENTS_FORM_URL+"?"+ PARAM_SELECTED_DOCUMENTS +"="+ recordId);
	});
		  
	j$('#purchase-options #qualify-price-ad').bind('click',function(evt){	
		j$("#purchase-options #qualify-price-ad-overlay").css("display","inline");
	     return false;
	});
	j$('#purchase-other-formats #qualify-price-ad').bind('click',function(evt){	
		j$("#purchase-other-formats #qualify-price-ad-overlay").css("display","inline");
	     return false;
	});
	
	j$('#ftm-purchase-other').delegate('#qualify-price-ad-other','click',function(evt){	
		//Do nothing for now , keep it like in production code , commented during Open Access
	    //Development, if required uncomment the line below and qualify-price-ad-overlay need to be 
		//displayed in the other formats purchase tab, if uncommented with out move over lay still comes
		//show in the regular purchase tab.
		//j$("#qualify-price-ad-overlay").css("display","inline");
	     return false;
	});
	
	j$('#article-page-bdy').delegate("#biblyear","click",function(){
			j$.ajax('/xpl/biblMetrics.ajax?'+document.getElementById("oqs").value+"&bbyear="+ j$(this).children(":first-child").text(), {
	  		success: function(data) {
	  			j$('.article-views-container #biblioMetrics-container').html(data);
	  		}
		  });
	 });

	j$('#article-page-bdy').delegate("#articles_crossref_anchor","click",function(){
			var crossRefCount = j$(this).children(".num").html();
			if(crossRefCount=='0'){
			}else{
				j$('#nav-article #abstract-citedby-tab').trigger('click');
				if(j$('#abstractCitations #crossRefNote').length<1){
					intHandler = window.setInterval(setCrossRefNote,1000);
				}else{
					j$('#abstractCitations #crossRefNote').html("Citations to this article as recorded by CrossRef");
				}
			}
		});

/* end best price module */



/** close button for the price ad module */
	//j$('#product-purchase-options').delegate('a.close-module','click',function(evt){
	j$('#purchase-options #qualify-price-ad-overlay').delegate('a.close-module','click',function(evt){
		j$('#purchase-options #qualify-price-ad-overlay').hide();
		
		evt.preventDefault();
		return false;
	});
/** end close button for the price ad module */


	/** close button for the price ad module */
	//j$('#product-purchase-options').delegate('a.close-module','click',function(evt){
	j$('#purchase-other-formats #qualify-price-ad-overlay').delegate('a.close-module','click',function(evt){
		j$('#purchase-other-formats #qualify-price-ad-overlay').hide();
		
		evt.preventDefault();
		return false;
	});
/** end close button for the price ad module */

  
  
  j$("#add-to-cart-button").bind('click',addItemsToCart);
  //j$("#puchase-from-partner-button").bind('click',function(){alert('hello');return false;});
  j$('#ftm-purchase').delegate('#puchase-from-partner-button','click',doPurchaseFromPartner);


  j$('#tabs-main').delegate('#btn-cite-map','click',showCitationsButterFly);
  j$('#article-page-bdy').delegate("#tabs-main #abstractReferences .article-blk .article .docs .links .popup-acm","click",acmPopup);
  j$('#article-actions').delegate(".article-tools .tools .tl-permission a","click",rightsLinkPopup);
  j$('#article-page-bdy').delegate("#tabs-main #abstractKeywords .article-blk .art-keywords a","click",writeIndexTermLink);
  j$('#articles_author_anchor').bind('click',
		  function(){j$('#nav-article #abstract-authors-tab').trigger('click'); });
  
  
  j$.fn.doOnce = function(func) { 
    this.length && func.apply(this); 
    return this; 
  }

  j$('div.tabbed-blk').doOnce(function(){
    this.tabify({
     target_el : 'div.subsection'
    });
  });
  if (isFunction("mc_initMiniCart"))
  {
	  mc_initMiniCart(toggleAddToCart,errorInitMiniCart);
  }


  var $article_page_hdr = j$('#article-page-hdr');
  var $btn_full_txt = j$('#btn-full-txt');
  if ($btn_full_txt.length) {
    var $hdr_btn_set = $article_page_hdr.find('div.button-set');
    var $full_txt_menu = j$('#full-txt-menu-wrap')
    hdr_btn_set_off = Math.floor($hdr_btn_set.offset().top);
    hdr_btn_set_h = $hdr_btn_set.innerHeight();
    $full_txt_menu.css('top', hdr_btn_set_off + hdr_btn_set_h);
    $btn_full_txt.click(function(e) {
      e.stopPropagation();
      $article_page_hdr.toggleClass('menu-open');
      if ($article_page_hdr.hasClass('menu-open')) {
        j$(document).one('click', function() {
          $article_page_hdr.removeClass('menu-open');
        });
      }
    });
    $full_txt_menu.click(function(e) {
      e.stopPropagation();
    });
  }

});

(function(j$){
  j$.fn.tabify = function(options) { 
    defaults = {
      target_el : 'div.section'
    };
    var options = j$.extend(defaults, options);
    return this.each(function() {
      var $this = j$(this);
      var $tabs_nav = $this.find('div.nav-tabs');
      var $tabs = $tabs_nav.find('li');
      var $tab_secs = $this.find(options.target_el);
      //$tabs.eq(0).addClass('active');
      //$tab_secs.hide();
      //$tab_secs.eq(0).show();
      $tabs_nav.delegate('a', 'click', function(e) {
        e.preventDefault();
        var $this_lnk = j$(this);    
        var this_href = $this_lnk.attr('href');
        $tab_secs.hide();
        j$(this_href).show();
        $tabs.removeClass('active');
        $this_lnk.closest('li').addClass('active');
      });
    });
  };
})(jQuery);


// Abstract Tabs (ajax & history AP!)
j$(function() {

  /*
  function formated to work with polyfill:
  history API JavaScript Library v3.2.5
  https://github.com/devote/HTML5-History-API
  */

  //map to keep track if ajax method already called 
  var ajaxCallMap = new Object();
  var target_selector = '#tabs-main';
  var $target = j$(target_selector);
  var $nav_el = j$('#nav-article');
  var $nav_lnks = $nav_el.find('a');
  if ($target.length == 0 || $nav_lnks.length == 0) { return false; }
  var default_html = $target.html();
  var default_title = document.title;
  var default_section = $target.data('section');
  $nav_el.on('click', 'a', function(e) {

    var a = j$(this);
    e.preventDefault();
    // keep the link in the browser history.  IE does not support method
    if ( !j$.browser.msie)
    	history.pushState( null, null, this.href );
    var url=window.location.href;
    
    var queryString=url.substring(url.indexOf('?'));
    var dsUrl=j$(this).attr('data-section');
    dataSection = j$(this).attr('data-section');
    if(dsUrl.indexOf("?")>0){
    	queryString = queryString.replace('?','&');
    }
    var url= "/xpl/"+dsUrl+queryString;
    showProductDetailsTab(j$(this), url, dataSection);

  });


  var showProductDetailsTab = function(eventOriginatorTab, url, dataSection ){
	 
	  var ajax_load = "<div class='tab-content'> <div id='spinning' class='article-blk'><img src='"+ ASSETS_RELATIVE_PATH +"/img/processing_information.gif' alt='Loading Data...'/>Loading Data...</div></div>";  
	  var closestLiElem= eventOriginatorTab.closest('li');
	  closestLiElem.siblings().removeClass('active');
	  closestLiElem.addClass('active');	 
	  //hide existing ta content and remove any spinning indicators
	  j$(target_selector).children().hide();
	  j$('#spinning').remove();
	  if(dataSection.indexOf("abstractCitations.ajax")==-1){
		  j$('#abstractCitations #crossRefNote').html("");
		  j$('#showAllCitations').remove();
	  }
	  if (j$('#showAllCitations').length > 0)
	  {
		  url = url.replace('abstractCitations','abstractCitationsAll');
	  }
	  var divTabId =  dataSection.split(".");
	  var $tabContent = j$('#'+divTabId[0]);
	  //check if tab content previously loaded, then show div content and return;
	  if ($tabContent.length > 0)
	  {
		  $tabContent.show();
		  return;
	  }
	  var ajaxCalled = ajaxCallMap[dataSection];
	  //if true, then first ajax call is still executing.  Thus, set spinning indicator and return;
	  if (ajaxCalled != undefined)
	  {
		  j$(target_selector).append(ajax_load);
		  return;
	  }
	  
	  j$(target_selector).append(ajax_load);
	  j$.ajax(url, {
	  		success: function(data) {
	  		j$('#spinning').remove();
	  		j$(target_selector).append(data);
	  	}
	  });
	  //store ajax call history in map.  The map is checked in future tab invocations to insure
	  //same ajax call is not made a second time while the existing call is still executing
	  ajaxCallMap[dataSection]=dataSection;
 }; 
 
 j$('#article-page-bdy').delegate("#articles_all_ciations_anchor","click",function(){
	//1. remove existing citation tab content
	j$('#abstractCitations').remove();
	j$(target_selector).append("<div id='showAllCitations'/>");
	 ajaxCallMap= new Object();
	//2. set click event to citation tab to make new ajax call
	j$('#nav-article #abstract-citedby-tab').trigger('click');
	
  });	

  // hang on popstate event triggered by pressing back/forward in browser
  j$(window).bind( "popstate", function(e) {
	  
	  //temporary fix to clear the ajax call history stored in ajaxCallMap when the backbutton is clicked
	  if (ajaxCallMap != undefined && mapSize(ajaxCallMap) > 0)
	  {
		  ajaxCallMap= new Object();
		  //alert(Object.keys(ajaxCallMap).length);
	  }
	 
    // we get a normal Location object

    /*
    * Note, this is the only difference when using this library,
    * because the object document.location cannot be overriden,
    * so library the returns generated "location" object within
    * an object window.history, so get it out of "history.location".
    * For browsers supporting "history.pushState" get generated
    * object "location" with the usual "document.location".
    */
    var returnLocation = history.location || document.location;
    
    /*
    * ISSUE: browsers using polyfill failing to update fragment, title & nav when navigating to 'original state' with back button
    * FIX: cache original fragment, title & nav state and use those if ajax returns no fragment
    */
    j$.ajax({
      url: returnLocation,
      data: {},
      success: function (data) {
        var frag = j$(data).find(target_selector);
        if (frag.html() !== undefined) {
          j$(target_selector).replaceWith(frag);
          document.title = stripTitle(data);
          updateNav(frag.data('section'));  
        } else {
          j$(target_selector).replaceWith(default_html);
          document.title = default_title;
          updateNav(default_section);
        }
        //_gaq.push(['_trackPageview', location.pathname+location.search]);        
      },
      dataType: 'html'
    });
  });
  
  var stripTitle = function(data) {
    return data.toString().substring(data.toString().indexOf("<title", 0) + 7, data.toString().indexOf("</title", 0))
  };
  
  var updateNav = function(section) {
    $nav_el.find('li.active').removeClass('active');
    $nav_el.find('a[data-section="' + section + '"]').closest('li').addClass('active');
  };

});




















