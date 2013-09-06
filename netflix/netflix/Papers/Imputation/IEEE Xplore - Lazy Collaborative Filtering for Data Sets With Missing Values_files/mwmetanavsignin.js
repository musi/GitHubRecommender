var j$ = jQuery.noConflict();  
// this is really what you are after
function xploreSignIn6(){
	var request = {
			url: j$("#mwMetaNavLoginModalWindowForm").attr('action'),
            method: "POST",
            data: {
                username: j$("#username").val(),
                password: j$("#password").val(),
                src2: j$("#src2").val(),
                url2: j$("#url2").val()
                
            }           
};	
var responseHandler = function(responsedata,status,headers){
	
	var jsonData = easyXDM.getJSONObject().parse(responsedata.data);
	
	if(responsedata.headers.Error)
	{
		//alert("error header :" +responsedata.headers.Error);
		if(responsedata.headers.Error.trim() == "badcreds")
		{
			//alert("badcredentials");
			j$("#loadingImg").hide();
			j$('#memberSignInErrorReplace').replaceWith('<h3 id="memberSignInErrorReplace">'+jsonData.errorReason+'</h3>');
 			j$("#modalWindowSignInError281").show();
 			  if (PAGE_TAGGING){
     			  dcsMultiTrack('DCS.dcsuri','/xplore_SIGNIN?em_Add=NA&status=SIGNINERROR','WT.ti','xplore_SIGNINERROR','DCS.dcsqry','');
     		  }

 			  
		}
		else if(responsedata.headers.Error.trim() == "membermisuse" )
		{
			//alert("membermisuse");
			  if (PAGE_TAGGING){
     			  dcsMultiTrack('DCS.dcsuri','/xplore_SIGNIN?em_Add=NA&status=SIGNINERROR','WT.ti','xplore_SIGNINERROR','DCS.dcsqry','');
     		  }
			window.parent.location.replace(jsonData.errorUrl);
		   
		}
		else if(responsedata.headers.Error.trim() == "authfailure" )
		{    			   
		   //alert("authfailure");
			j$("#loadingImg").hide();
		   j$('#memberSignInErrorReplace').replaceWith('<h3 id="memberSignInErrorReplace">'+jsonData.errorReason+'</h3>');
		   j$("#modalWindowSignInError281").show();
		   if (PAGE_TAGGING){
     			  dcsMultiTrack('DCS.dcsuri','/xplore_SIGNIN?em_Add=NA&status=SIGNINERROR','WT.ti','xplore_SIGNINERROR','DCS.dcsqry','');
     		  }

			}
	}
	else
	{    		
		j$("#singleSignOnFlyout").slideUp(5);
		var jsonData = easyXDM.getJSONObject().parse(responsedata.data);    			  			
		if(jsonData.successRedirectUrl)
		{
		  var userType=jsonData.userType;

		  if(userType=="M" && j$("#username").val().indexOf("@")==-1)
		  {    	
			  if(IBP_WS_ENABLED_FLAG == true )
			  {       				  
				  //var url='/xpl/mwLegacyAccountTransition.jsp?sid='+globalCustomerNumber+'&emailId='+emailAddressOnFile+'oldUserName='+singleSignOnUserName;
    			  var oldUserName = j$("#username").val();
    			  j$.cookie('legacyUserName' ,oldUserName, { path:'/',domain:'.ieee.org' });
    			  var url='/xpl/mwLegacyAccountTransition.jsp';
    			
    			 Modal.refreshLegacyAccountTransition(url);
    			 return false;
			 }
		  }		
		  if (PAGE_TAGGING){
			   dcsMultiTrack('DCS.dcsuri','/xplore_SIGNIN?em_Add='+ j$("#username").val()+'_SIGNIN&status=SIGNINSUCCESS','WT.ti','xplore_SIGNINSUCCESS','DCS.dcsqry','');
			  }
		  var targetUrl = window.location.href;	
		      var redirecturl = targetUrl;
			      if(targetUrl.indexOf("guesthome.jsp") > 0)
				      targetUrl="/Xplore/guesthome.jsp"; 
			    				      
				  if(targetUrl.indexOf("url=") >=0)
				  {
					  targetUrl=targetUrl.substring(targetUrl.indexOf("url=")+4);
					  if(targetUrl.indexOf("&") >=0)
						  targetUrl=targetUrl.substring(0,targetUrl.indexOf("&"));
					  
					  if(targetUrl.indexOf("%3A%2F%2F")>0)
					  {
						  targetUrl=decodeURIComponent(targetUrl);
					  }
				  }
				  
				  if( (redirecturl.indexOf("/servlet/wayf.jsp") > 0) && (targetUrl.indexOf("home.jsp") > 0) ){
					  targetUrl="/Xplore/home.jsp";  										  
				  }
				  if(targetUrl.indexOf("login.jsp") >=0)
					  targetUrl=document.referrer;
				  
				  if(targetUrl.indexOf("login.jsp")>=0 && targetUrl.indexOf("home.jsp") > 0)
					  targetUrl="/Xplore/home.jsp";
				  
			  if(targetUrl.indexOf("guesthome.jsp") > -1 )
			  {					 
				  targetUrl=targetUrl.replace("guesthome","dynhome");
			  }					 
			  if( targetUrl.indexOf("ieeevendorsso") > -1 && targetUrl.indexOf("nexturl") > -1) 
			  {
				  targetUrl = targetUrl.substring(targetUrl.indexOf("nexturl")+8);						 
			  }
			  if(targetUrl.indexOf("dynhome") > -1 )
			  {					  
				  targetUrl=targetUrl.replace("\?signout=success","");					  
			  }
			  if(targetUrl.indexOf("dynhome") > -1 )
			  {					  
				  targetUrl=targetUrl.replace("\?signout=success#","");					  
			  }
			
			  if(targetUrl.indexOf("#") != -1)
				  targetUrl=targetUrl.replace("#", ""); 	  
			 
			  if(targetUrl != null && targetUrl != ""){			  
				  window.parent.location.replace(targetUrl);
			  } else{
				  //alert("going to refresh");
				  Modal.closeAndRefresh();
			   }
			  
		}
		
	}    
};
var errorHandler = function(responsedata,status,message) {
	j$("#loadingImg").hide();
	j$('#memberSignInErrorReplace').replaceWith('<h3 id="memberSignInErrorReplace">'+'This function is temporarily unavailable. Please try again later.'+'</h3>');
		j$("#modalWindowSignInError281").show();		    
};	
xhr.request(request, responseHandler, errorHandler);
}
j$('document').ready(function(){

	
	j$('#mwMetaNavLoginModalWindowForm').keydown(function(e) {
		if (/MSIE (\d+\.\d+);/.test(navigator.userAgent)){ //test for MSIE x.x;
			 var ieversion=new Number(RegExp.$1); // capture x.x portion and store as a number
			 if (ieversion>=6){
			  if(e.which==13)
				  j$("#modalWindowSignInBtn").click();
			 }
		}
	});
		 
	
	j$("#metaNavSignInForgotUsernameLink").click(function(){
		j$("#singleSignOnFlyout").slideUp(1);
		Modal.show("/xpl/mwForgotUserPasswordIntro.jsp");
	});//End metaNavSignInForgotUsernameLink click function
	
		
	
	
	j$("#modalWindowSignInError281").hide();	
    j$("#modalWindowSignInBtn").click(function(event){ 
    	
    	j$("#modalWindowSignInError281").hide();
    	event.preventDefault();    	
    	j$("#loadingImg").show();
    	var singleSignOnUserName = '';
    	var singleSignOnPassword = '';
    	singleSignOnUserName = j$("#username").val();
    	singleSignOnPassword = j$("#password").val();
    	var isValidDataEntered = false;
        	 
    	if(singleSignOnUserName != null && singleSignOnUserName != '' && singleSignOnPassword != null && singleSignOnPassword != ''){
    		isValidDataEntered = true;
    		//alert(" isValidDataEnterd : " +isValidDataEntered);
    	} 
    	  if(isValidDataEntered){
    		  var millisecondsToWait = 1000;
			  if(IBP_WS_ENABLED_FLAG == true ){		
				  		  
				  a_authenticateUserByJsonp(singleSignOnUserName,singleSignOnPassword);
				  millisecondsToWait = IBP_MEMBEER_SIGNIN_TIME_WAIT_IN_MILLIES;					
				  	setTimeout(function() {		
				  		xploreSignIn6();
					}, millisecondsToWait);				  
			  }
			  else{
				  xploreSignIn6();
			  }
			  if (PAGE_TAGGING){
    			  dcsMultiTrack('DCS.dcsuri','/xplore_SIGNIN?em_Add='+ singleSignOnUserName+'_SIGNIN','WT.ti','xplore_SIGNIN','DCS.dcsqry','');
    	   	  }
		  }
     	return false; 
		
    	  
    });//End click function
  });//End document ready function