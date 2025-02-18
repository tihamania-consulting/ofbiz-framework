import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.common.CommonWorkers;
import org.apache.ofbiz.entity.condition.*;

// Since this script is run after ViewShipment, we will re-use the shipment in the context
shipmentId = parameters.shipmentId;
if (UtilValidate.isEmpty(shipmentId))
{
    shipmentId = request.getAttribute("shipmentId");
}
shipment = delegator.findOne("Shipment", [shipmentId : shipmentId], false);
if (UtilValidate.isEmpty(shipment))
{
    return;
}
else
{
	context.shipment = shipment;
}

// get the packages related to this shipment in order of packages
shipmentPackages = shipment.getRelated("ShipmentPackage", ['shipmentPackageSeqId']);

context.shipmentPackages = shipmentPackages;
// first we scan the shipment items and count the quantity of each product that is being shipped
quantityShippedByProduct = [:];
quantityInShipmentByProduct = [:];
shipmentItems = shipment.getRelated("ShipmentItem");
shipmentItems.each { shipmentItem ->
    productId = shipmentItem.productId;
    shipped = quantityShippedByProduct.get(productId);
    if (UtilValidate.isEmpty(shipped))
    {
        shipped = 0 as Double;
    }
    shipped += shipmentItem.getDouble("quantity").doubleValue();
    quantityShippedByProduct.put(productId, shipped);
    quantityInShipmentByProduct.put(productId, shipped);
}

// Add in the total of all previously shipped items
previousShipmentIter = delegator.find("Shipment",
        EntityCondition.makeCondition(
            UtilMisc.toList(
                EntityCondition.makeCondition("primaryOrderId", EntityOperator.EQUALS, shipment.getString("primaryOrderId")),
                EntityCondition.makeCondition("shipmentTypeId", EntityOperator.EQUALS, "SALES_SHIPMENT"),
                EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO,
                        ObjectType.simpleTypeConvert(shipment.getString("createdDate"), "Timestamp", null, null))
            ),
        EntityOperator.AND), null, null, null, null);

while (previousShipmentItem = previousShipmentIter.next()) 
{
    if (!previousShipmentItem.shipmentId.equals(shipment.shipmentId)) 
    {
        previousShipmentItems = previousShipmentItem.getRelated("ShipmentItem");
        previousShipmentItems.each { shipmentItem ->
            productId = shipmentItem.productId;
            shipped = quantityShippedByProduct.get(productId);
            if (UtilValidate.isEmpty(shipped))
            {
                shipped = new Double(0);
            }
            shipped += shipmentItem.getDouble("quantity").doubleValue();
            quantityShippedByProduct.put(productId, shipped);
        }
    }
}
previousShipmentIter.close();

// next scan the order items (via issuances) to count the quantity of each product requested
quantityRequestedByProduct = [:];
countedOrderItems = [:]; // this map is only used to keep track of the order items already counted
order = shipment.getRelatedOne("PrimaryOrderHeader");
issuances = order.getRelated("ItemIssuance");
issuances.each { issuance ->
    orderItem = issuance.getRelatedOne("OrderItem");
    productId = orderItem.productId;
    if (!countedOrderItems.containsKey(orderItem.orderId + orderItem.orderItemSeqId)) 
    {
        countedOrderItems.put(orderItem.orderId + orderItem.orderItemSeqId, null);
        requested = quantityRequestedByProduct.get(productId);
        if (UtilValidate.isEmpty(requested)) 
        {
            requested = new Double(0);
        }
        cancelQuantity = orderItem.getDouble("cancelQuantity");
        quantity = orderItem.getDouble("quantity");
        requested += quantity.doubleValue() - (cancelQuantity ? cancelQuantity.doubleValue() : 0);
        quantityRequestedByProduct.put(productId, requested);
    }
}

// for each package, we want to list the quantities and details of each product
packages = []; // note we assume that the package number is simply the index + 1 of this list
shipmentPackages.each { shipmentPackage ->
    contents = shipmentPackage.getRelated("ShipmentPackageContent", ['shipmentItemSeqId']);

    // each line is one logical Product and the quantities associated with it
    lines = [];
    contents.each { content ->
        shipmentItem = content.getRelatedOne("ShipmentItem");
        product = shipmentItem.getRelatedOne("Product");
        productTypeId = product.get("productTypeId");

        line = [:];
        line.shipmentItemSeqId = shipmentItem.shipmentItemSeqId;
        line.product = product;
        line.quantityRequested = quantityRequestedByProduct.get(product.productId);
        line.quantityInPackage = content.quantity;
        if (CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", productTypeId, "parentTypeId", "MARKETING_PKG_PICK") && line.quantityInPackage > line.quantityRequested) 
        {
            line.quantityInPackage = line.quantityRequested;
        }
        line.quantityInShipment = quantityInShipmentByProduct.get(product.productId);
        if (CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", productTypeId, "parentTypeId", "MARKETING_PKG_PICK") && line.quantityInShipment > line.quantityRequested) 
        {
            line.quantityInShipment = line.quantityRequested;
        }
        line.quantityShipped = quantityShippedByProduct.get(product.productId);
        if (CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", productTypeId, "parentTypeId", "MARKETING_PKG_PICK") && line.quantityShipped > line.quantityRequested) 
        {
            line.quantityShipped = line.quantityRequested;
        }
        lines.add(line);
    }
    packages.add(lines);
}

context.packages = packages;


//get a list of all invoices
allInvoices = new HashSet();
orderBilling = delegator.findByAnd("OrderItemBilling", [orderId : order.orderId], ["invoiceId"]);
orderBilling.each { billingGv ->
    allInvoices.add(billingGv.invoiceId);
}
context.invoices = allInvoices;