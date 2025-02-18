package com.osafe.services;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.ofbiz.solr.SolrUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.content.PartyContentWrapper;
import org.apache.ofbiz.product.category.CategoryContentWrapper;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.product.ProductContentWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.supercsv.cellprocessor.ConvertNullTo;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
//import au.com.bytecode.opencsv.CSVReader;

import com.osafe.solr.SolrConstants;
import com.osafe.util.Util;

public class SolrServices {

    public static final String module = SolrServices.class.getName();

    private static final List<String> indexedLangs = Arrays.asList("ar", "fr", "nl");
    private static final ResourceBundle OSAFE_PROPS = UtilProperties.getResourceBundle("OsafeProperties.xml", Locale.getDefault());
    private  static final String FIELD_NAME_ID = "id";
    private  static final String FIELD_NAME_ROW_TYPE = "rowType";
    private  static final String FIELD_NAME_PRODUCT_ID = "productId";
    private  static final String FIELD_NAME_NAME = "name";
    private  static final String FIELD_NAME_INTERNAL_NAME = "internalName";
    private  static final String FIELD_NAME_DESCRIPTION = "description";
    private  static final String FIELD_NAME_CATEGORY_DESC = "categoryDescription";
    private  static final String FIELD_NAME_CATEGORY_PDP_DESC = "categoryPdpDescription";
    private  static final String FIELD_NAME_CATEGORY_ID = "productCategoryId";
    private  static final String FIELD_NAME_TOP_MOST_CATEGORY_ID = "topMostProductCategoryId";
    private  static final String FIELD_NAME_CATEGORY_LEVEL = "categoryLevel";
    private  static final String FIELD_NAME_CATEGORY_NAME = "categoryName";
    private  static final String FIELD_NAME_CATEGORY_IMAGE_URL = "categoryImageUrl";
    private  static final String FIELD_NAME_IMAGE_SMALL_URL = "productImageSmallUrl";
    private  static final String FIELD_NAME_IMAGE_SMALL_ALT = "productImageSmallAlt";
    private  static final String FIELD_NAME_IMAGE_SMALL_ATL_URL = "productImageSmallAltUrl";
    private  static final String FIELD_NAME_IMAGE_MEDIUM_URL = "productImageMediumUrl";
    private  static final String FIELD_NAME_IMAGE_LARGE_URL = "productImageLargeUrl";
    private  static final String FIELD_NAME_FEATURE_GROUP_ID = "productFeatureGroupId";
    private  static final String FIELD_NAME_FEATURE_GROUP_DESC = "productFeatureGroupDescription";
    private  static final String FIELD_NAME_PRODUCT_FACET_GROUP = "productCategoryFacetGroups";
    private  static final String FIELD_NAME_LIST_PRICE = "listPrice";
    private  static final String FIELD_NAME_PRICE = "price";
    private  static final String FIELD_NAME_RECCURENCE_PRICE = "recurrencePrice";
    private  static final String FIELD_NAME_CUSTOMER_RATING = "customerRating";
    private  static final String FIELD_NAME_SEQ_NUM = "sequenceNum";
    private  static final String FIELD_NAME_TOTAL_TIMES_VIEWED = "totalTimesViewed";
    private  static final String FIELD_NAME_TOTAL_QUANTITY = "totalQuantityOrdered";
    private  static final String FIELD_NAME_PRODUCT_FACILITY_IDS = "productFacilityIds";
    private  static final String FIELD_NAME_INTRO_DATE = "introductionDate";
    private  static final String FIELD_NAME_DISCOTUNIATION_DATE = "salesDiscontinuationDate";
    private  static final String FIELD_NAME_DISCOTUNIATION_DATE_NULL_FLAG = "salesDiscontinuationDateNullFlag";
    private  static final String FIELD_NAME_MANUFACTURER_NAME = "manufacturerName";
    private  static final String FIELD_NAME_MANUFACTURER_ID_NO = "manufacturerIdNo";

