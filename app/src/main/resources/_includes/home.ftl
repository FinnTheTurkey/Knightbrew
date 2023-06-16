<#include "base_top.ftl">

<#if content != "">
    <div class="full-card">
        <div class="left">
            ${content}
        </div>
    </div>
</#if>

<!-- <h1>Recent Articles</h1> -->


<div class="articles">
  <div class="col" data-columns>
     <#list items as item >
      <div class="card item" data-aos="fade-up" onclick="location.href='${item.url}'">
        <a class="card-title times" href="${item.url}">${item.title}</a>
        <#if item.has_image>
        	<img loading=lazy class="content-image" src="${item.image_url}" />
        </#if>
        <div class="card-info">
          <div class="card-meta">
            <span class="card-author">${item.author}</span>
          </div>
          <p class="card-blurb line-clamp">${ item.blurb }</p>
        </div>
      </div>
    </#list>
  </div>
</div>
<#include "base_bottom.ftl">
