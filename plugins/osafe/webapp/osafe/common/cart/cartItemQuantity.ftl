<li class="${request.getAttribute("attributeClass")!}<#if lineIndex == 0> firstRow</#if>">
  <div>
    <label>${uiLabelMap.CartItemQuantityCaption}</label>
    <span>${quantity!}</span>
  </div>
</li>