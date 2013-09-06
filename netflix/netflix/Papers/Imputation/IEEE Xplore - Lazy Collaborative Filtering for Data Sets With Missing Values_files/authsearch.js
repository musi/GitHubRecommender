AuthorSearch = {
    init: function(){ 
        //jQuery('.author-search-container input').placeholder();                    
        AuthorSearch.bindEvents();
        AuthorSearch.newFeatureIcon();
    },
    constructAuthName: function(firstName, middleInit, lastName) {
        var firstNameMidInit = firstName + " " + middleInit;
        if(lastName != '' && firstNameMidInit.trim() != '') {
            lastName = lastName + ", ";
        }

        return lastName + firstNameMidInit;
    },
    bindEvents: function(){
        jQuery('.author-search-container input').keyup(function(e) {
            if(e.which == 13) {
                jQuery(this).parents().find('.search-button input').click();
            }
        }); 

         jQuery('.auth-form-action').click(function(e) {
            e.preventDefault();
            AuthorSearch.processQuery();
        });

        jQuery('.search-toggle').click(function(e) {
        var j$this = jQuery(this),
            currVal = j$this.html(),
            j$authContainer = jQuery('#auth-search-form'),
            j$bsContainer = jQuery('#search-fieldset');
        j$this.html(j$this.data('other'));
        j$this.data('other', currVal);
        j$this.toggleClass('pad-adjust');
        j$authContainer.toggle();
        j$bsContainer.toggle();
        AuthorSearch.newFeatureIcon();
        return false;
    });

    },
    /*validatePlaceholderValues: function(runType){ 
        if(!jQuery.fn.placeholder.input) { //if placeholder functionality is not supported (IE)   
            jQuery('#fname, #minit, #lname').each(function(k,namePiece){
                var $this = jQuery(namePiece),
                    placeholder = $this.attr('placeholder');             
                $this.val($this.val() === placeholder ? '' : $this.val());                                    
            });
        }        
    },*/
    getAuthorNameUrlString: function(authorString,firstName,midInit,lastName){
        var filteredTerm = getFilteredTerms(authorString),
            urlDomain = '/search/searchresult.jsp?',
            argsObj = {};
        	var searchWithinArgs = [];
            argsObj[searchPropertiesParamMatchBoolean] = 'true';
            argsObj[searchPropertiesParamNewSearch] = 'true';
            
            
            if (firstName)
            	searchWithinArgs.push(authorsFirstNameProperty + ':' + firstName);
      		if (midInit)
     			searchWithinArgs.push(authorsMiddleNameProperty + ':' + midInit);
            if (lastName)
	        	searchWithinArgs.push(authorsLastNameProperty + ':' + lastName);
	           jQuery.each(searchWithinArgs, function(k,v) {
	        	argsObj[searchPropertiesParamSearchWithin + k]=v;
	        });
	        var searchUrl = jQuery.param(argsObj);
	        searchUrl = searchUrl.replace(/searchWithin[0-9]*/g,searchPropertiesParamSearchWithin);
	        return urlDomain + searchUrl;                
    },
    processQuery: function(){
        var $firstName = jQuery('#fname'),
            $midInitial = jQuery('#minit'),
            $lastName = jQuery('#lname'),
            firstName = $firstName.val(),
            midInit =  $midInitial.val(),
            lastName = $lastName.val(),
            authName = AuthorSearch.constructAuthName(firstName, midInit, lastName),
            tempNode = document.createElement('input'); //adapter to work with existing filterQueryElement fn
        
        tempNode.value = authName;
        firstName = stopCrossSiteScripting(firstName);
        lastName = stopCrossSiteScripting(lastName);
        midInit = stopCrossSiteScripting(midInit);
        if(firstName === '' && lastName === '' && midInit === '') {
            alert('First Name, Last Name or Middle Name is required');
        } else {
        	var isWildCardQualified = filterQueryElement(tempNode);
        	if(isWildCardQualified) 
            window.location.href = AuthorSearch.getAuthorNameUrlString(authName,firstName,midInit,lastName);
        }


    },
    newFeatureIcon: function(){
        jQuery('#search-field .newFeature').remove();
        jQuery('#search-field .tools').before('<span class="newFeature"></span>');
        jQuery('#search-field .newFeature').html('');
        },
}; //AuthorSearch object

function authorLink(authorTerm, authorId) {
	var filteredtTerm = getFilteredTerms('"' + authorTerm + '"');
    var link = '/search/searchresult.jsp?' + searchPropertiesParamSearchWithin + '=' + authorsGetReference + ':' + filteredtTerm;
    if (authorId && authorId.length > 0)
    {
    	var authorIdParam =authorIdReference+ ':' +authorId;
        link += '&' +searchPropertiesParamSearchWithin + '=' + authorIdParam;
    }
    link += '&' + searchPropertiesParamNewSearch + '=true';    
    return link;
}
/* Author Search Functions*/
var j$ = jQuery.noConflict();
j$(document).ready(function() {
    j$('.prefNameLink').each(function() {
        var authorName = j$(this).find("#preferredName").attr("class");
        var authorId = j$(this).find("#authorId").attr("class");
        var writeAuthorLink = authorLink(authorName, authorId);
        j$(this).attr('href', writeAuthorLink);
    });

   
AuthorSearch.init();

});