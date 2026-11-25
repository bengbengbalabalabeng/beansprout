@import dev.danvega.beansprout.view.Site
@param Site site
@param boolean allCurrent = false
@if(!site.tags().isEmpty())
  <nav class="filter-bar" aria-label="Tags">
    <a class="filter" href="${site.home()}" aria-current="${allCurrent ? "page" : null}">All</a>
    @for(var t : site.tags())
      <a class="filter" href="${t.url()}" aria-current="${t.current() ? "page" : null}"># ${t.name()} <span class="cnt">(${t.count()})</span></a>
    @endfor
  </nav>
@endif
