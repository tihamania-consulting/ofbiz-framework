/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.*;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.*;
import org.apache.ofbiz.accounting.payment.*;
import org.apache.ofbiz.order.order.*;
import org.apache.ofbiz.party.contact.*;
import org.apache.ofbiz.product.catalog.*;
import org.apache.ofbiz.product.store.*;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import com.osafe.util.Util;

ShoppingCart cart = session.getAttribute("shoppingCart");
context.cart = cart;

orderItems = cart.makeOrderItems(dispatcher);
context.orderItems = orderItems;

//Get the Sub Total
shoppingCartSubTotal = cart.getSubTotal();

orderAdjustments = cart.makeAllAdjustments();

shoppingCartSize = cart.getTotalQuantity();

shippingApplies = shoppingCart.shippingApplies();

orderItemShipGroupInfo = cart.makeAllShipGroupInfos();
if (orderItemShipGroupInfo) 
{
    orderItemShipGroupInfo.each 
	{ valueObj ->
        if ("OrderAdjustment".equals(valueObj.getEntityName())) 
		{
            // shipping / tax adjustment(s)
            orderAdjustments.add(valueObj);
        }
    }
}
context.orderAdjustments = orderAdjustments;

//Get currency
CURRENCY_UOM_DEFAULT = Util.getProductStoreParm(request,"CURRENCY_UOM_DEFAULT");
currencyUom = CURRENCY_UOM_DEFAULT;
if(UtilValidate.isEmpty(currencyUom))
{
	currencyUom = cart.getCurrency();
}

workEfforts = cart.makeWorkEfforts();   // if required make workefforts for rental fixed assets too.
context.workEfforts = workEfforts;

orderHeaderAdjustments = OrderReadHelper.getOrderHeaderAdjustments(orderAdjustments, null);
context.orderHeaderAdjustments = orderHeaderAdjustments;
context.orderItemShipGroups = cart.getShipGroups();
context.headerAdjustmentsToShow = OrderReadHelper.filterOrderAdjustments(orderHeaderAdjustments, true, false, false, false, false);

orderSubTotal = OrderReadHelper.getOrderItemsSubTotal(orderItems, orderAdjustments, workEfforts);
context.orderSubTotal = orderSubTotal;
context.placingCustomerPerson = userLogin?.getRelatedOne("Person");
context.paymentMethods = cart.getPaymentMethods();

paymentMethodTypeIds = cart.getPaymentMethodTypeIds();
paymentMethodType = null;
paymentMethodTypeId = null;
if (paymentMethodTypeIds) 
{
    paymentMethodTypeId = paymentMethodTypeIds[0];
    paymentMethodType = delegator.findByPrimaryKeyCache("PaymentMethodType", [paymentMethodTypeId : paymentMethodTypeId]);
    context.paymentMethodType = paymentMethodType;
}

webSiteId = CatalogWorker.getWebSiteId(request);

productStore = ProductStoreWorker.getProductStore(request);
context.productStore = productStore;

isDemoStore = !"N".equals(productStore.isDemoStore);
context.isDemoStore = isDemoStore;

payToPartyId = productStore.payToPartyId;
paymentAddress = PaymentWorker.getPaymentAddress(delegator, payToPartyId);
if (paymentAddress) context.paymentAddress = paymentAddress;


// TODO: FIXME!
/*
billingAccount = cart.getBillingAccountId() ? delegator.findOne("BillingAccount", [billingAccountId : cart.getBillingAccountId()]) : null;
if (billingAccount)
    context.billingAccount = billingAccount;
*/

context.customerPoNumber = cart.getPoNumber();
context.carrierPartyId = cart.getCarrierPartyId();
context.shipmentMethodTypeId = cart.getShipmentMethodTypeId();
context.shippingInstructions = cart.getShippingInstructions();
context.maySplit = cart.getMaySplit();
context.giftMessage = cart.getGiftMessage();
context.isGift = cart.getIsGift();
context.currencyUomId = cart.getCurrency();

shipmentMethodType = delegator.findByPrimaryKeyCache("ShipmentMethodType", [shipmentMethodTypeId : cart.getShipmentMethodTypeId()]);
if (shipmentMethodType) context.shipMethDescription = shipmentMethodType.description;

orh = new OrderReadHelper(orderAdjustments, orderItems);
context.localOrderReadHelper = orh;
context.orderShippingTotal = cart.getTotalShipping();
context.orderTaxTotal = cart.getTotalSalesTax();
context.orderGrandTotal = cart.getGrandTotal();

// nuke the event messages
request.removeAttribute("_EVENT_MESSAGE_");
//Get currency
context.currencyUom = currencyUom;

//Sub Total
context.shoppingCartSubTotal = shoppingCartSubTotal;
context.cartSubTotal = shoppingCartSubTotal;
//cart size
context.shoppingCartTotalQuantity = shoppingCartSize;
context.shoppingCartSize = shoppingCartSize;

context.shippingApplies = shippingApplies;
