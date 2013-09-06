
function addFilter(filter,checked) { 
	  if(checked)
	  {
		  document.getElementById("newsearch").value="partialPref";
	  	  document.getElementById("filter").value=filter;
		// put a large number so that it would not hit the server
		  acompleter.options.minChars=3000;
		  document.getElementById('search_suggestions').style.visibility = 'hidden';
	  }
	  else
	  {
		  document.getElementById("filter").value="";
		  acompleter.options.minChars=3;
		  document.getElementById('search_suggestions').style.visibility = 'visible';
	  }
}

function submitForm() {
	document.forms["search_form"].submit();
}  
function fill(thisValue){
	j$('#queryText').val(thisValue);
	setTimeout("j$('#searchDropDown').hide();", 200);
} // fill
j$(function(){
	   // -- Search options click
   j$('#moreSearchOptions').bind('click', function(){
     j$('#searchOptions').show();

     
    });
    
   j$('#search-field').delegate('#searchOptions','mouseleave', function(){
	   j$('#searchOptions').hide();
   });
	  
	//Search with in suggetions has anchor tag in them With new search so preventing defalut behaviour
   j$('#search_within_suggestions').click(function(event){
	   event.preventDefault();
   });		
	j$(document).keyup(function(){
		activeObj = document.activeElement;
		if(j$(activeObj).parents('#search_form').length == 0) fill();
	});
	j$("#queryText").autocomplete({
		source:lookup,
			delay:1000,
			matchSubset:1,
			matchContains:1,
			cacheLength:10,
			autoFill:true
			
	});

	j$('#searchDropClose a').click(function(){
		fill();
		return false;
	});
});

function showAdvancedSearch(){
var url_string = '/search/advsearch.jsp';
location.href = url_string;
}

function fixsrchquote(oldstr) {

if(oldstr.length < 1){
	nocriteria=="";
	//nocriteria="Enter one or more keywords";	
	//alert("Enter one or more keywords");
	return nocriteria;
}

nLoc = 0;
	nStart = 0;
	var  tempstr;
tempstr = '"';
nLoc = oldstr.indexOf(tempstr, nStart);
sTemp = "";

while(nLoc != -1) {
	sTemp += oldstr.substring(nStart, nLoc);
	sTemp += "~~";
	nStart = nLoc+1;
	nLoc = oldstr.indexOf(tempstr, nStart);
}
sTemp += oldstr.substring(nStart, oldstr.length);
oldstr = sTemp;

nLoc = 0;
nStart = 0;
tempstr = "'";
nLoc = oldstr.indexOf(tempstr, nStart);
sTemp = "";
while(nLoc != -1) {
	sTemp += oldstr.substring(nStart, nLoc);
	sTemp += " ";
	nStart = nLoc+1;
	nLoc = oldstr.indexOf(tempstr, nStart);
}
sTemp += oldstr.substring(nStart, oldstr.length);

return sTemp;
}
function resultCountAction(){
document.getElementById("action").value="resultCount";	
document.getElementById("random").value=new Date().getTime();
document.searchResultsForm.submit();
}

function resetQueryText()
{	
if (document.getElementById('chkresult').checked){
//	document.getElementById('queryText').value="";	
	document.getElementById('queryText').focus();
} //else {
	//document.getElementById('queryText').value = document.getElementById('oldqrytext').value;
//}


}

/** Advanced Search functions 
* 
* @param Mform
* @param Mmenu
* @param Mvalue
* @param Mtext
* @return
*/
function menuChange(Mform, Mmenu, Mvalue, Mtext) {
 document.getElementById(Mmenu).value = Mtext;
 if(Mmenu=='menu1'){
    document.getElementById("scope1").value = Mvalue;
 }
 if(Mmenu=='menu2'){
    document.getElementById("scope2").value = Mvalue;
 }
 if(Mmenu=='menu3'){
    document.getElementById("scope3").value = Mvalue;
 }
 var dropMenu = 'Drop' + Mmenu;
 MM_showHideLayers(dropMenu,'','hide');
 menu1 = Mvalue;
 
}


function changePublications() {
 if (document.bsearch.std_status.selectedIndex > 0) {
    document.bsearch.coll1.checked=false;
    document.bsearch.coll2.checked=false;
    document.bsearch.coll3.checked=false;
    document.bsearch.coll4.checked=false;
    //document.bsearch.coll5.checked=true; 
    document.bsearch.coll1.disabled=true;
    document.bsearch.coll2.disabled=true;
    document.bsearch.coll3.disabled=true;
    document.bsearch.coll4.disabled=true;
    //document.bsearch.coll5.disabled=true;
    document.bsearch.srchlist[0].checked=true;
 } else {
    document.bsearch.coll1.checked=true;
    document.bsearch.coll2.checked=true;
    document.bsearch.coll3.checked=true;
    document.bsearch.coll4.checked=true;
    //document.bsearch.coll5.checked=true; 
    document.bsearch.coll1.disabled=false;
    document.bsearch.coll2.disabled=false;
    document.bsearch.coll3.disabled=false;
    document.bsearch.coll4.disabled=false;
    //document.bsearch.coll5.disabled=false;
    document.bsearch.srchlist[0].checked=true;
  }
 }

