
j$(document).ready(function() {
	j$('#pofsignIn').bind('click',signIn);
	stopSpinner();
	
});

//This is a fire and forget request, the result of this call is ,ObSSO cookie set in the browser.
//This is the cookie used for all the IEEE enterprise properties managed by Oracle Access Manager.
function callEnterpriseSignOn(form_obj,data){	
	var millisecondsToWait = 1000;
	millisecondsToWait = IBP_MEMBEER_SIGNIN_TIME_WAIT_IN_MILLIES;	
	a_authenticateUserByJsonp(data.username,data.password);
		setTimeout(function() {	
			return xploreSignIn(form_obj,data);
	}, millisecondsToWait);				  

}
function a_authenticateUserByJsonp(singleSignOnUserName,singleSignOnPassword){
	 a_authenticateUser(singleSignOnUserName,singleSignOnPassword);
}
function showSpinner(){
	j$('#SpinnerImage').show();
}
function stopSpinner(){
	j$('#SpinnerImage').hide();
}
j$.fn.serializeObject = function()
{
    var o = {};
    var a = this.serializeArray();
    j$.each(a, function() {
        if (o[this.name] !== undefined) {
            if (!o[this.name].push) {
                o[this.name] = [o[this.name]];
            }
            o[this.name].push(this.value || '');
        } else {
            o[this.name] = this.value || '';
        }
    });
    return o;
};
function xploreSignIn(form_obj,data){
	request = {
			url: form_obj.attr('action') ,
            method: "POST",
            data: data
	   		
	};
	responseHandler = function(responsedata,status,headers,eventOrig){
		handleResponse(responsedata,status,headers,form_obj);
	};
	errorHandler = function(responsedata,status,headers,eventOrig){
		handleError(responsedata,status,headers,form_obj);
	};

	xhr.request(request,responseHandler,errorHandler);
	return false;
}
function signIn()
{	
	showSpinner();
	var form_obj=j$(this).closest('form');
	var data=form_obj.serializeObject();	
    if( (data.username == "" && data.password == "")|| (data.username == "") ||  (data.password == "")) {
    	j$("#errorMessages").html("Please enter a valid username and/or password");
    	stopSpinner();
    	return false;
    }
    else
    {
	     if(IBP_WS_ENABLED_FLAG == true ){		 
	    	return callEnterpriseSignOn(form_obj,data);
	     }else{
			  return xploreSignIn(form_obj,data);
		 }	
    }
	
}
function handleResponse(responsedata,status,headers,eventOrig){		
   stopSpinner();
   var jsonData = easyXDM.getJSONObject().parse(responsedata.data);
   if(jsonData.errorReason)
	{		
			eventOrig.find("#errorMessages").html(jsonData.errorReason);
	}
	else
	{		
		
			if(returnTargetUrl() != null && returnTargetUrl() != ""){			  
				  window.parent.location.replace(returnTargetUrl());
			} else{
				var url = location.href;
				location.reload();
			}
			
		
	}

   
}
function handleError(responsedata,status,headers,eventOrig){	
	stopSpinner();
}

function returnTargetUrl(){	
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
	
	return targetUrl;
  
}

