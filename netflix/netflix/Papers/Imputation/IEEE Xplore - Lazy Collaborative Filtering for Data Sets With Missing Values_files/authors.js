if(!j$) var j$ = jQuery.noConflict();

//change class name to turn off hover for 2013-05-09 release
j$(function(){
   j$('.authorPreferredName_Off').qtip({
       content: {
           text: function() {
           var aliases = j$(this).attr('title');
           var affiliations = j$(this).find("#authorAffiliations").attr("class");
                          alias = [];
               aliasList = '' ;
           
           if (!aliases && !affiliations){
        	   return;
           }
           var tipHtml = '';
           if (aliases)
           {
	           aliases = aliases.slice(0,-3); //remove the last |c| delimiter
	           alias = aliases.split('|c|');
	           alias.sort();
	           j$.each(alias, function() {
	               aliasList = aliasList + '<li>' + this +'</li>';
	               return aliasList;
	           });
	           tipHtml = '<div class="overlay-label">Also Known As:</div><div class="aliases"><ul>'+ aliasList +'</ul></div>';
           }
           if (affiliations)
           {
        	   var affiliationList = '' ;
        	   var affiliation = [];
        	   affiliations = affiliations.slice(0,-3); //remove the last |c| delimiter
        	   affiliation = affiliations.split('|c|');
        	   var s = j$(affiliation).size();
        	   j$.each(affiliation, function(key,value) {
        		   if(s == 1){
        			   affiliationList = affiliationList + '<li>' + value +'</li>';
        		   }
         		  else{
         			  affiliationList = affiliationList + '<li>' + value;
         			  if(key < s-1){
         				 affiliationList = affiliationList+';'+'</li>';
         			  }
         		  }
                  return affiliationList;
         		  
               });
        	   tipHtml = tipHtml+ '<br/><div class="overlay-label">Affiliation(s):</div><div class="aliases"><ul>'+ affiliationList +'</ul></div>';
           }
           return tipHtml;           
       }
   },
       position: {my: 'bottom middle', at: 'top middle'},
       style: {width: '300px', classes: 'qtip-lightIeee'}
   });

});   