    @SuppressWarnings("unchecked")
    public static Map genProductsIndex(DispatchContext dctx, Map context) throws GenericTransactionException 
    {

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String browseRootProductCategoryId = (String) context.get("browseRootProductCategoryId");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        List<SolrInputDocument> documentList = new ArrayList();
        
        List<String> headerColumns = getHeaderColumns();

        List<CellProcessor> cellProcessors = getCellProcesser();
        
        List<String> prodFeatureColNames = new ArrayList();
        ProductContentWrapper productContentWrapper = null;
        CategoryContentWrapper categoryContentWrapper = null;
        try 
        {

            // Find Product Store - to find store's currency setting
            GenericValue productStore = delegator.findOne("ProductStore", UtilMisc.toMap("productStoreId", productStoreId), false);

            List<GenericValue> productFeaturesList = delegator.findList("ProductFeature", null, UtilMisc.toSet("productFeatureId", "productFeatureTypeId"), null, null, false); 
            
            List<GenericValue> productFeatureGroupApplsList = delegator.findList("ProductFeatureGroupAppl", null, UtilMisc.toSet("productFeatureGroupId", "productFeatureId", "fromDate", "thruDate", "sequenceNum"), null, null, false); 
            productFeatureGroupApplsList = EntityUtil.filterByDate(productFeatureGroupApplsList);
            
            List<GenericValue> productFeatureGroupList = delegator.findList("ProductFeatureGroup", null, UtilMisc.toSet("productFeatureGroupId", "description"), null, null, false); 
            
            Set productFeatureCatGrpApplFieldSet = FastSet.newInstance();
            productFeatureCatGrpApplFieldSet.add("productCategoryId");
            productFeatureCatGrpApplFieldSet.add("productFeatureGroupId");
            productFeatureCatGrpApplFieldSet.add("fromDate");
            productFeatureCatGrpApplFieldSet.add("thruDate");
            productFeatureCatGrpApplFieldSet.add("sequenceNum");
            productFeatureCatGrpApplFieldSet.add("facetValueMin");
            productFeatureCatGrpApplFieldSet.add("facetValueMax");
            List<GenericValue> productFeatureCatGrpApplsList = delegator.findList("ProductFeatureCatGrpAppl", null, productFeatureCatGrpApplFieldSet, null, null, false); 
            productFeatureCatGrpApplsList = EntityUtil.filterByDate(productFeatureCatGrpApplsList);

            SolrInputDocument productDocument = null;
            SolrInputDocument productCategoryDocument = null;
            SolrInputDocument facetGroupDocument = null;
            Map<String, Object> results = null;
            String productId = null;
            String productCategoryId = null;
            StringUtil.StringWrapper imageUrl = null;
            String categoryDescription = null;
            Double totalQuantityOrdered = 0.00;
            BigDecimal averageProductRating = BigDecimal.ZERO;
            Long totalTimesViewed = 0L;
            
            String productDocumentId = null;

            // Get all unexpired Product Categories (Top Level Catalog Category)
            List<Map<String, Object>> allUnexpiredCategories = getRelatedCategories(delegator, browseRootProductCategoryId, null, true, true, true);

            // All Sub Categries
            GenericValue workingCategory = null;
            String productCategoryIdPath = null;
            int categoryLevel = 0;
            List<String> categoryTrail = null;
            for (Map<String, Object> workingCategoryMap : allUnexpiredCategories) 
            {
                workingCategory = (GenericValue) workingCategoryMap.get("ProductCategory");

                // Only index products under "Catalog Categories"
                if ("CATALOG_CATEGORY".equals(workingCategory.getString("productCategoryTypeId"))) 
                {
                    // Add "Product Category" SOLR documents
                    productCategoryDocument = new SolrInputDocument();
                    productCategoryId = (String) workingCategory.getString("productCategoryId");
                    productCategoryDocument.setField(FIELD_NAME_ID, SolrConstants.ROW_TYPE_PRODUCT_CATEGORY + "_" + productCategoryId);
                    productCategoryDocument.setField(FIELD_NAME_ROW_TYPE, SolrConstants.ROW_TYPE_PRODUCT_CATEGORY);
                    categoryContentWrapper = new CategoryContentWrapper(dispatcher, workingCategory, locale, "text/html");

                    categoryTrail = (List<String>) workingCategoryMap.get("categoryTrail");
                    categoryLevel = categoryTrail.size() - 1;
                    productCategoryIdPath = StringUtils.join(categoryTrail, " ");
                    productCategoryDocument.setField(FIELD_NAME_CATEGORY_ID, productCategoryIdPath);
                    productCategoryDocument.setField(FIELD_NAME_CATEGORY_LEVEL, categoryLevel);
                    productCategoryDocument.setField(FIELD_NAME_CATEGORY_NAME, workingCategory.getString("categoryName"));
                    for (String lang: indexedLangs) {
                        String TransCategoryName = CategoryContentWrapper.getProductCategoryContentAsText(workingCategory, "CATEGORY_NAME", new Locale(lang), dispatcher, "html");
                        if (UtilValidate.isNotEmpty(TransCategoryName)) {
                            productCategoryDocument.setField(getTranslationField(FIELD_NAME_CATEGORY_NAME, lang), TransCategoryName);
                        }
                    }
                    String categoryImageUrl = workingCategory.getString("categoryImageUrl");
                    if (UtilValidate.isNotEmpty(categoryImageUrl)) 
                    {
                        productCategoryDocument.setField(FIELD_NAME_CATEGORY_IMAGE_URL, categoryImageUrl);
                    }
                    // Category_DESCRIPTION
                    categoryDescription = workingCategory.getString("longDescription");
                    if (UtilValidate.isNotEmpty(categoryDescription) && !"null".equalsIgnoreCase(categoryDescription)) 
                    {
                        productCategoryDocument.setField(FIELD_NAME_CATEGORY_DESC, categoryDescription.toString());
                    }

                    documentList.add(productCategoryDocument);

                    // For each category get all products
                    List<GenericValue> productCategoryMembers = workingCategory.getRelated("ProductCategoryMember");
                    productCategoryMembers = EntityUtil.orderBy(productCategoryMembers,UtilMisc.toList("sequenceNum"));

                    // Remove any expired
                    productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    
                    for (GenericValue productCategoryMember : productCategoryMembers) 
                    {
                        GenericValue product = productCategoryMember.getRelatedOne("Product");
                        if (UtilValidate.isNotEmpty(product)) 
                        {
                            String isVariant = product.getString("isVariant");
                            if (UtilValidate.isEmpty(isVariant)) 
                            {
                                isVariant = "N";
                            }
                            // All Non-Variant Products
                            if ("N".equals(isVariant)) 
                            {

                                if (ProductWorker.isSellable(product)) 
                                {
                                    productContentWrapper = new ProductContentWrapper(dispatcher, product, new Locale("en"), "text/html");
                                    productDocument = new SolrInputDocument();
                                    productId = product.getString("productId");
                                    productDocumentId = SolrConstants.ROW_TYPE_PRODUCT + "_" + productId;
                                    productDocument.setField(FIELD_NAME_ID, productDocumentId);
                                    productDocument.setField(FIELD_NAME_PRODUCT_ID, productId);
                                    productDocument.setField(FIELD_NAME_ROW_TYPE, SolrConstants.ROW_TYPE_PRODUCT);
                                    productDocument.setField(FIELD_NAME_NAME, productContentWrapper.get("PRODUCT_NAME", "html").toString());
                                    for (String lang : indexedLangs) {

                                        String transProductName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", new Locale(lang), dispatcher , "string").toString();
                                        if (UtilValidate.isNotEmpty(transProductName)) {
                                            productDocument.setField(getTranslationField(FIELD_NAME_NAME, lang), transProductName);
                                        }
                                    }

                                    productDocument.setField(FIELD_NAME_INTERNAL_NAME,  product.getString("internalName"));

                                    GenericValue goodIdentification = delegator.findOne("GoodIdentification", UtilMisc.toMap("productId", productId, "goodIdentificationTypeId", "MANUFACTURER_ID_NO"));
                                    if(UtilValidate.isNotEmpty(goodIdentification))
                                    {
                                    	productDocument.setField(FIELD_NAME_MANUFACTURER_ID_NO, goodIdentification.getString("idValue"));
                                    }
                                    
                                    String manufacturerPartyId = product.getString("manufacturerPartyId");
                                    if(UtilValidate.isNotEmpty(manufacturerPartyId))
                                    {
                                    	GenericValue manufacturerParty = delegator.findOne("Party", UtilMisc.toMap("partyId", manufacturerPartyId));
                                    	if(UtilValidate.isNotEmpty(manufacturerParty))
                                    	{
                                    		PartyContentWrapper partyContentWrapper = new PartyContentWrapper(dispatcher, manufacturerParty, locale, "text/html");
                                    		productDocument.setField(FIELD_NAME_MANUFACTURER_NAME,  partyContentWrapper.get("PROFILE_NAME", "html").toString());
                                    	}
                                    }
                                    
                                    if(UtilValidate.isNotEmpty(product.getTimestamp("introductionDate")))
                                    {
                                    	productDocument.setField(FIELD_NAME_INTRO_DATE,  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(product.getTimestamp("introductionDate").getTime())));
                                    }
                                    
                                    productDocument.setField(FIELD_NAME_DISCOTUNIATION_DATE_NULL_FLAG, 0);
                                    Timestamp salesDiscoDateTs = product.getTimestamp("salesDiscontinuationDate");
                                    if(UtilValidate.isNotEmpty(salesDiscoDateTs))
                                    {
                                    	productDocument.setField(FIELD_NAME_DISCOTUNIATION_DATE , new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(salesDiscoDateTs.getTime())));
                                    	productDocument.setField(FIELD_NAME_DISCOTUNIATION_DATE_NULL_FLAG, 1);
                                    }
                                    String SORT_OPTIONS  = Util.getProductStoreParm(productStoreId, "PLP_AVAILABLE_SORT");
                                    if(UtilValidate.isNotEmpty(SORT_OPTIONS) && SORT_OPTIONS.contains("DISCO_DATE"))
                                    {
	                                    //if virtual then attempt to override disco date with variant disco date
	                                    Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
	                                    if(UtilValidate.isEmpty(salesDiscoDateTs) || (UtilValidate.isNotEmpty(salesDiscoDateTs) && !salesDiscoDateTs.before(nowTimestamp)))
	                                    {
		                                    String isVirtual = product.getString("isVirtual");
		                                    if ("Y".equals(isVirtual)) 
		                                    {
		                                    	List<GenericValue> productAssocVariantList = product.getRelated("MainProductAssoc");
		                                    	productAssocVariantList = EntityUtil.filterByAnd(productAssocVariantList, UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT")); 
		                                    	List<GenericValue> assocProductVariantList = EntityUtil.getRelated("AssocProduct", productAssocVariantList);
		                                    	assocProductVariantList = EntityUtil.orderBy(assocProductVariantList, UtilMisc.toList("salesDiscontinuationDate ASC"));
		                                    	for(GenericValue productVariant : assocProductVariantList)
		                                    	{
	                                    			if(UtilValidate.isNotEmpty(productVariant.getTimestamp("salesDiscontinuationDate")))
	                                                {
	                                    				if(!productVariant.getTimestamp("salesDiscontinuationDate").before(nowTimestamp))
	                                    				{
	                                    					if(UtilValidate.isEmpty(salesDiscoDateTs))
	                                                        {
	                                    						salesDiscoDateTs = productVariant.getTimestamp("salesDiscontinuationDate");
	                                    						productDocument.setField(FIELD_NAME_DISCOTUNIATION_DATE ,new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(salesDiscoDateTs.getTime())));
			                                                	productDocument.setField(FIELD_NAME_DISCOTUNIATION_DATE_NULL_FLAG,1);
	                                                        }
	                                    					else if(UtilValidate.isNotEmpty(salesDiscoDateTs) && salesDiscoDateTs.after(productVariant.getTimestamp("salesDiscontinuationDate")))
		                                    				{
	                                    						salesDiscoDateTs = productVariant.getTimestamp("salesDiscontinuationDate");
			                                                	productDocument.setField(FIELD_NAME_DISCOTUNIATION_DATE ,new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(salesDiscoDateTs.getTime())));
			                                                	productDocument.setField(FIELD_NAME_DISCOTUNIATION_DATE_NULL_FLAG,1);
		                                    				}
	                                    					break;
	                                    				}
	                                                }
		                                    	}
		                                    }
	                                    }
                                    }
                                    
                                    productDocument.setField(FIELD_NAME_SEQ_NUM ,productCategoryMember.getString("sequenceNum"));
                                    productDocument.setField(FIELD_NAME_CATEGORY_NAME , workingCategory.getString("categoryName"));
                                    for (String lang: indexedLangs) {
                                        String TransCategoryName = CategoryContentWrapper.getProductCategoryContentAsText(workingCategory, "CATEGORY_NAME", new Locale(lang), dispatcher, "html");
                                        if (UtilValidate.isNotEmpty(TransCategoryName)) {
                                            productDocument.setField(getTranslationField(FIELD_NAME_CATEGORY_NAME, lang), TransCategoryName);
                                        }
                                    }


                                    if (UtilValidate.isNotEmpty(categoryDescription) && !"null".equalsIgnoreCase(categoryDescription.toString())) 
                                    {
                                    	productDocument.setField(FIELD_NAME_CATEGORY_DESC ,categoryDescription.toString());
                                    }
                                    // LONG_DESCRIPTION
                                    String longDescription = ProductContentWrapper.getProductContentAsText(product, "LONG_DESCRIPTION", new Locale("en"), dispatcher , "string");
                                    if (UtilValidate.isNotEmpty(longDescription))
                                    {
                                        productDocument.setField(FIELD_NAME_DESCRIPTION ,longDescription);
                                    }

                                    for (String lang: indexedLangs) {
                                        String transLongDescription = ProductContentWrapper.getProductContentAsText(product, "LONG_DESCRIPTION", new Locale(lang), dispatcher , "string");

                                        if (UtilValidate.isNotEmpty(transLongDescription))
                                        {
                                            productDocument.setField(getTranslationField(FIELD_NAME_DESCRIPTION,  lang),transLongDescription);
                                        }
                                    }



                                    // SMALL_IMAGE_URL
                                    imageUrl = productContentWrapper.get("SMALL_IMAGE_URL", "string");
                                    if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
                                    {
                                        productDocument.setField(FIELD_NAME_IMAGE_SMALL_URL, imageUrl.toString());
                                    }

                                    // SMALL_IMAGE_ALT
                                    imageUrl = productContentWrapper.get("SMALL_IMAGE_ALT", "string");
                                    if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
                                    {
                                        productDocument.setField(FIELD_NAME_IMAGE_SMALL_ALT, imageUrl.toString());
                                    }

                                    // SMALL_IMAGE_ALT_URL
                                    imageUrl = productContentWrapper.get("SMALL_IMAGE_ALT_URL", "string");
                                    if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
                                    {
                                    	if (UtilValidate.isNotEmpty(imageUrl.toString()))
                                    	{
                                            productDocument.setField(FIELD_NAME_IMAGE_SMALL_ATL_URL, imageUrl.toString());
                                    	}
                                    }
                                    // MEDIUM_IMAGE_URL
                                    imageUrl = productContentWrapper.get("MEDIUM_IMAGE_URL", "string");
                                    if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
                                    {
                                        productDocument.setField(FIELD_NAME_IMAGE_MEDIUM_URL, imageUrl.toString());
                                    }

                                    // LARGE_IMAGE_URL
                                    imageUrl = productContentWrapper.get("LARGE_IMAGE_URL", "string");
                                    if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
                                    {
                                        productDocument.setField(FIELD_NAME_IMAGE_LARGE_URL, imageUrl.toString());
                                    }

                                    results = dispatcher.runSync("getProductFeaturesByType", UtilMisc.toMap("productId", productId));
                                    List<String> productFeatureTypes = (List<String>) results.get("productFeatureTypes");

                                    // for each new feature type add a column heading
                                    // ex. COLOR SIZE

                                    Map<String, GenericValue> productFeaturesByType = (Map<String, GenericValue>) results.get("productFeaturesByType");
                                    for (String productFeatureType : productFeatureTypes) 
                                    {

                                        List<GenericValue> productFeatures = (List<GenericValue>) productFeaturesByType.get(productFeatureType);

                                        try 
                                        {
                                        	
                                            Map<String, String> featureValuesMap = FastMap.newInstance();
                                            List<String> featureValues = new ArrayList();
                                            try 
                                            {
                                                for (GenericValue feature : productFeatures) 
                                                {
                                                	String description ="";
                                                	String productFeatureApplTypeId = feature.getString("productFeatureApplTypeId");
                                                	List<GenericValue> productFeatureGroupAppls = EntityUtil.filterByAnd(productFeatureGroupApplsList, UtilMisc.toMap("productFeatureId",feature.getString("productFeatureId")));
                                                	//List<GenericValue> productFeatureGroupAppls = delegator.findByAnd("ProductFeatureGroupAppl", UtilMisc.toMap("productFeatureId",feature.getString("productFeatureId")));
                                                	//productFeatureGroupAppls = EntityUtil.filterByDate(productFeatureGroupAppls);
                                                	if(productFeatureGroupAppls.size() > 0) 
                                                	{
                                                		//Issue 38916
                            		                    description = feature.getString("description");
                            		                	if (UtilValidate.isNotEmpty(description))
                            		                	{
                            	                            String forceCase = Util.getProductStoreParm(delegator, productStoreId, "FACET_VALUE_FORCE_CASE");
                                		                	if (UtilValidate.isNotEmpty(forceCase))
                                		                	{
                                	                            if ("UPPER".equals(forceCase.trim().toUpperCase()))
                                	                            {
                                	    		                    description = description.toUpperCase();
                                	                            }
                                	                            else if ("LOWER".equals(forceCase.trim().toUpperCase()))
                                	                            {
                                	    		                    description = description.toLowerCase();
                                	                            	
                                	                            }
                                		                		
                                		                	}
                            		                	}
                                                	}
                                                    //Commented out block
                                                    //issue #25879
                                                    //implemented to meet a specific requirement for GMH Production by srufle.
                                                    //Not needed anymore, need to insure GMH is still working after comment
                                                    if ("SELECTABLE_FEATURE".equals(productFeatureApplTypeId)) 
                                                    {
                                                        if (description.contains("/")) 
                                                        {
                                                            // Split descriptions for SELECTABLE_FEATURE items that have a "/" in them
                                                            // Ex.Off Black/Burgundy would be split in to seperate facets for "Off Black"
                                                            // and
                                                            // "Burgundy"
                                                            String[] descriptionParts = StringUtils.split(description, "/");
                                                            for (String descPart : descriptionParts) 
                                                            {
                                                                descPart = StringUtils.trim(descPart);
                                                                featureValuesMap.put(descPart, descPart);
                                                            }
                                                            continue;
                                                        }
                                                    }
                                                    try 
                                                    {
                                                    	if (UtilValidate.isNotEmpty(description))
                                                    	{
                                                            featureValuesMap.put(description, description);
                                                    		
                                                    	}
                                                    }
                                                    catch (Exception eee) 
                                                    {
                                                    	Debug.logError(eee, "h" + eee.getMessage()+ description, module);
                                                    }
                                                }
                                            }
                                            catch (Exception eee) 
                                            {
                                            	Debug.logError(eee, eee.getMessage(), module);
                                            }
                                            Set<Entry<String, String>> featureValuesEntrySet = featureValuesMap.entrySet();
                                            Iterator<Entry<String, String>> featureValuesIterator = featureValuesEntrySet.iterator();
                                            while (featureValuesIterator.hasNext()) 
                                            {
                                                Map.Entry<String, String> featureValueEntry = (Map.Entry<String, String>) featureValuesIterator.next();
                                                featureValues.add((String) featureValueEntry.getKey());
                                            }

                                            // Need to replace spaces in decription with underscore symbol "_"
                                            for (int i = 0; i < featureValues.size(); i++) 
                                            {
                                                String val = featureValues.get(i);
                                                val = URLEncoder.encode(StringUtils.replace(val, " ", "_"), SolrConstants.DEFAULT_ENCODING);
                                                featureValues.set(i, val);
                                            }

                                            productDocument.setField(productFeatureType, StringUtils.join(featureValues, " "));

                                        }
                                         catch (Exception ee)
                                         {
                                        	 Debug.logError(ee, ee.getMessage(),module);
                                         }
                                        // ex. Red Small
                                    }

                                    // Product Prices
                                    String currencyUomId = productStore.getString("defaultCurrencyUomId");
                                    results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product, "currencyUomId", currencyUomId));
                                    if (results.get("listPrice")!= null)
                                        productDocument.setField(FIELD_NAME_LIST_PRICE, ((BigDecimal) results.get("listPrice")).floatValue());
                                    if (results.get("price")!= null)
                                        productDocument.setField(FIELD_NAME_PRICE, ((BigDecimal) results.get("price")).floatValue());

                                    // Product RECURRENCE Prices
                                    results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product, "currencyUomId", currencyUomId,"productPricePurposeId","RECURRING_CHARGE"));
                                    if (results.get("price")!= null)
                                        productDocument.setField(FIELD_NAME_RECCURENCE_PRICE, ((BigDecimal) results.get("price")).floatValue());

                                    GenericValue ProductCalculatedInfo = delegator.findOne("ProductCalculatedInfo", UtilMisc.toMap("productId", productId), false);
                                    // Product Ratings
                                    if(UtilValidate.isNotEmpty(ProductCalculatedInfo) && UtilValidate.isNotEmpty(ProductCalculatedInfo.getBigDecimal("averageCustomerRating")))
                                    {
                                        averageProductRating = ProductCalculatedInfo.getBigDecimal("averageCustomerRating");
                                    } 
                                    else 
                                    {
                                        averageProductRating = BigDecimal.ZERO;
                                    }
                                    productDocument.setField(FIELD_NAME_CUSTOMER_RATING, averageProductRating.floatValue());

                                    // Product Quantity Ordered
                                   if(UtilValidate.isNotEmpty(ProductCalculatedInfo) && ProductCalculatedInfo.getDouble("totalQuantityOrdered")!= null)
                                   {
                                       totalQuantityOrdered = ProductCalculatedInfo.getDouble("totalQuantityOrdered");
                                   }
                                   else
                                   {
                                       totalQuantityOrdered = 0.00;
                                   }
                                   productDocument.setField(FIELD_NAME_TOTAL_QUANTITY, totalQuantityOrdered.floatValue());
                                   // Product View Count
                                   if(UtilValidate.isNotEmpty(ProductCalculatedInfo) && ProductCalculatedInfo.getLong("totalTimesViewed")!= null)
                                   {
                                	   totalTimesViewed = ProductCalculatedInfo.getLong("totalTimesViewed");
                                   }
                                   else
                                   {
                                       totalTimesViewed = 0L;
                                   }
                                   productDocument.setField(FIELD_NAME_TOTAL_TIMES_VIEWED, totalTimesViewed);

                                   // Product Facilities
                                   List<GenericValue>  productFacilityList = delegator.findList("ProductFacility", EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId), UtilMisc.toSet("facilityId"), null, null, false);
                                   List productFacilityIdList = new ArrayList();
                                   for (GenericValue productFacility : productFacilityList)
                                   {
                                	   productFacilityIdList.add(productFacility.getString("facilityId"));
                                   }
                                   String productFacilityIds = StringUtils.join(productFacilityIdList, " ");
                                   productDocument.setField(FIELD_NAME_PRODUCT_FACILITY_IDS, productFacilityIds);

                                    
                                   List<GenericValue> rollups = null;

