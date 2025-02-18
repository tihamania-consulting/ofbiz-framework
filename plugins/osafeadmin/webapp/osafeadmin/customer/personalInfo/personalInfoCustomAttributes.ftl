<#if partyId?exists && partyId?has_content>
    <#assign partyAttributes = delegator.findByAnd("PartyAttribute", {"partyId" : partyId})?if_exists />
</#if>

<#if customPartyAttributeList?has_content>
      <div class="${request.getAttribute("attributeClass")!}">
      <#list customPartyAttributeList as customPartyAttribute>
        <#assign mandatory= customPartyAttribute.Mandatory!"N"/>
        <#if mandatory =="NA">
              <#assign mandatory = "N"/>
        <#elseif mandatory =="SYS_YES">
              <#assign attributeMandatory = "Y"/>
        <#elseif mandatory =="SYS_NO">
              <#assign attributeMandatory = "N"/>
        <#elseif mandatory =="NO">
              <#assign attributeMandatory = "N"/>
        <#elseif mandatory =="YES">
              <#assign mandatory = "Y"/>
        </#if>
        <div class="infoRow">
            <div class="infoEntry">
                <div class="infoCaption">
                    <label><#if mandatory == 'Y'><span class="required">*</span></#if> ${customPartyAttribute.Caption!}</label>
                </div>
                <div class="<#if customPartyAttribute.Type == 'RADIO_BUTTON' || customPartyAttribute.Type == 'CHECKBOX'>entry checkbox<#else>infoValue</#if>">
                    <#assign attrValue =  Static["org.apache.ofbiz.base.util.StringUtil"].wrapString(parameters.get("${customPartyAttribute.AttrName!}"))! />
                    <#assign dobMonth = ""/>
					<#assign dobDay = ""/>
					<#assign dobYear = ""/>
						
                    <#if customPartyAttribute.Type == 'CHECKBOX' || customPartyAttribute.Type == 'DROP_DOWN_MULTI'>
                        <#if partyAttributes?has_content && !errorMessageList?has_content>
	                        <#list partyAttributes as partyAttribute>
	                            <#if partyAttribute.attrName == customPartyAttribute.AttrName>
	                                <#assign attrValue = partyAttribute.attrValue!"">
	                                <#break>
	                            </#if>
	                        </#list>
                        </#if>
                    <#elseif customPartyAttribute.Type == 'DATE_MMDD'>
                        <#if partyAttributes?has_content>
                            <#list partyAttributes as partyAttribute>
	                            <#if partyAttribute.attrName == customPartyAttribute.AttrName>
	                                <#assign DOB_MMDD = partyAttribute.attrValue!"">
                                    <#if DOB_MMDD?has_content && DOB_MMDD?length gt 4>
                                        <#assign dobMonth= DOB_MMDD.substring(0, 2) />
                                        <#assign dobDay = DOB_MMDD.substring(3,5) />
                                    </#if>
	                                <#break>
	                            </#if>
	                        </#list>
                        </#if>
                    <#elseif customPartyAttribute.Type = 'DATE_MMDDYYYY'>
                        <#if partyAttributes?has_content>
                            <#list partyAttributes as partyAttribute>
	                            <#if partyAttribute.attrName == customPartyAttribute.AttrName>
	                                <#assign DOB_MMDDYYYY = partyAttribute.attrValue!"">
								      <#if DOB_MMDDYYYY?has_content && (DOB_MMDDYYYY?length gt 9)>
								          <#assign dobMonth= DOB_MMDDYYYY.substring(0, 2) />
								          <#assign dobDay = DOB_MMDDYYYY.substring(3,5) />
								          <#assign dobYear = DOB_MMDDYYYY.substring(6,10) />
								      </#if>
	                                <#break>
	                            </#if>
	                        </#list>
                        </#if>
                    <#elseif customPartyAttribute.Type = 'DATE_DDMM'>
                        <#if partyAttributes?has_content>
                            <#list partyAttributes as partyAttribute>
	                            <#if partyAttribute.attrName == customPartyAttribute.AttrName>
	                                <#assign DOB_DDMM = partyAttribute.attrValue!"">
								      <#if DOB_DDMM?has_content && (DOB_DDMM?length gt 5)>
								          <#assign dobDay= DOB_DDMM.substring(0, 2) />
								          <#assign dobMonth = DOB_DDMM.substring(3,5) />
								      </#if>
	                                <#break>
	                            </#if>
	                        </#list>
                        </#if>
                    <#elseif customPartyAttribute.Type = 'DATE_DDMMYYYY'>
                        <#if partyAttributes?has_content>
                            <#list partyAttributes as partyAttribute>
	                            <#if partyAttribute.attrName == customPartyAttribute.AttrName>
	                                <#assign DOB_DDMMYYYY = partyAttribute.attrValue!"">
								      <#if DOB_DDMMYYYY?has_content && (DOB_DDMMYYYY?length gt 9)>
								          <#assign dobDay= DOB_DDMMYYYY.substring(0, 2) />
								          <#assign dobMonth = DOB_DDMMYYYY.substring(3,5) />
								          <#assign dobYear = DOB_DDMMYYYY.substring(6,10) />
								      </#if>
	                                <#break>
	                            </#if>
	                        </#list>
                        </#if>
                    <#else>
                        <#if !(attrValue?has_content) && partyAttributes?has_content>
	                        <#list partyAttributes as partyAttribute>
	                            <#if partyAttribute.attrName == customPartyAttribute.AttrName>
	                                <#assign attrValue = partyAttribute.attrValue!"">
	                                <#break>
	                            </#if>
	                        </#list>
                        </#if>
                    </#if>
                    
                    <#if customPartyAttribute.Type == 'ENTRY'>
                        <input type="text" class="FIELD_ERROR_${customPartyAttribute_index}" name = "${customPartyAttribute.AttrName}" maxLength = "${customPartyAttribute.MaxLength!}" value="${attrValue!}" />
                    <#elseif customPartyAttribute.Type == 'ENTRY_BOX'>
                        <textarea name = "${customPartyAttribute.AttrName}" class="FIELD_ERROR_${customPartyAttribute_index}" class="smallArea" <#if customPartyAttribute.MaxLength?has_content>onKeyPress='setMaxLength(this);' maxLength="${customPartyAttribute.MaxLength!}" </#if>>${attrValue!}</textarea>
                    <#elseif customPartyAttribute.Type == 'RADIO_BUTTON'>
                        <#assign valueList =  customPartyAttribute.ValueList! />
                        <#if valueList?has_content>
                            <#assign values = valueList?split(',')>
                            <#list values as value>
                                 <#assign valueTrim = value?string?trim />
                                 <input type="radio" name = "${customPartyAttribute.AttrName}" class="FIELD_ERROR_${customPartyAttribute_index}" <#if valueTrim?upper_case == attrValue>checked</#if> value="${valueTrim?upper_case}"/>
                                 <span class="radioOptionText">${valueTrim}</span>
                            </#list>
                        </#if>
                    <#elseif customPartyAttribute.Type == 'CHECKBOX'>
                        <#assign valueList =  customPartyAttribute.ValueList! />
                        <#if valueList?has_content>
                            <#assign values = valueList?split(',')>
                            <#list values as value>
                                 <#assign value = value?string?trim />
                                 <#assign valueTrim = value?string?trim />
                                 <input type="checkbox" name="${customPartyAttribute.AttrName}" class="FIELD_ERROR_${customPartyAttribute_index}" value="${valueTrim?upper_case}" <#if attrValue?contains(valueTrim?upper_case)>checked = "checked"</#if>/>
                                 <span class="checkboxOptionText">${valueTrim}</span>
                            </#list>
                        </#if>
                    <#elseif customPartyAttribute.Type == 'DROP_DOWN'>
                        <#assign valueList =  customPartyAttribute.ValueList! />
                        <#if valueList?has_content>
                            <select name="${customPartyAttribute.AttrName}" class="FIELD_ERROR_${customPartyAttribute_index}">
                                <#assign values = valueList?split(',')>
                                <#list values as value>
                                    <#assign value = value?string?trim />
                                    <#assign valueTrim = value?string?trim />
                                    <option value="${valueTrim?upper_case}" <#if valueTrim?upper_case == attrValue>selected</#if>>${valueTrim!}</option>
                                </#list>
                            </select>
                        </#if>
                    <#elseif customPartyAttribute.Type == 'DROP_DOWN_MULTI'>
                        <#assign valueList =  customPartyAttribute.ValueList! />
                        <#if valueList?has_content>
                            <select name="${customPartyAttribute.AttrName}" class="FIELD_ERROR_${customPartyAttribute_index}" multiple>
                                <#assign values = valueList?split(',')>
                                <#list values as value>
                                    <#assign value = value?string?trim />
                                    <#assign valueTrim = value?string?trim />
                                    <option value="${valueTrim?upper_case}" <#if attrValue?contains(valueTrim?upper_case)>selected</#if>>${valueTrim!}</option>
                                </#list>
                            </select>
                        </#if>
                    <#elseif customPartyAttribute.Type == 'DATE_MMDD'>
                        <select name="${customPartyAttribute.AttrName}_MM" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobMonth = parameters.get("${customPartyAttribute.AttrName}_MM")!dobMonth!"">
                            <#if dobMonth?has_content && (dobMonth?length gt 1)>
                                <option value="${dobMonth?if_exists}">${dobMonth?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Month}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddMonths")}
                        </select>
                        <select name="${customPartyAttribute.AttrName}_DD" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobDay = parameters.get("${customPartyAttribute.AttrName}_DD")!dobDay!"">
                            <#if dobDay?has_content && (dobDay?length gt 1)>
                                <option value="${dobDay?if_exists}">${dobDay?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Day}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddDays")}
                        </select>
                    <#elseif customPartyAttribute.Type == 'DATE_MMDDYYYY'>
                        <select name="${customPartyAttribute.AttrName}_MM" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobMonth = parameters.get("${customPartyAttribute.AttrName}_MM")!dobMonth!"">
                            <#if dobMonth?has_content && (dobMonth?length gt 1)>
                                <option value="${dobMonth?if_exists}">${dobMonth?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Month}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddMonths")}
                        </select>
                        <select class="FIELD_ERROR_${customPartyAttribute_index}" name="${customPartyAttribute.AttrName}_DD">
                            <#assign dobDay = parameters.get("${customPartyAttribute.AttrName}_DD")!dobDay!"">
                            <#if dobDay?has_content && (dobDay?length gt 1)>
                                <option value="${dobDay?if_exists}">${dobDay?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Day}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddDays")}
                        </select>
                        <select name="${customPartyAttribute.AttrName}_YYYY" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobYear = parameters.get("${customPartyAttribute.AttrName}_YYYY")!dobYear!"">
			                <#if dobYear?has_content && (dobYear?length gt 1)>
			                    <option value="${dobYear?if_exists}">${dobYear?if_exists}</option>
			                </#if>
                            <option value="">${uiLabelMap.DOB_Year}</option>
                            ${screens.render("component://osafeadmin/widget/CommonScreens.xml#ddYears")}
                        </select>
                    <#elseif customPartyAttribute.Type == 'DATE_DDMM'>
                        <select name="${customPartyAttribute.AttrName}_DD" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobDay = parameters.get("${customPartyAttribute.AttrName}_DD")!dobDay!"">
                            <#if dobDay?has_content && (dobDay?length gt 1)>
                                <option value="${dobDay?if_exists}">${dobDay?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Day}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddDays")}
                        </select>
                        <select  name="${customPartyAttribute.AttrName}_MM" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobMonth = parameters.get("${customPartyAttribute.AttrName}_MM")!dobMonth!"">
                            <#if dobMonth?has_content && (dobMonth?length gt 1)>
                                <option value="${dobMonth?if_exists}">${dobMonth?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Month}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddMonths")}
                        </select>
                    <#elseif customPartyAttribute.Type == 'DATE_DDMMYYYY'>
                        <select name="${customPartyAttribute.AttrName}_DD" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobDay = parameters.get("${customPartyAttribute.AttrName}_DD")!dobDay!"">
                            <#if dobDay?has_content && (dobDay?length gt 1)>
                                <option value="${dobDay?if_exists}">${dobDay?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Day}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddDays")}
                        </select>
                        <select name="${customPartyAttribute.AttrName}_MM" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobMonth = parameters.get("${customPartyAttribute.AttrName}_MM")!dobMonth!"">
                            <#if dobMonth?has_content && (dobMonth?length gt 1)>
                                <option value="${dobMonth?if_exists}">${dobMonth?if_exists}</option>
                            </#if>
                            <option value="">${uiLabelMap.DOB_Month}</option>
                            ${screens.render("component://osafe/widget/CommonScreens.xml#ddMonths")}
                        </select>
                        <select name="${customPartyAttribute.AttrName}_YYYY" class="FIELD_ERROR_${customPartyAttribute_index}">
                            <#assign dobYear = parameters.get("${customPartyAttribute.AttrName}_YYYY")!dobYear!"">
			                <#if dobYear?has_content && (dobYear?length gt 1)>
			                    <option value="${dobYear?if_exists}">${dobYear?if_exists}</option>
			                </#if>
                            <option value="">${uiLabelMap.DOB_Year}</option>
                            ${screens.render("component://osafeadmin/widget/CommonScreens.xml#ddYears")}
                        </select>
                    </#if>
                </div>
            </div>            
        </div>
        </#list>
    </div>
   
</#if>