// For advsearch.jsp
function isvalidsearch(f,yr) {
    var q1 = "true";
    var q2 = "true";
    for (var i = 0; i < f.length; i++) {
        var c = f.elements[i];
        if (c.name == "query1") {
            if ((c.value == null) || (c.value == "") || isblank(c.value)) {
                q1 = "false";
            }
        } 
        if (c.name == "queryblock") {
            if ((c.value == null) || (c.value == "") || isblank(c.value)) {
                q2 = "false";
            }
        }
    }
    if ((q1 == "false") && (q2 == "false")) {
    alert("Please enter at least one search criteria!");
    return false;
    }
    BuildSrchQuery(yr);
    return true;
}



function BuildSrchQuery(YY){
    
    var queryText;
    
    var qry1 = document.bsearch.query1.value;
    var len1 = qry1.length;
            
    var qry2 = document.bsearch.query2.value;
    var len2 = qry2.length;
            
    var qry3 = document.bsearch.query3.value;
    var len3 = qry3.length;
    
    var sc1 = document.bsearch.scope1.value;
    var sc2 = document.bsearch.scope2.value;
    var sc3 = document.bsearch.scope3.value;
    
    var op1 = document.bsearch.op1.value;
    var op2 = document.bsearch.op2.value;

    var p1 = document.bsearch.py1.value;
    var p2 = document.bsearch.py2.value;
    	    
    var queryText = document.bsearch.queryblock.value;
    queryText = queryText.toLowerCase();
    if(queryText.length > 0){
    	var nLoc = queryText.indexOf("<in>");
    	var containLoc = queryText.indexOf("<contains>");
    	if(nLoc==-1 && containLoc == -1 ){
    	  queryText = "(("+ queryText +")<in>metadata)";
    	}
    	document.bsearch.reqloc.value = "adv";
    	
    }

    if( len1 > 0 || len2 > 0 || len3 > 0) {
       
        if(sc1.length > 0 && qry1.length > 0){
            qry1 = " ((" + qry1 + ")<in>" + sc1 + " ) ";
        }
        
        if(sc2.length > 0 && qry2.length > 0){
            qry2 = " ((" + qry2 + ")<in>" + sc2 + " ) ";
        }
        
        if(sc3.length > 0 && qry3.length > 0){
            qry3 = " ((" + qry3 + ")<in>" + sc3 + " ) ";
        }
	
		
        
        if(len1 > 0 && len2 < 1 && len3 < 1){
         
          queryText = qry1;
        }
        
        if(len1 > 0 && len2 > 0 && len3 < 1){
          queryText = "("+ qry1 + "<" + op1 +">" + qry2 +")";
        }
        
        if(len1 > 0 && len2 > 0 && len3 > 0){
          queryText = "("+ qry1 + "<" + op1 +">" + qry2 +")"+ "<" + op2 + ">" +qry3;
        }
    
    }
    queryText=fixsrchquote(queryText);
    
    
    if(p1 == '1950' && p2 == YY && queryText.length > 0){
	    queryText = queryText;
    }else{
        queryText = "(" + queryText +") <and> (pyr >= "+ p1 + " <and> pyr <= " + p2 + ")";
    }
  	
    document.bsearch.queryText.value = queryText.toLowerCase();    
    
    var ipel = SelectRadio();
    
    if(ipel=='ipellist'){
    	
    	document.bsearch.queryText.value = "("+queryText.toLowerCase() + ")<and>(ipel_flag<in>packages)";
    	
    }
    
    if(document.bsearch.coll1.checked ==true) {
	    document.bsearch.coll6.value = "preprint";  
    }
    if(document.bsearch.coll1.checked ==false) {
	    document.bsearch.coll6.value = "";
    }
   
}

