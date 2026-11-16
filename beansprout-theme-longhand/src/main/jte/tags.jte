@import dev.danvega.beansprout.view.Site
@param Site site
@if(!site.tags().isEmpty())
  <h2 class="label sidebar-label">Tags</h2>
  <ul class="tag-list">
    @for(var t : site.tags())
      <li><a class="tag-link" href="${t.url()}" aria-current="${t.current() ? "page" : null}"><span class="tag-link-name">${t.name()}</span> <span class="tag-link-count">${t.count()}</span></a></li>
    @endfor
  </ul>
@endif
