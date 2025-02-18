package shipping;

import javolution.util.FastList;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.util.EntityUtil;

orderBy = ["partyId"];

List carrierList = new ArrayList();

partyExpr= new ArrayList();
partyStatusExpr= new ArrayList();
partyCond = null;
partyStatusCond = null;

partyExpr.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "CARRIER"));
partyExpr.add(EntityCondition.makeCondition("partyTypeId", EntityOperator.EQUALS, "PARTY_GROUP"));
partyCond = EntityCondition.makeCondition(partyExpr, EntityOperator.AND);

partyStatusExpr.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PARTY_ENABLED"));
partyStatusExpr.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null));
partyStatusCond = EntityCondition.makeCondition(partyStatusExpr, EntityOperator.OR);

carrierList = delegator.findList("PartyRoleAndPartyDetail",EntityCondition.makeCondition([partyCond, partyStatusCond], EntityOperator.AND), UtilMisc.toSet("partyId", "groupName", "roleTypeId"), orderBy, null, false);
partyGroupPartyIds = EntityUtil.getFieldListFromEntityList(delegator.findList("PartyGroup",null, UtilMisc.toSet("partyId"), orderBy, null, false), "partyId", true);

context.resultList = carrierList;
context.partyGroupPartyIds = partyGroupPartyIds;