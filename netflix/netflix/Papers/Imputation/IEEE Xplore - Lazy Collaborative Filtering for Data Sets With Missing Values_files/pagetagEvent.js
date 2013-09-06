(function (j$) {	
		j$(function () {
			if (!PAGE_TAGGING) {return;}
			/********** ICP Page Tagging *****************/
			j$('.tools-bookmark').addTag({tagName : 'Bookmark_Article',
				analyticType : 'unica'
			});
			j$('.tools-email').addTag({tagNode : 'span',tagAttr : 'id',
				analyticType : 'unica'
			});
			j$('.tools-print').addTag({tagName : 'Print Page',
				analyticType : 'unica'
			});
			j$('.tools-cites').addTag({tagName : 'Download Citations',
				analyticType : 'unica'
			});
			j$('.tools-refs').addTag({tagName : 'Download_Reference',
				analyticType : 'unica'
			});
			j$('.tools-rights').addTag({tagName : 'Rights_Permissions',
				analyticType : 'unica'
			});
			j$('.pdf').addTag({tagName : 'Download_PDF',
				analyticType : 'unica'
			});
			j$('#article-nav ul').addTag({tagNode : 'li a',tagAttr : 'html',
				analyticType : 'unica'
			});
			j$('#glance-hdr .tab-menu').addTag({tagNode : 'li',tagAttr : 'id',tagType : 'dynamic',
				analyticType : 'unica'
			});
			j$('.article-hdr').addTag({tagAttr : 'href',
				analyticType : 'unica'
			});
			j$('#FooterWrapperBottom').addTag({tagAttr : 'class',eventPrefix : 'footerNavLinks',
				analyticType : 'unica'
			});
			j$('#instSignInOptions li').addTag({tagNode : 'a',tagAttr : 'html',
				analyticType : 'unica'
			});
			j$('#FooterWrapper').addTag({tagNode : 'a', tagAttr : 'html',eventPrefix : 'footerNavLinks',
				analyticType : 'unica'
			});
			j$('.tools-cites').addTag({tagName : 'save-to-project-button',
				analyticType : 'unica'
			});
			
			/********** My Settings Menu Page Tagging *****************/
			j$('#my-account li').addTag({tagNode : 'a',
				tagAttr : 'class',	eventPrefix : 'mysettings',
				analyticType : 'unica'
			});
			
			/********** My Projects Menu Page Tagging *****************/
			j$('#my-projects').addTag({tagNode : 'a',tagName : 'myprojects',
				analyticType : 'unica'
			});
			
			/********** Search Menu Options Page Tagging *****************/
			j$('.search-field .tools').addTag({tagNode : 'a',
				tagAttr : 'class',eventPrefix : 'Search',
				analyticType : 'unica'
			});
			j$('.search-field .tools').addTag({tagNode : 'span',
				tagAttr : 'class',eventPrefix : 'Search',
				analyticType : 'unica'
			});
			/********** Browse Menu Page Tagging *****************/
			j$('#browse-content  div.SubMenu li').addTag({tagAttr : 'class',
				eventPrefix : 'Browse',analyticType : 'unica'
			});			
			/********** Meta Nav Page Tagging *****************/
			j$('#UtilityNav .externalLinks').addTag({tagAttr : 'class',eventPrefix : 'external link',
				eventLocation : 'true',
				analyticType : 'unica'
			});			
			j$('#UtilityNav #mnEvLinks').addTag({tagNode : 'a',tagAttr : 'title',eventPrefix : 'metaNavLinks',
				analyticType : 'unica'
			});
			
			j$('#singleSignOnFlyout').addTag({tagName : 'FORGOT_USERID-xplore_FORGOT_USERID_PWD_VIEW',tagNode : '#metaNavSignInForgotUsernameLink',
				tagType : 'dynamic'
			});
			j$('#singleSignOnFlyout').addTag({tagName : 'FORGOT_PWD-xplore_FORGOT_USERID_PWD_VIEW',tagNode : '#metaNavSignInForgotUsernameLink',
				tagType : 'dynamic'
			});
			/********** ToolBarWrapper Page Tagging *****************/
			j$('#ToolBar .support').addTag({tagAttr : 'id',
				analyticType : 'unica'
			});			
			/********** Abstract Page-Tools Page Tagging *****************/
			j$('.page-tools').addTag({tagNode : '#popup-download-document-citations',
				tagAttr : 'id',
				analyticType : 'unica'
			});
			j$('.page-tools').addTag({tagNode : '#rightsandpermissions',tagAttr : 'id',
				analyticType : 'unica'
			});
			j$('.page-tools').addTag({tagNode : '#popup-download-searchresult-citations',
				tagAttr : 'id',
				analyticType : 'unica'
			});
			j$('.page-tools').addTag({tagNode : '#save-this-search-button',tagAttr : 'id',
				analyticType : 'unica'
			});			
			j$('#adv-search-nav').addTag({tagNode : '#popup-search-preferences',tagName : 'preferences',
				analyticType : 'unica'
			});			
			j$('#highlights .pagination li').addTag({tagNode : 'a',tagAttr : 'id',
				eventPrefix : 'highlights',
				analyticType : 'unica'
			});			
			j$('#addToCartSpan').addTag({tagNode : 'input',
				tagAttr : 'id'
			});	
			j$('#relatedContent').addTag({tagNode : 'li',tagType : 'dynamic',tagName : 'relatedContent',
				analyticType : 'unica'
			});
			j$('#advKeywordSrch').addTag({tagName : 'advKeywordSearch',
				tagNode : 'input',
				analyticType : 'unica',
				tagType: 'dynamic'
			});	
			j$('#PubQuickSearch').addTag({tagName : 'advPubQuickSearch',
				analyticType : 'unica',
				tagType: 'dynamic'
			});	
			j$('#advCommandSearch').addTag({tagName : 'advCommandSearch',
				analyticType : 'unica',
				tagType: 'dynamic'
			});	
		});		
	})(jQuery);
