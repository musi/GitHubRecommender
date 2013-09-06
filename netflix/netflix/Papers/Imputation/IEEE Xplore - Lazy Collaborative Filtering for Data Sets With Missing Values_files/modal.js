  Modal = {
		   submitForm:function(formName) {
			var url=$(formName).action;
			url=url+"?"+$(formName).serialize();
		    j$.colorbox({href:url,scrolling:false,opacity:0.6,overlayClose:false,close:""}); 
		    return false;    
		  },
		  show:function(url) {
		    j$.colorbox({href:url,scrolling:false,opacity:0.6,overlayClose:false,close:""});
			j$('#cboxTopRight').html('<a id="mdCloseButton" href="javascript:Modal.hide()" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
			
		  },
		  showLegacyAccountTransition:function(url) {
			    j$.colorbox({href:url,scrolling:false,opacity:0.6,overlayClose:false,close:""});
			    j$('#cboxTopRight').html('<a id="mdCloseButton" href="#" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
			  },
		  
		  showIframeInstSignIn:function(url) {
			    j$.colorbox({href:url,width:"680px",height:"320px",scrolling:false,iframe:true,opacity:0,overlayClose:false,close:""});
				j$('#cboxTopRight').html('<a id="mdCloseButton" href="javascript:Modal.hide()" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
			  },
		  showIframeMemberSignIn:function(url) {
			   j$.colorbox({href:url,width:"730px",height:"500px",scrolling:false,iframe:true,opacity:0,overlayClose:false,close:""});
			   j$('#cboxTopRight').html('<a id="mdCloseButton" href="javascript:Modal.hide()" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
		  },
		  showIframeSignOut:function(url) {
			    j$.colorbox({href:url,width:"680px",height:"420px",scrolling:false,iframe:true,opacity:0,overlayClose:false,close:""});
				j$('#cboxTopRight').html('<a id="mdCloseButton" href="javascript:Modal.hide()" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
			  },

		  
		  refresh:function(url) {
		   j$.colorbox({href:url,scrolling:false,overlayClose:false,overlayClose:false,opacity:0.6}); 
		   j$('#cboxTopRight').html('<a id="mdCloseButton" href="javascript:Modal.hide()" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
		  },
		  
		  refreshLegacyAccountTransition:function(url) {
			   j$.colorbox({href:url,scrolling:false,overlayClose:false,overlayClose:false,opacity:0.6}); 
			   j$('#cboxTopRight').html('<a id="mdCloseButton" href="#" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
			  },
		  
		  refreshIframe:function(url) {
			   j$.colorbox({href:url,scrolling:false,iframe:true,overlayClose:false,overlayClose:false,opacity:0.6}); 
			   j$('#cboxTopRight').html('<a id="mdCloseButton" href="javascript:Modal.hide()" title="Close this window"><img src="/assets/img/btnCloseModalWindow.gif" alt="Close this window" /></a>');
			  },	  
	

		  hide:function() {
		    j$.colorbox.close();
		  },
		  
		  cancelIframe:function(){
			 parent.j$.fn.colorbox.close(); 
		  },
		  closeAndSubmitForm:function(formName){		  	
		  		var 
				$form       = j$('#' + formName),
				url         = $form.attr('action'),
				projectData = $form.serialize();
				
				j$.post(url,projectData,function(data){
					j$.colorbox({
				    	html:data,
				    	scrolling:false,
				    	opacity:0.6,
				    	overlayClose:true,
				    	close:""
			    	});		    	
				});

				
				return false;    
		  },
		  closeAndRefresh:function(){
				  j$.colorbox.remove();
				  var url = location.href;				  
				  if(url.indexOf("guesthome") > -1 && url.indexOf("signout=success") > -1) {
					  url = url.replace("\?signout=success","");
					  javascript:void(location.href=url);
				  }else
					  location.reload();
		  },
		  closeAndRedirectToUrl:function(url){
			  if (url != undefined && j$.trim(url) != '') {
				j$.colorbox.remove();
				javascript:void(location.href=url);
			  }
		 }
			  
		  
};  
 function showLoadingImageText(id){
	    j$("#"+id).show();
	    j$("#"+id+"-Text").hide();
	    loadingImgTimer=setTimeout(function(){j$("#"+id+"-DefaultText").hide();j$("#"+id+"-Text").show();},10000);
	  
	    
}
 function hideLoadingImageText(id)
 {
 	j$("#"+id).hide();
 	j$("#"+id+"-Text").hide();
 	j$("#"+id+"-DefaultText").hide();
 	clearTimeout(loadingImgTimer);
 }
