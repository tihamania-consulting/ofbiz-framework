package common;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.category.CategoryContentWrapper;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.base.util.UtilMisc;

catalogName = CatalogWorker.getCatalogName(request);

String productCategoryId = parameters.productCategoryId;
if (UtilValidate.isNotEmpty(productCategoryId))
 {
   GenericValue gvProductCategory =  delegator.findOne("ProductCategory", UtilMisc.toMap("productCategoryId",productCategoryId), true);
   if (UtilValidate.isNotEmpty(gvProductCategory)) 
   {
	    CategoryContentWrapper currentProductCategoryContentWrapper = new CategoryContentWrapper(gvProductCategory, request);
	    context.currentProductCategory = gvProductCategory;
	
	    //set Meta title, Description and Keywords
	    String categoryName = "";
	    categoryName = gvProductCategory.categoryName;
	    
	    if(UtilValidate.isNotEmpty(categoryName)) 
	    {
	        context.metaTitle = categoryName;
	        context.pageTitle = categoryName;
	    }
	    if(UtilValidate.isNotEmpty(gvProductCategory.description)) 
	    {
	        context.metaKeywords = gvProductCategory.description;
	    }
	    if(UtilValidate.isNotEmpty(gvProductCategory.longDescription)) 
	    {
	        context.metaDescription = gvProductCategory.longDescription;
	    }
	    //override Meta title, Description and Keywords
	    String metaTitle = currentProductCategoryContentWrapper.get("HTML_PAGE_TITLE", "string");
	    if(UtilValidate.isNotEmpty(metaTitle)) 
	    {
	        context.metaTitle = metaTitle;
	    }
	    String metaKeywords = currentProductCategoryContentWrapper.get("HTML_PAGE_META_KEY", "string");
	    if(UtilValidate.isNotEmpty(metaKeywords)) 
	    {
	        context.metaKeywords = metaKeywords;
	    }
	    String metaDescription = currentProductCategoryContentWrapper.get("HTML_PAGE_META_DESC", "string");
	    if(UtilValidate.isNotEmpty(metaDescription)) 
	    {
	        context.metaDescription = metaDescription;
	    }
	    
	    String canonicalUrl = currentProductCategoryContentWrapper.get("CANONICAL_URL", "string");
	    if(UtilValidate.isNotEmpty(canonicalUrl)) 
	    {
	        context.canonicalUrl = canonicalUrl;
	    }
   } 
 } 