                                   rollups = delegator.findByAnd("ProductCategoryRollup", UtilMisc.toMap("productCategoryId", productCategoryMember.getString("productCategoryId")), UtilMisc.toList("sequenceNum"));
                                   rollups = EntityUtil.filterByDate(rollups);
                                   
                                    GenericValue gvTopMostCategory = null;
                                    for(GenericValue rollUp : rollups)
                                    {
                                    	gvTopMostCategory = rollUp.getRelatedOne("ParentProductCategory");
                                    	if(categoryTrail.contains(gvTopMostCategory.getString("productCategoryId")))
                                    	{
                                    	    break;
                                    	}
                                    }
                                    
                                    if (UtilValidate.isNotEmpty(gvTopMostCategory)) 
                                    {
                                        String topMostProductCategoryId = gvTopMostCategory.getString("productCategoryId");
                                        productDocumentId = SolrConstants.ROW_TYPE_PRODUCT + "_" + productId + "_" + topMostProductCategoryId + "_" + productCategoryMember.getString("productCategoryId");
                                        productDocument.setField(FIELD_NAME_ID, productDocumentId);
                                        productDocument.setField(FIELD_NAME_CATEGORY_ID, productCategoryMember.getString("productCategoryId"));
                                        productDocument.setField(FIELD_NAME_TOP_MOST_CATEGORY_ID, topMostProductCategoryId);
                                    }

                                    // Find "Facet Groups" available for each "Product Category"
                                    List<GenericValue> currentProductCategories = ProductWorker.getCurrentProductCategories(product);
                                    
