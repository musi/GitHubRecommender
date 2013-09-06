function showSignIn(){
    if (j$('#singleSignOnFlyout') != "" && j$('#singleSignOnFlyout') != undefined) {
        j$('#singleSignOnLink').click();
        //j$('#singleSignOnFlyout').show();
    } 
    else {
        try {
            javascript: Modal.show('/xpl/mwMemberSignIn.jsp');
        } 
        catch (e) {
        //do nothing
        }
    }

}
function p_refreshPage() {
	location.reload();
}

function doPurchaseFromPartner() {
	var formId = '#'+j$(this).closest('form').attr('id');
	var formCheckBoxCountCount = j$(formId+ " input:checked").length;
	if (formCheckBoxCountCount <= 0) {
		alert('No item selected.  Please select an item.');
		return false;
	}else{		
		window.location.href = j$(formId).attr('action');
		return false;
	}
}

function addItemsToCart(){
	try{
		j$('#article-page-hdr').removeClass('menu-open');
		var formId = '#'+j$(this).closest('form').attr('id');
		var count = j$(formId+ " input:checked").length;
		if (count <= 0) {
			alert('Please select an item to add to Cart');
			return false;
		}
		else if (isFunction("isPartNumberExists")== false || isFunction('mc_addItems')== false )
		{
			alert("The ecommerce service is not available at this time.  Please try again .");
			return false;
		}
		var items='[';
	
		j$(formId+ " input:checked").each(function(index) {
		    var item_to_add = '{"partNum":"'+j$(formId+ " input:checked").val()+'","quantity":"1","membershipType":"","country":""}';
		    items=items+item_to_add;
		    if(index < (count-1))
		    	items=items+','+"'+'";  
		  });
		items=items+']';
		j$("#add-to-cart-button").attr("disabled", "disabled");
		mc_addItems(items,addToCartSuccess,addToCartFailure);
	}catch(err){
		alert(err);
		alert("The ecommerce service is not available at this time.  Please try again later.");
		return false;
	}
	return false;

}
function addToCartFailure(){
	
}
function mc_removeItem_success(){
	toggleAddToCart();
	j$("#add-to-cart-button").removeAttr("disabled");
}
function addToCartSuccess(){
	toggleAddToCart();
}