function buildQuery(YY)
{
	
	 var queryText;
	 var Ntk, Ntt, Nf;
	 
	 var qry1 = document.advancedSearchForm.query1.value;
	 var len1 = qry1.length;
	            
	 var qry2 = document.advancedSearchForm.query2.value;
	 var len2 = qry2.length;
	            
	 var qry3 = document.advancedSearchForm.query3.value;
	 var len3 = qry3.length;
	    
	 var sc1 = document.advancedSearchForm.scope1.value;
	 var sc2 = document.advancedSearchForm.scope2.value;
	 var sc3 = document.advancedSearchForm.scope3.value;
	    
	 var op1 = document.advancedSearchForm.op1.value;
	 var op2 = document.advancedSearchForm.op2.value;
	    
	 var p1 = document.advancedSearchForm.py1.value;
	 var p2 = document.advancedSearchForm.py2.value;
	    
	 if( len1 > 0 || len2 > 0 || len3 > 0) {
		       
	     if(sc1.length > 0 && qry1.length > 0){
	    	queryText = qry1;
	     	Ntk = sc1;
	      	Ntt = qry1;
	     }
	        
	     if(sc2.length > 0 && qry2.length > 0){
	       	if(Ntk.length>0){
		       	Ntk = Ntk+"|"+sc2;
		       	Ntt = Ntt+"|"+qry2;
	       	}else{
		        Ntk = sc2;
		       	Ntt = qry2;
	       	}
	     }
	        
	     if(sc3.length > 0 && qry3.length > 0){
	       	if(Ntk.length>0){
		       	Ntk = Ntk+"|"+sc3;
		       	Ntt = Ntt+"|"+qry3;
	       	}else{
		        Ntk = sc3;
		       	Ntt = qry3;
	       	}
	     }
	 }
	 if(p1 == '1950' && p2 == YY && queryText.length > 0){
	 }else if(p1.length>0&&p2.length>0){
	    Nf = "year|BTWN "+ p1+" "+p2;
	 }else
		Nf ="";
	var N="";
	for(i=1; i<document.advancedSearchForm.paramN.length;i++){
		if(document.advancedSearchForm.paramN[i].checked){
			if(N.length>=1){					
				N = N+"+"+document.advancedSearchForm.paramN[i].value;
			}else{
				N = document.advancedSearchForm.paramN[i].value;
			}
		}
	}
	if(N.length<1){
		N = "0";
	}	
	document.advancedSearchForm.Ntk.value = Ntk; 
	document.advancedSearchForm.Ntt.value = Ntt; 
	document.advancedSearchForm.Nf.value = Nf; 
	document.advancedSearchForm.N.value = N; 
	document.advancedSearchForm.queryText.value = Ntt; 
}

function chk_range(val,yy){

    if(val=='recentyr'){
         document.advancedSearchForm.py1.value='1950';
          document.advancedSearchForm.py1.disabled=true;
          document.advancedSearchForm.py2.value=yy;
          document.advancedSearchForm.py2.disabled=true;
     }  
    if(val=='allyr'){
          document.advancedSearchForm.py1.disabled=false;
          document.advancedSearchForm.py2.disabled=false;
     }

}
function applySpecialCharacterMapping(terms, ignoreBrackets) {
	for (var i = 0; i < SPECIAL_CHARACTERS.length; i++) {
		if(ignoreBrackets && (SPECIAL_CHARACTERS[i] == '(' || SPECIAL_CHARACTERS[i] == ')')) {
			continue;
		}
		var index = terms.indexOf(SPECIAL_CHARACTERS[i]);
		while (index > -1) {
			terms = terms.replace(SPECIAL_CHARACTERS[i], SPECIAL_CHARACTER_REPLACEMENTS[i]);
			index = terms.indexOf(SPECIAL_CHARACTERS[i]);
		}
	}
	return terms;
}
function getFilteredTermsExt(terms, ignoreBrackets) {
    terms = terms.replace(/\*+/, "*").replace(/\s+/, " ");
    terms = applySpecialCharacterMapping(terms, ignoreBrackets);
    terms = terms.replace(/^\s+|\s+$/g, "");
    return terms;
}

function getFilteredTerms(terms) {
    return getFilteredTermsExt(terms, false);
}




function filterQueryElement(textField, minCharsInWildCardsInWord) {
    var min = 3;
    if(arguments.length > 1) {
        min = minCharsInWildCardsInWord;
    }
    var terms = textField.value;
    terms = j$.trim(terms);
    if(!terms.match(/\w/)) {
        alert("Please enter one or more keywords");
        return false;
    }
    terms = getFilteredTerms(terms);
    var stars = terms.match(/\*/g);
    if(stars != null && stars.length > 5) {
        alert("Please limit the number of wildcarded words to 5");
        return false;
    }
    parts = terms.split(/[^*_a-z_A-Z_0-9]/);
    for(var i = 0; i < parts.length; i++) {
        if(parts[i].indexOf("*") > -1) {
            var validChars = parts[i].match(/\w/g);
            if(validChars == null || validChars.length < min) {
                alert("Please provide at least " + min + " valid characters in wildcarded word (" + parts[i] + ")");
                return false;
            }
        }
    }
    terms = stopCrossSiteScripting(terms);
    textField.value = terms;
    return true;
}