                                    Map<String, String> productFeatureTypeMap = null;
                                    for (GenericValue productCategory : currentProductCategories) 
                                    {

                                        productCategoryId = productCategory.getString("productCategoryId");
                                        
                                        List<GenericValue> productFeatureCatGrpAppls = EntityUtil.filterByAnd(productFeatureCatGrpApplsList, UtilMisc.toMap("productCategoryId", productCategory.getString("productCategoryId")));
                                        productFeatureCatGrpAppls = EntityUtil.orderBy(productFeatureCatGrpAppls, UtilMisc.toList("sequenceNum"));

                                        for (GenericValue productFeatureCatGrpAppl : productFeatureCatGrpAppls) 
                                        {
                                            productFeatureTypeMap = FastMap.newInstance();
                                            
                                            GenericValue productFeatureGroup = EntityUtil.getFirst(EntityUtil.filterByAnd(productFeatureGroupList, UtilMisc.toMap("productFeatureGroupId", productFeatureCatGrpAppl.getString("productFeatureGroupId"))));

                                            String productFeatureGroupDescription = productFeatureGroup.getString("description");
                                            String productFeatureGroupId = productFeatureGroup.getString("productFeatureGroupId");
                                            
                                            Long facetValueMin = productFeatureCatGrpAppl.getLong("facetValueMin");
                                            Long facetValueMax = productFeatureCatGrpAppl.getLong("facetValueMax");
                                            try 
                                            {
                                                if (UtilValidate.isEmpty(facetValueMin)) 
                                                {
                                                	
                                                    String xProductStorePramFaceValueMin = Util.getProductStoreParm(delegator, productStoreId, "FACET_VALUE_MIN");
                                                    if (UtilValidate.isNotEmpty(xProductStorePramFaceValueMin)) 
                                                    {
                                                    	facetValueMin = Long.parseLong(xProductStorePramFaceValueMin);
                                                    }
                                                }
                                                if (UtilValidate.isEmpty(facetValueMax)) 
                                                {
                                                    String xProductStorePramFaceValueMax = Util.getProductStoreParm(delegator, productStoreId, "FACET_VALUE_MAX");
                                                    if (UtilValidate.isNotEmpty(xProductStorePramFaceValueMax)) 
                                                    {
                                                        facetValueMax = Long.parseLong(xProductStorePramFaceValueMax);
                                                    }
                                                }
                                            } 
                                            catch (NumberFormatException nfe) 
                                            {
                                                facetValueMax = 0L;
                                                facetValueMin = 0L;
                                                Debug.logError(nfe, nfe.getMessage(), module);
                                            }

                                            List<GenericValue> productFeatureGroupAppls = EntityUtil.filterByAnd(productFeatureGroupApplsList, UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId)); 

                                            if(productFeatureGroupAppls.size() > 0) 
                                            {
                                            	facetGroupDocument = new SolrInputDocument();
                                                facetGroupDocument.setField(FIELD_NAME_ID, SolrConstants.ROW_TYPE_FACET_GROUP + "_" + productCategoryId + "_" + productFeatureGroupId);
                                                facetGroupDocument.setField(FIELD_NAME_ROW_TYPE, SolrConstants.ROW_TYPE_FACET_GROUP);
                                                facetGroupDocument.setField(FIELD_NAME_CATEGORY_ID, productCategoryId);
                                                //facetGroupDocument.setProductFeatureGroupFacetValueMin(facetValueMin);
                                                //facetGroupDocument.setProductFeatureGroupFacetValueMax(facetValueMax);
                                                facetGroupDocument.setField(FIELD_NAME_FEATURE_GROUP_DESC, productFeatureGroupDescription);
                                                facetGroupDocument.setField(FIELD_NAME_FEATURE_GROUP_ID, productFeatureGroupId);

                                                productFeatureGroupAppls = EntityUtil.orderBy(productFeatureGroupAppls, UtilMisc.toList("sequenceNum"));
                                                for (GenericValue productFeatureGroupAppl : productFeatureGroupAppls) 
                                                {
                                                    GenericValue productFeature = EntityUtil.getFirst(EntityUtil.filterByAnd(productFeaturesList, UtilMisc.toMap("productFeatureId", productFeatureGroupAppl.getString("productFeatureId"))));
                                                    String productFeatureTypeId = productFeature.getString("productFeatureTypeId");
                                                    String key = productFeatureTypeId;
                                                    productFeatureTypeMap.put(key, productFeatureTypeId);
                                                }

                                                String productFeatureTypeIds = StringUtils.join(UtilMisc.toList(productFeatureTypeMap.keySet()), " ");

                                                facetGroupDocument.setField(FIELD_NAME_PRODUCT_FACET_GROUP,productFeatureTypeIds);
                                                documentList.add(facetGroupDocument);
                                            }
                                            
                                        }
                                    }
                                    documentList.add(productDocument);
                                }

                            }
                        }
                    }

                }

            }

            if (UtilValidate.isNotEmpty(documentList)) 
            {

                // Delete previous index using Http Client
                HttpSolrClient updateClient = SolrUtil.getHttpSolrClient("solrdefault");

                updateClient.deleteByQuery("*:*");

                //Saving the document
                updateClient.commit();

                updateClient.add(documentList);
                updateClient.commit();
            }
        } 
        catch (Exception e) 
        {
            Debug.logError(e, e.getMessage(), module);
        }

        if (documentList == null) 
        {
            documentList = new ArrayList();
        }
        result.put("documentListCount", documentList.size());

        return result;
    }

    private static String getTranslationField(String fieldNameCategoryName, String lang) {
        return fieldNameCategoryName + StringUtils.capitalize(lang);
    }

    private static List<CellProcessor> getCellProcesser() {
		List<CellProcessor> cellProcessors = new ArrayList();
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(null), new ConvertNullTo("product"), new ConvertNullTo(""), new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""), new ConvertNullTo("")));
		cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""),new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""), new ConvertNullTo(""), new ConvertNullTo(""), new ConvertNullTo(""), new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""),new ConvertNullTo(""), new ConvertNullTo(""), new ConvertNullTo(""), new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""), new ConvertNullTo(""), new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""), new ConvertNullTo(""), new ConvertNullTo(""),new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""), new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""), new ConvertNullTo("")));
        cellProcessors.addAll(UtilMisc.toList(new ConvertNullTo(""), new ConvertNullTo("")));
		return cellProcessors;
	}

	private static List<String> getHeaderColumns() {
		List<String> headerColumns = new ArrayList();
        headerColumns.addAll(UtilMisc.toList("id", "rowType", "productId", "name"));
        headerColumns.addAll(UtilMisc.toList("internalName", "description"));
        headerColumns.addAll(UtilMisc.toList("categoryDescription","categoryPdpDescription"));
        headerColumns.addAll(UtilMisc.toList("productCategoryId", "topMostProductCategoryId", "categoryLevel", "categoryName", "categoryImageUrl"));
        headerColumns.addAll(UtilMisc.toList("productImageSmallUrl", "productImageSmallAlt","productImageSmallAltUrl","productImageMediumUrl", "productImageLargeUrl"));
        headerColumns.addAll(UtilMisc.toList("productFeatureGroupId", "productFeatureGroupDescription","productCategoryFacetGroups"));
        headerColumns.addAll(UtilMisc.toList("listPrice","price", "customerRating","sequenceNum"));
        headerColumns.addAll(UtilMisc.toList("totalTimesViewed","totalQuantityOrdered"));
        headerColumns.addAll(UtilMisc.toList("productFacilityIds"));
        headerColumns.addAll(UtilMisc.toList("introductionDate"));
        headerColumns.addAll(UtilMisc.toList("salesDiscontinuationDate", "salesDiscontinuationDateNullFlag"));
        headerColumns.addAll(UtilMisc.toList("manufacturerName", "manufacturerIdNo"));
		return headerColumns;
	}

    private static List<Map<String, Object>> getRelatedCategories(Delegator delegator, String parentId, List<String> categoryTrail, boolean limitView, boolean excludeEmpty, boolean recursive) 
    {
        List<Map<String, Object>> categories = new ArrayList();
        if (categoryTrail == null) 
        {
            categoryTrail = new ArrayList();
        }
        categoryTrail.add(parentId);
        if (Debug.verboseOn())
        {
        	Debug.logVerbose("[SolrServices.getRelatedCategories] ParentID: " + parentId, module);
        }

        List<GenericValue> rollups = null;

        try 
        {
            rollups = delegator.findByAnd("ProductCategoryRollup", UtilMisc.toMap("parentProductCategoryId", parentId), UtilMisc.toList("sequenceNum"));
            if (limitView) 
            {
                rollups = EntityUtil.filterByDate(rollups, true);
            }
        } 
        catch (GenericEntityException e) 
        {
            Debug.logWarning(e.getMessage(), module);
        }
        if (rollups != null) 
        {
            // Debug.log("Rollup size: " + rollups.size(), module);
            for (GenericValue parent : rollups) 
            {
                // Debug.log("Adding child of: " +
                // parent.getString("parentProductCategoryId"), module);
                GenericValue cv = null;
                Map<String, Object> cvMap = FastMap.newInstance();

                try 
                {
                    cv = parent.getRelatedOne("CurrentProductCategory");
                } 
                catch (GenericEntityException e) 
                {
                    Debug.logWarning(e.getMessage(), module);
                }
                if (cv != null) 
                {

                    if (excludeEmpty) 
                    {
                        if (!CategoryWorker.isCategoryEmpty(cv)) 
                        {
                            cvMap.put("ProductCategory", cv);
                            categories.add(cvMap);
                            if (recursive) 
                            {
                                categories.addAll(getRelatedCategories(delegator, cv.getString("productCategoryId"), categoryTrail, limitView, excludeEmpty, recursive));
                            }
                            List<String> popList = new ArrayList();
                            popList.addAll(categoryTrail);
                            cvMap.put("categoryTrail", popList);
                            categoryTrail.remove(categoryTrail.size() - 1);
                        }
                    }
                    else 
                    {
                        cvMap.put("ProductCategory", cv);
                        cvMap.put("parentProductCategoryId", parent.getString("parentProductCategoryId"));
                        categories.add(cvMap);
                        if (recursive) 
                        {
                            categories.addAll(getRelatedCategories(delegator, cv.getString("productCategoryId"), categoryTrail, limitView, excludeEmpty, recursive));
                        }
                        List<String> popList = new ArrayList();
                        popList.addAll(categoryTrail);
                        cvMap.put("categoryTrail", popList);
                        categoryTrail.remove(categoryTrail.size() - 1);
                    }
                }
            }
        }
        return categories;
    }

    private static GenericValue getTopMostParentProductCategory(Delegator delegator, String productCategoryId, String browseRootProductCategoryId) 
    {
        GenericValue gvTopMost = null;
        if (Debug.verboseOn())
        {
            Debug.logVerbose("[SolrServices.getTopParentProductCategory] productCategoryId: " + productCategoryId + ", browseRootProductCategoryId: " + browseRootProductCategoryId, module);
        }
        List<GenericValue> rollups = null;

        try 
        {
            rollups = delegator.findByAnd("ProductCategoryRollup", UtilMisc.toMap("productCategoryId", productCategoryId), UtilMisc.toList("sequenceNum"));
            rollups = EntityUtil.filterByDate(rollups, true);
        } 
        catch (GenericEntityException e) 
        {
            Debug.logWarning(e.getMessage(), module);
        }
        String parentProductCategoryId = null;
        if (rollups != null) 
        {
            for (GenericValue child : rollups) 
            {
                parentProductCategoryId = child.getString("parentProductCategoryId");
                if (parentProductCategoryId.equals(browseRootProductCategoryId)) 
                {
                    return child;
                } 
                else 
                {
                    GenericValue topMostParentRollup = getTopMostParentProductCategory(delegator, parentProductCategoryId, browseRootProductCategoryId);
                    if (UtilValidate.isNotEmpty(topMostParentRollup)) 
                    {
                        try 
                        {
                            gvTopMost = topMostParentRollup.getRelatedOne("CurrentProductCategory");
                        } 
                        catch (GenericEntityException e) 
                        {
                            Debug.logWarning(e.getMessage(), module);
                        }
                    }
                }
            }
        }

        return gvTopMost;
    }
    /**
     * service for update the schema.xml file in solr .service remove the field element of fields parent element.
     * For append the field element which treat as constant, 
     * service import these element from schema-ConstantField.xml from location same as schema.xml
     * Dynamic field element get by ProductFeatureType entity, 
     * value of attributes of dynamic field elements are fix 
     * and service retrive these keys, values from com.osafe.solr.SolrConstants java file
     */
    public static Map<String, Object> updateSolrSchemaXml(DispatchContext ctx, Map<String, ?> context) 
    {

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resp = null;
        int addFeatureElementCount = 0;
        InputStream ins = null, insc = null;
        OutputStream os = null;
        Document xmlDocument = null;

        String schemaXmlFilePath = FlexibleStringExpander.expandString(OSAFE_PROPS.getString("solrSchemaXmlFile"), context);
        String schemaConstantXmlFilePath = FlexibleStringExpander.expandString(OSAFE_PROPS.getString("solrSchemaConstantFieldXmlFile"), context);

        if (UtilValidate.isNotEmpty(schemaXmlFilePath) && UtilValidate.isNotEmpty(schemaConstantXmlFilePath)) 
        {

            URL schemaXmlFileUrl = UtilURL.fromFilename(schemaXmlFilePath);
            URL schemaConstantXmlFileUrl = UtilURL.fromFilename(schemaConstantXmlFilePath);

            try 
            {
                if (schemaXmlFileUrl  != null) ins = schemaXmlFileUrl.openStream();
                if (schemaConstantXmlFileUrl  != null) insc = schemaConstantXmlFileUrl.openStream();

                if (ins != null && insc != null) 
                {
                    xmlDocument = UtilXml.readXmlDocument(ins, schemaXmlFileUrl.toString());
                    // ##############################################
                    // remove the field element and copyField element
                    // ##############################################
                    Node parentFieldNode = removeNode(xmlDocument, "field");
                    Node parentCopyFieldNode = removeNode(xmlDocument, "copyField");
                    // ###################################################
                    // append constant field element and copyField element  
                    // which import from schema-ConstantField.xml
                    // ###################################################
                    Document xmlConstantDocument = UtilXml.readXmlDocument(insc, schemaConstantXmlFileUrl.toString());
                    importNode(xmlConstantDocument, parentFieldNode, "field");
                    importNode(xmlConstantDocument, parentCopyFieldNode, "copyField");

                    // ##################################################
                    // append dynamic field element and copyField element
                    // from ProductFeatureType entity
                    // ##################################################
                    if (parentFieldNode!= null && parentCopyFieldNode!= null) 
                    {
                        List<GenericValue>  productFeatureTypeIdList=delegator.findByAnd("ProductFeatureType", UtilMisc.toMap());
                        for (GenericValue productFeatureType: productFeatureTypeIdList) 
                        {
                            addFeatureElementCount++;
                            //create dynamic field element with their attributes
                            Element newFeatureElement = UtilXml.addChildElement((Element)parentFieldNode, "field", xmlDocument);
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_NAME_ATTR, productFeatureType.get("productFeatureTypeId").toString());
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_INDEXED_ATTR, SolrConstants.SCHEMA_PRODUCT_FEATURE_INDEXED_VALUE);
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_MULTIVALUED_ATTR, SolrConstants.SCHEMA_PRODUCT_FEATURE_MULTIVALUED_VALUE);
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_OMITNORMS_ATTR, SolrConstants.SCHEMA_PRODUCT_FEATURE_OMITNORMS_VALUE);
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_REQUIRED_ATTR, SolrConstants.SCHEMA_PRODUCT_FEATURE_REQUIRED_VALUE);
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_STORED_ATTR, SolrConstants.SCHEMA_PRODUCT_FEATURE_STORED_VALUE);
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_TYPE_ATTR, SolrConstants.SCHEMA_PRODUCT_FEATURE_TYPE_VALUE);

                            //create dynamic copyField element with their attributes
                            newFeatureElement = UtilXml.addChildElement((Element)parentCopyFieldNode, "copyField", xmlDocument);
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_SOURCE_ATTR, productFeatureType.get("productFeatureTypeId").toString());
                            newFeatureElement.setAttribute(SolrConstants.SCHEMA_PRODUCT_FEATURE_DEST_ATTR, SolrConstants.SCHEMA_PRODUCT_FEATURE_DEST_VALUE);
                        }
                    }
                }
                else 
                {
                    resp = ServiceUtil.returnFailure();
                }
            } 
            catch (IOException ioe) 
            {
                Debug.logError(ioe, module);
                resp = ServiceUtil.returnFailure();
                xmlDocument = null;
            }
            catch (Exception exc) 
            {
                Debug.logError(exc, module);
                resp = ServiceUtil.returnFailure();
                xmlDocument = null;
            }
            finally 
            {
                try 
                {
                    if (xmlDocument != null) 
                    {
                        // ################################################
                        // save the DOM document to schema.xml file 
                        // ################################################
                        xmlDocument.normalize();
                        os = new FileOutputStream(schemaXmlFileUrl.getPath());
                        UtilXml.writeXmlDocument(os, xmlDocument);
                    }
                    if (os != null) os.close();
                    if (ins != null) ins.close();
                    if (insc != null) insc.close();
                } 
                catch (IOException ioe) 
                {
                    Debug.logError(ioe, module);
                    resp = ServiceUtil.returnFailure();
                } 
                catch (Exception exc) 
                {
                    Debug.logError(exc, module);
                    resp = ServiceUtil.returnFailure();
                }
                os = null;
                ins = null;
                insc = null;
                if (resp == null) resp = ServiceUtil.returnSuccess();
            }
        }
        else 
        {
            resp = ServiceUtil.returnFailure();
        }
        resp.put("addFeatureElementCount", addFeatureElementCount);
        return resp;
    }

    private static Node removeNode(Document docment, String removeNodeName) throws Exception 
    {

        Node parentNode = null;
        if (UtilValidate.isNotEmpty(removeNodeName) && UtilValidate.isNotEmpty(docment)) 
        {
            if (docment.getElementsByTagName(removeNodeName).getLength() > 0 ) 
            {
                List<? extends Node> nodeList = UtilXml.childNodeList(docment.getElementsByTagName(removeNodeName).item(0));
                for (Node node: nodeList) 
                {
                    if (node.getNodeName().equalsIgnoreCase(removeNodeName)) 
                    {
                        parentNode = node.getParentNode();
                        parentNode.removeChild(node);
                    }
                }
            }
        }
        return parentNode;
    }

    private static void importNode(Document importDocument, Node parentNode, String importNodeName) throws Exception 
    {

        if (UtilValidate.isNotEmpty(importNodeName) && UtilValidate.isNotEmpty(importDocument) && UtilValidate.isNotEmpty(parentNode) ) 
        {
            if (importDocument.getElementsByTagName(importNodeName).getLength() > 0 ) 
            {
                List<? extends Node> nodeList = UtilXml.childNodeList(importDocument.getElementsByTagName(importNodeName).item(0));
                for (Node node: nodeList) 
                {
                    if (node.getNodeName().equalsIgnoreCase(importNodeName)) 
                    {
                        Node importNode = parentNode.getOwnerDocument().importNode(node, true);
                        parentNode.appendChild(importNode);
                    }
                }
            }
        }
    }
    
    //<!-- REMOVE THE TEST JOB -->
    public static Map testJob(DispatchContext dctx, Map context)
    {
    	Map<String, Object> resp = null;
    	Map<String, Object> results = null;
    	String testValue = "success";
    	//results.put("test", "testing");
    	if(UtilValidate.isNotEmpty(testValue))
    	{
    		if(testValue.equals("error"))
    		{
    			resp= ServiceUtil.returnError("Your testJob service threw this error.");
    		}
    		else if(testValue.equals("fail"))
    		{
    			resp=  ServiceUtil.returnFailure("Your testJob has failed");
    		}
    		else if(testValue.equals("success"))
    		{
    			resp=  ServiceUtil.returnSuccess("Your testJob ran successfully.");
    		}
    		
    	}
    	return resp;
    }
    
    public static Map genProductsIndexAdd(DispatchContext dctx, Map context) throws GenericTransactionException
    {

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String productId = (String) context.get("productId");
        String browseRootProductCategoryId = (String) context.get("browseRootProductCategoryId");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<ProductDocument> documentList = new ArrayList();
        
        List<String> headerColumns = getHeaderColumns();

        List<String> headerNames = getHeaderColumns();
        
        List<CellProcessor> cellProcessors = getCellProcesser();

        List<String> prodFeatureColNames = new ArrayList();
        
        ProductContentWrapper productContentWrapper = null;
        try 
        {
            // Find Product Store - to find store's currency setting
            GenericValue productStore = delegator.findOne("ProductStore", UtilMisc.toMap("productStoreId", productStoreId), false);

            List<GenericValue> productFeatureGroupApplsList = delegator.findList("ProductFeatureGroupAppl", null, UtilMisc.toSet("productFeatureGroupId", "productFeatureId", "fromDate", "thruDate", "sequenceNum"), null, null, false);
            
            productFeatureGroupApplsList = EntityUtil.filterByDate(productFeatureGroupApplsList);
            
            // Get all unexpired Product Categories (Top Level Catalog Category)
            GenericValue workingCategory = null;
            List<Map<String, Object>> allUnexpiredCategories = getRelatedCategories(delegator, browseRootProductCategoryId, null, true, true, true);
            updateHeaderAndProcesser(dispatcher, headerColumns, headerNames,
					cellProcessors, prodFeatureColNames,
					allUnexpiredCategories);
            
            ProductDocument productDocument = null;
            
            Map<String, Object> results = null;
            String productCategoryId = null;
            StringUtil.StringWrapper imageUrl = null;
            String categoryDescription = null;
            Double totalQuantityOrdered = 0.00;
            BigDecimal averageProductRating = BigDecimal.ZERO;
            Long totalTimesViewed = 0L;
            GenericValue product = null;
            if(UtilValidate.isNotEmpty(productId))
            {
            	product = delegator.findOne("Product", UtilMisc.toMap("productId", productId));
            }
            
            String productDocumentId = null;

            // All Sub Categries
            String productCategoryIdPath = null;
            int categoryLevel = 0;
            List<String> categoryTrail = null;
            if(UtilValidate.isNotEmpty(product)) 
            {
                List<GenericValue> productCategoryMembers = product.getRelated("ProductCategoryMember");
                productCategoryMembers = EntityUtil.orderBy(productCategoryMembers,UtilMisc.toList("sequenceNum"));

                // Remove any expired
                productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    
                for (GenericValue productCategoryMember : productCategoryMembers) 
                {
                    productCategoryId = (String) productCategoryMember.getString("productCategoryId");
                    workingCategory = productCategoryMember.getRelatedOne("ProductCategory");
                    String isVariant = product.getString("isVariant");
	                if (UtilValidate.isEmpty(isVariant)) 
	                {
	                    isVariant = "N";
	                }
                    // All Non-Variant Products
                    if ("N".equals(isVariant)) 
                    {
                        //if (ProductWorker.isSellable(product)) 
                        //{
                            productDocument = getProductDocumentFromProduct(
											locale, delegator, dispatcher,
											productStore,
											productFeatureGroupApplsList,
											workingCategory,
											categoryDescription, product,
											productCategoryMember);
                                    
                            documentList.add(productDocument);
                        //}
                    }
                }
            }

            if (UtilValidate.isNotEmpty(documentList)) 
            {
                // Generate CSV File
                String[] columnNames = (String[]) headerColumns.toArray(new String[headerColumns.size()]);
                String filename = FlexibleStringExpander.expandString(OSAFE_PROPS.getString("solrProductIndexFile"), context);

                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true), "UTF-8")));
				CsvBeanWriter cbw = new CsvBeanWriter(writer, CsvPreference.EXCEL_PREFERENCE);

                CellProcessor[] cp = (CellProcessor[]) cellProcessors.toArray(new CellProcessor[cellProcessors.size()]);
                
                for (ProductDocument doc : documentList) 
                {
                    cbw.write(doc, columnNames, cp);
                }
                cbw.close();
                writer.flush();

                String solrServer = OSAFE_PROPS.getString("solrIndexServer");

                Debug.log("solrServer=" + solrServer, module);

                // Delete previous index using Http Client
                String deleteAllUrl = solrServer + "/update?stream.body=<delete><query>*:*</query></delete>&commit=true";
                HttpClient hc = new HttpClient(deleteAllUrl);
                String deleteResponse = hc.post();
                Debug.log(deleteResponse, module);

                // Import CSV file using Http Client
                int index = prodFeatureColNames.size();
                for (String prodFeatureType : prodFeatureColNames)
                {
                    columnNames[columnNames.length - index] = prodFeatureType;
                    index--;
                }
                String importUrl = solrServer + "/update/csv?stream.file=" + filename + "&stream.contentType=text/plain;charset=utf-8&header=false&commit=true&fieldnames=" + StringUtils.join(columnNames, ",");
                Debug.log(importUrl, module);
                hc = new HttpClient(importUrl);
                String importResponse = hc.post();
                Debug.log(importResponse, module);
            }
        } 
        catch (Exception e) 
        {
            Debug.logError(e, e.getMessage(), module);
        }

        if (documentList == null) 
        {
            documentList = new ArrayList();
        }
        result.put("documentListCount", documentList.size());

        return result;
    
    }

	private static ProductDocument getProductDocumentFromProduct(Locale locale,
			Delegator delegator, LocalDispatcher dispatcher,
			GenericValue productStore,
			List<GenericValue> productFeatureGroupApplsList,
			GenericValue workingCategory, String categoryDescription,
			GenericValue product, GenericValue productCategoryMember)
			throws GenericEntityException, GenericServiceException {
		String productId;
        String productStoreId = productStore.getString("productStoreId");
		ProductContentWrapper productContentWrapper;
		ProductDocument productDocument;
		Map<String, Object> results;
		StringUtil.StringWrapper imageUrl;
		Double totalQuantityOrdered;
		BigDecimal averageProductRating;
		Long totalTimesViewed;
		String productDocumentId;
		productContentWrapper = new ProductContentWrapper(dispatcher, product, locale, "text/html");
		productDocument = new ProductDocument();
		productId = product.getString("productId");
		productDocumentId = SolrConstants.ROW_TYPE_PRODUCT + "_" + productId;
		productDocument.setId(productDocumentId);
		productDocument.setProductId(productId);
		productDocument.setRowType(SolrConstants.ROW_TYPE_PRODUCT);
		productDocument.setName(productContentWrapper.get("PRODUCT_NAME", "html").toString());
		productDocument.setInternalName(product.getString("internalName"));
		
		GenericValue goodIdentification = delegator.findOne("GoodIdentification", UtilMisc.toMap("productId", productId, "goodIdentificationTypeId", "MANUFACTURER_ID_NO"));
		if(UtilValidate.isNotEmpty(goodIdentification))
		{
			productDocument.setManufacturerIdNo(goodIdentification.getString("idValue"));
		}
		
		String manufacturerPartyId = product.getString("manufacturerPartyId");
		if(UtilValidate.isNotEmpty(manufacturerPartyId))
		{
			GenericValue manufacturerParty = delegator.findOne("Party", UtilMisc.toMap("partyId", manufacturerPartyId));
			if(UtilValidate.isNotEmpty(manufacturerParty))
			{
				PartyContentWrapper partyContentWrapper = new PartyContentWrapper(dispatcher, manufacturerParty, locale, "text/html");
				productDocument.setManufacturerName(partyContentWrapper.get("PROFILE_NAME", "html").toString());
			}
		}
		
		
		
		if(UtilValidate.isNotEmpty(product.getTimestamp("introductionDate")))
		{
			productDocument.setIntroductionDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(product.getTimestamp("introductionDate").getTime())));
		}
		
		productDocument.setSalesDiscontinuationDateNullFlag(0);
        Timestamp salesDiscoDateTs = product.getTimestamp("salesDiscontinuationDate");
        if(UtilValidate.isNotEmpty(salesDiscoDateTs))
        {
        	productDocument.setSalesDiscontinuationDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(salesDiscoDateTs.getTime())));
        	productDocument.setSalesDiscontinuationDateNullFlag(1);
        }
        
        String SORT_OPTIONS  = Util.getProductStoreParm(productStoreId, "PLP_AVAILABLE_SORT");
        if(UtilValidate.isNotEmpty(SORT_OPTIONS) && SORT_OPTIONS.contains("DISCO_DATE"))
        {
            //if virtual then attempt to override disco date with variant disco date
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            if(UtilValidate.isEmpty(salesDiscoDateTs) || (UtilValidate.isNotEmpty(salesDiscoDateTs) && !salesDiscoDateTs.before(nowTimestamp)))
            {
                String isVirtual = product.getString("isVirtual");
                if ("Y".equals(isVirtual)) 
                {
                	List<GenericValue> productAssocVariantList = product.getRelated("MainProductAssoc");
                	productAssocVariantList = EntityUtil.filterByAnd(productAssocVariantList, UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT")); 
                	List<GenericValue> assocProductVariantList = EntityUtil.getRelated("AssocProduct", productAssocVariantList);
                	assocProductVariantList = EntityUtil.orderBy(assocProductVariantList, UtilMisc.toList("salesDiscontinuationDate ASC"));
                	for(GenericValue productVariant : assocProductVariantList)
                	{
            			if(UtilValidate.isNotEmpty(productVariant.getTimestamp("salesDiscontinuationDate")))
                        {
            				if(!productVariant.getTimestamp("salesDiscontinuationDate").before(nowTimestamp))
            				{
            					if(UtilValidate.isEmpty(salesDiscoDateTs))
                                {
            						salesDiscoDateTs = productVariant.getTimestamp("salesDiscontinuationDate");
            						productDocument.setSalesDiscontinuationDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(salesDiscoDateTs.getTime())));
                                	productDocument.setSalesDiscontinuationDateNullFlag(1);
                                }
            					else if(UtilValidate.isNotEmpty(salesDiscoDateTs) && salesDiscoDateTs.after(productVariant.getTimestamp("salesDiscontinuationDate")))
                				{
            						salesDiscoDateTs = productVariant.getTimestamp("salesDiscontinuationDate");
                                	productDocument.setSalesDiscontinuationDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").format(new java.util.Date(salesDiscoDateTs.getTime())));
                                	productDocument.setSalesDiscontinuationDateNullFlag(1);
                				}
            					break;
            				}
                        }
                	}
                }
            }
        }
        
		productDocument.setSequenceNum(productCategoryMember.getString("sequenceNum"));
		productDocument.setCategoryName(workingCategory.getString("categoryName"));
		
		if (UtilValidate.isNotEmpty(categoryDescription) && !"null".equalsIgnoreCase(categoryDescription.toString())) 
		{
			productDocument.setCategoryDescription(categoryDescription.toString());
		}
		// LONG_DESCRIPTION
		String longDescription = ProductContentWrapper.getProductContentAsText(product, "LONG_DESCRIPTION", locale, dispatcher , "string");
		if (UtilValidate.isNotEmpty(longDescription)) 
		{
		    productDocument.setDescription(longDescription);
		}

		// SMALL_IMAGE_URL
		imageUrl = productContentWrapper.get("SMALL_IMAGE_URL", "string");
		if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
		{
		    productDocument.setProductImageSmallUrl(imageUrl.toString());
		}

		// SMALL_IMAGE_ALT
		imageUrl = productContentWrapper.get("SMALL_IMAGE_ALT", "string");
		if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
		{
		    productDocument.setProductImageSmallAlt(imageUrl.toString());
		}

		// SMALL_IMAGE_ALT_URL
		imageUrl = productContentWrapper.get("SMALL_IMAGE_ALT_URL", "string");
		if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
		{
			if (UtilValidate.isNotEmpty(imageUrl.toString()))
			{
		        productDocument.setProductImageSmallAltUrl(imageUrl.toString());
			}
		}
		// MEDIUM_IMAGE_URL
		imageUrl = productContentWrapper.get("MEDIUM_IMAGE_URL", "string");
		if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
		{
		    productDocument.setProductImageMediumUrl(imageUrl.toString());
		}

		// LARGE_IMAGE_URL
		imageUrl = productContentWrapper.get("LARGE_IMAGE_URL", "string");
		if (UtilValidate.isNotEmpty(imageUrl) && !"null".equalsIgnoreCase(imageUrl.toString())) 
		{
		    productDocument.setProductImageLargeUrl(imageUrl.toString());
		}

		results = dispatcher.runSync("getProductFeaturesByType", UtilMisc.toMap("productId", productId));
		List<String> productFeatureTypes = (List<String>) results.get("productFeatureTypes");

		// for each new feature type add a column heading
		// ex. COLOR SIZE

		Map<String, GenericValue> productFeaturesByType = (Map<String, GenericValue>) results.get("productFeaturesByType");
		for (String productFeatureType : productFeatureTypes) 
		{

		    List<GenericValue> productFeatures = (List<GenericValue>) productFeaturesByType.get(productFeatureType);

		    try 
		    {
		    	
		        Map<String, String> featureValuesMap = FastMap.newInstance();
		        List<String> featureValues = new ArrayList();
		        try 
		        {
		            for (GenericValue feature : productFeatures) 
		            {
		            	String description ="";
		            	String productFeatureApplTypeId = feature.getString("productFeatureApplTypeId");
		            	List<GenericValue> productFeatureGroupAppls = EntityUtil.filterByAnd(productFeatureGroupApplsList, UtilMisc.toMap("productFeatureId",feature.getString("productFeatureId")));
		            	if(productFeatureGroupAppls.size() > 0) 
		            	{
                    		//Issue 38916
		                    description = feature.getString("description");
		                	if (UtilValidate.isNotEmpty(description))
		                	{
	                            String forceCase = Util.getProductStoreParm(delegator, productStoreId, "FACET_VALUE_FORCE_CASE");
    		                	if (UtilValidate.isNotEmpty(forceCase))
    		                	{
    	                            if ("UPPER".equals(forceCase.trim().toUpperCase()))
    	                            {
    	    		                    description = description.toUpperCase();
    	                            }
    	                            else if ("LOWER".equals(forceCase.trim().toUpperCase()))
    	                            {
    	    		                    description = description.toLowerCase();
    	                            	
    	                            }
    		                		
    		                	}
		                	}
		            	}
		                if ("SELECTABLE_FEATURE".equals(productFeatureApplTypeId)) 
		                {
		                    if (description.contains("/")) 
		                    {
		                        String[] descriptionParts = StringUtils.split(description, "/");
		                        for (String descPart : descriptionParts) 
		                        {
		                            descPart = StringUtils.trim(descPart);
		                            featureValuesMap.put(descPart, descPart);
		                        }
		                        continue;
		                    }
		                }
		                try 
		                {
		                	if (UtilValidate.isNotEmpty(description))
		                	{
		                        featureValuesMap.put(description, description);
		                		
		                	}
		                }
		                catch (Exception eee) 
		                {
		                	Debug.logError(eee, "h" + eee.getMessage()+ description, module);
		                }
		            }
		        }
		        catch (Exception eee) 
		        {
		        	Debug.logError(eee, eee.getMessage(), module);
		        }
		        Set<Entry<String, String>> featureValuesEntrySet = featureValuesMap.entrySet();
		        Iterator<Entry<String, String>> featureValuesIterator = featureValuesEntrySet.iterator();
		        while (featureValuesIterator.hasNext()) 
		        {
		            Map.Entry<String, String> featureValueEntry = (Map.Entry<String, String>) featureValuesIterator.next();
		            featureValues.add((String) featureValueEntry.getKey());
		        }

		        // Need to replace spaces in decription with underscore symbol "_"
		        for (int i = 0; i < featureValues.size(); i++) 
		        {
		            String val = featureValues.get(i);
		            val = URLEncoder.encode(StringUtils.replace(val, " ", "_"), SolrConstants.DEFAULT_ENCODING);
		            featureValues.set(i, val);
		        }

		        productDocument.addProductFeature(productFeatureType, featureValues);

		    }
		     catch (Exception ee)
		     {
		    	 Debug.logError(ee, ee.getMessage(),module);
		     }
		    // ex. Red Small
		}

		// Product Prices
		String currencyUomId = productStore.getString("defaultCurrencyUomId");
		results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product, "currencyUomId", currencyUomId));
		productDocument.setListPrice((BigDecimal) results.get("listPrice"));
		productDocument.setPrice((BigDecimal) results.get("price"));

		// Product RECURRENCE Prices
		results = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product, "currencyUomId", currencyUomId,"productPricePurposeId","RECURRING_CHARGE"));
		productDocument.setRecurrencePrice((BigDecimal) results.get("price"));

		GenericValue ProductCalculatedInfo = delegator.findOne("ProductCalculatedInfo", UtilMisc.toMap("productId", productId), false);
		// Product Ratings
		if(UtilValidate.isNotEmpty(ProductCalculatedInfo) && UtilValidate.isNotEmpty(ProductCalculatedInfo.getBigDecimal("averageCustomerRating")))
		{
		    averageProductRating = ProductCalculatedInfo.getBigDecimal("averageCustomerRating");
		} 
		else 
		{
		    averageProductRating = BigDecimal.ZERO;
		}
		productDocument.setCustomerRating(averageProductRating);

		// Product Quantity Ordered
        if(UtilValidate.isNotEmpty(ProductCalculatedInfo) && ProductCalculatedInfo.getDouble("totalQuantityOrdered")!= null)
        {
		    totalQuantityOrdered = ProductCalculatedInfo.getDouble("totalQuantityOrdered");
        }
        else
        {
		    totalQuantityOrdered = 0.00;
        }
        productDocument.setTotalQuantityOrdered(totalQuantityOrdered);
        
        // Product View Count
        if(UtilValidate.isNotEmpty(ProductCalculatedInfo) && ProductCalculatedInfo.getLong("totalTimesViewed")!= null)
        {
		    totalTimesViewed = ProductCalculatedInfo.getLong("totalTimesViewed");
        }
        else
        {
		    totalTimesViewed = 0L;
        }
        productDocument.setTotalTimesViewed(totalTimesViewed);

        // Product Facilities
        List<GenericValue>  productFacilityList = delegator.findList("ProductFacility", EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId), UtilMisc.toSet("facilityId"), null, null, false);
        List productFacilityIdList = new ArrayList();
        for (GenericValue productFacility : productFacilityList)
        {
		    productFacilityIdList.add(productFacility.getString("facilityId"));
        }
        String productFacilityIds = StringUtils.join(productFacilityIdList, " ");
        productDocument.setProductFacilityIds(productFacilityIds);

		
        List<GenericValue> rollups = null;

        rollups = delegator.findByAnd("ProductCategoryRollup", UtilMisc.toMap("productCategoryId", productCategoryMember.getString("productCategoryId")), UtilMisc.toList("sequenceNum"));
        rollups = EntityUtil.filterByDate(rollups);
                           
		GenericValue gvTopMostCategory = null;
		for(GenericValue rollUp : rollups)
		{
			gvTopMostCategory = rollUp.getRelatedOne("ParentProductCategory");
			break;
		}
		
		if (UtilValidate.isNotEmpty(gvTopMostCategory)) 
		{
		    String topMostProductCategoryId = gvTopMostCategory.getString("productCategoryId");
		    productDocumentId = SolrConstants.ROW_TYPE_PRODUCT + "_" + productId + "_" + topMostProductCategoryId + "_" + productCategoryMember.getString("productCategoryId");
		    productDocument.setId(productDocumentId);
		    productDocument.setProductCategoryId(productCategoryMember.getString("productCategoryId"));
		    productDocument.setTopMostProductCategoryId(topMostProductCategoryId);
		}
		return productDocument;
	}
    
    
    public static Map genProductsIndexUpdate(DispatchContext dctx, Map context) throws GenericTransactionException
    {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String productId = (String) context.get("productId");
        String browseRootProductCategoryId = (String) context.get("browseRootProductCategoryId");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<ProductDocument> documentList = new ArrayList();
        List<ProductDocument> documentListExisting = new ArrayList();
        
        List<String> headerColumns = getHeaderColumns();
        
        List<String> headerNames = getHeaderColumns();

        List<CellProcessor> cellProcessors = getCellProcesser();
        
        List<String> prodFeatureColNames = new ArrayList();
        
        ProductContentWrapper productContentWrapper = null;
        try 
        {

            // Find Product Store - to find store's currency setting
            GenericValue productStore = delegator.findOne("ProductStore", UtilMisc.toMap("productStoreId", productStoreId), false);

            List<GenericValue> productFeatureGroupApplsList = delegator.findList("ProductFeatureGroupAppl", null, UtilMisc.toSet("productFeatureGroupId", "productFeatureId", "fromDate", "thruDate", "sequenceNum"), null, null, false); 
            productFeatureGroupApplsList = EntityUtil.filterByDate(productFeatureGroupApplsList);
            
            // Get all unexpired Product Categories (Top Level Catalog Category)
            GenericValue workingCategory = null;
            List<Map<String, Object>> allUnexpiredCategories = getRelatedCategories(delegator, browseRootProductCategoryId, null, true, true, true);
            updateHeaderAndProcesser(dispatcher, headerColumns, headerNames,
					cellProcessors, prodFeatureColNames,
					allUnexpiredCategories);
            
            ProductDocument productDocument = null;
            
            Map<String, Object> results = null;
            String productCategoryId = null;
            StringUtil.StringWrapper imageUrl = null;
            String categoryDescription = null;
            Double totalQuantityOrdered = 0.00;
            BigDecimal averageProductRating = BigDecimal.ZERO;
            Long totalTimesViewed = 0L;
            GenericValue product = null;
            if(UtilValidate.isNotEmpty(productId))
            {
            	product = delegator.findOne("Product", UtilMisc.toMap("productId", productId));
            }
            
            String productDocumentId = null;

            // All Sub Categries
            String productCategoryIdPath = null;
            int categoryLevel = 0;
            List<String> categoryTrail = null;
            if(UtilValidate.isNotEmpty(product)) 
            {
                List<GenericValue> productCategoryMembers = product.getRelated("ProductCategoryMember");
                productCategoryMembers = EntityUtil.orderBy(productCategoryMembers,UtilMisc.toList("sequenceNum"));

                // Remove any expired
                productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    
                for (GenericValue productCategoryMember : productCategoryMembers) 
                {
                    productCategoryId = (String) productCategoryMember.getString("productCategoryId");
                    workingCategory = productCategoryMember.getRelatedOne("ProductCategory");
                    String isVariant = product.getString("isVariant");
                    if (UtilValidate.isEmpty(isVariant)) 
                    {
                        isVariant = "N";
                    }
                    // All Non-Variant Products
                    if ("N".equals(isVariant)) 
                    {
                        //if (ProductWorker.isSellable(product)) 
                        //{
                            productDocument = getProductDocumentFromProduct(
							        locale, delegator, dispatcher,
								    productStore,
									productFeatureGroupApplsList,
									workingCategory,
									categoryDescription, product,
									productCategoryMember);
                                    
                            documentList.add(productDocument);
                        //}
                     }
                 }
             }

             if (UtilValidate.isNotEmpty(documentList)) 
             {
                 // Generate CSV File
                 String[] columnNames = (String[]) headerColumns.toArray(new String[headerColumns.size()]);
                 String[] headerColumnNames = (String[]) headerNames.toArray(new String[headerNames.size()]);
                 String filename = FlexibleStringExpander.expandString(OSAFE_PROPS.getString("solrProductIndexFile"), context);

                 Debug.log("solrProductIndexFile=" + filename, module);

                 CellProcessor[] cp = (CellProcessor[]) cellProcessors.toArray(new CellProcessor[cellProcessors.size()]);
                
                 /*String[] row = null;
                 
                 InputStreamReader inFile = new InputStreamReader(new FileInputStream(filename));
                 
                 CSVReader csvReader = new CSVReader(inFile);
                 
                 ProductDocument productDocumentRead = null;
                 //List content = csvReader.readAll();
                 while((row = csvReader.readNext()) != null)
                 {
                     productDocumentRead = getProductDocumentFromCsv(row, headerColumnNames);
                     if(productDocumentRead.getRowType().equals(SolrConstants.ROW_TYPE_PRODUCT))
                	 {
                	     for (ProductDocument doc : documentList) 
                         {
            				 if(productDocumentRead.getProductId().equals(doc.getProductId()))
                             {
                            	 documentListExisting.add(doc);
                             }
                             else
                             {
                            	 documentListExisting.add(productDocumentRead);
                             }
                         }
                	 }
                	 else
                	 {
                	     documentListExisting.add(productDocumentRead);
                	 }
                     System.out.println("++++++++++++"+row[0]);
                 }
                 csvReader.close();
                 inFile.close();*/
                
                 PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true), "UTF-8")));
				 CsvBeanWriter cbw = new CsvBeanWriter(writer, CsvPreference.EXCEL_PREFERENCE);
                
                 for (ProductDocument doc : documentList) 
                 {
                     cbw.write(doc, columnNames, cp);
                 }
                 cbw.close();
                 writer.flush();

                 String solrServer = OSAFE_PROPS.getString("solrIndexServer");

                 // Delete previous index using Http Client
                 String deleteAllUrl = solrServer + "/update?stream.body=<delete><query>*:*</query></delete>&commit=true";
                 HttpSolrClient hc = new HttpSolrClient.Builder(solrServer).build();
                 HttpResponse deleteResponse = hc.getHttpClient().execute(new HttpPost(deleteAllUrl));
                 Debug.log(IOUtils.toString(deleteResponse.getEntity().getContent(), Charset.defaultCharset()), module);

                 // Import CSV file using Http Client
                 int index = prodFeatureColNames.size();
                 for (String prodFeatureType : prodFeatureColNames) 
                 {
                     columnNames[columnNames.length - index] = prodFeatureType;
                     index--;
                 }
                 String importUrl = solrServer + "/update/csv?stream.file=" + filename + "&stream.contentType=text/plain;charset=utf-8&header=false&commit=true&fieldnames=" + StringUtils.join(columnNames, ",");
                 Debug.log(importUrl, module);
                 HttpResponse importResponse = hc.getHttpClient().execute(new HttpPost(importUrl));
                 Debug.log(IOUtils.toString(importResponse.getEntity().getContent(), Charset.defaultCharset()), module);
             }
         } 
         catch (Exception e) 
         {
             Debug.logError(e, e.getMessage(), module);
         }

         if (documentList == null) 
         {
             documentList = new ArrayList();
         }
         result.put("documentListCount", documentList.size());

         return result;
    }

	private static void updateHeaderAndProcesser(LocalDispatcher dispatcher,
			List<String> headerColumns, List<String> headerNames,
			List<CellProcessor> cellProcessors,
			List<String> prodFeatureColNames,
			List<Map<String, Object>> allUnexpiredCategories)
			throws GenericEntityException, GenericServiceException {
		GenericValue workingCategory;
		for (Map<String, Object> workingCategoryMap : allUnexpiredCategories) 
		{
		    workingCategory = (GenericValue) workingCategoryMap.get("ProductCategory");
		    List<GenericValue> productCatMemberList = workingCategory.getRelated("ProductCategoryMember");
		    if(UtilValidate.isNotEmpty(productCatMemberList))
		    {
		    	for(GenericValue productCatMember : productCatMemberList)
		    	{
		    		GenericValue product = productCatMember.getRelatedOne("Product");
		    		String isVariant = product.getString("isVariant");
		    		if (UtilValidate.isEmpty(isVariant)) 
		            {
		                isVariant = "N";
		            }
		            if ("N".equals(isVariant)) 
		            {
		                //if (ProductWorker.isSellable(product)) 
		                //{
		                	Map results = dispatcher.runSync("getProductFeaturesByType", UtilMisc.toMap("productId", product.getString("productId")));
		                    List<String> productFeatureTypes = (List<String>) results.get("productFeatureTypes");

		                    Map<String, GenericValue> productFeaturesByType = (Map<String, GenericValue>) results.get("productFeaturesByType");
		                    for (String productFeatureType : productFeatureTypes) 
		                    {
		                        try 
		                        {
		                            if (!prodFeatureColNames.contains(productFeatureType)) 
		                            {
		                                headerColumns.add("productFeature");
		                                headerNames.add(productFeatureType);
		                                cellProcessors.add(new ProductFeatureCellProcessor(productFeatureType));
		                                prodFeatureColNames.add(productFeatureType);
		                            }
		                        }
		                         catch (Exception ee)
		                         {
		                        	 Debug.logError(ee, ee.getMessage(),module);
		                         }
		                    }
		                //}
		            }
		    	}
		    }
		}
	}
    
    /*private static ProductDocument getProductDocumentFromCsv(String[] row, String[] headerColumnNames)
    {
    	ProductDocument doc = new ProductDocument();
    	if(row.length == headerColumnNames.length)
    	{
    		doc.setId(getString(row[0]));
    		doc.setRowType(getString(row[1]));
    		doc.setProductId(getString(row[2]));
    		doc.setName(getString(row[3]));
    		doc.setInternalName(getString(row[4]));
    		doc.setDescription(getString(row[5]));
    		doc.setCategoryDescription(getString(row[6]));
    		doc.setCategoryPdpDescription(getString(row[7]));
    		doc.setProductCategoryId(getString(row[8]));
    		doc.setTopMostProductCategoryId(getString(row[9]));
    		doc.setCategoryLevel(getString(row[10]));
    		doc.setCategoryName(getString(row[11]));
    		doc.setCategoryImageUrl(getString(row[12]));
    		doc.setProductImageSmallUrl(getString(row[13]));
    		doc.setProductImageSmallAlt(getString(row[14]));
    		doc.setProductImageSmallAltUrl(getString(row[15]));
    		doc.setProductImageMediumUrl(getString(row[16]));
    		doc.setProductImageLargeUrl(getString(row[17]));
    		doc.setProductFeatureGroupId(getString(row[18]));
    		doc.setProductFeatureGroupDescription(getString(row[19]));
    		doc.setProductCategoryFacetGroups(getString(row[20]));
    		doc.setListPrice(getString(row[21]));
    		doc.setPrice(getString(row[22]));
    		doc.setCustomerRating(getString(row[23]));
    		doc.setSequenceNum(getString(row[24]));
    		doc.setTotalTimesViewed(getString(row[25]));
    		doc.setTotalQuantityOrdered(getString(row[26]));
    		doc.setProductFacilityIds(getString(row[27]));
    		doc.setIntroductionDate(getString(row[28]));
    		doc.setSalesDiscontinuationDate(getString(row[29]));
    		doc.setManufacturerName(getString(row[30]));
    		doc.setManufacturerIdNo(getString(row[31]));
    		
    		for(int i = 32; i < headerColumnNames.length; i++)
    		{
    			doc.addProductFeature(headerColumnNames[i], new ArrayList<String>(Arrays.asList(getString(row[i]).split(" "))));
    		}
    	}
    	return doc;
    }*/
    
    public static String getString(Object tsObj) 
    {
        if (UtilValidate.isNotEmpty(tsObj))
        {
            return tsObj.toString();
        }
        else
        {
            return "";
        }
    }
    
}
