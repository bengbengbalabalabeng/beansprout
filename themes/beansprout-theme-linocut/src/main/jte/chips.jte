@import dev.danvega.beansprout.view.Site
@param Site site
@if(!site.tags().isEmpty())
  <nav class="chips" aria-label="Tags">
    @for(var t : site.tags())
      <a href="${t.url()}" aria-current="${t.current() ? "page" : null}">${t.name()} (${t.count()})</a>
    @endfor
  </nav>
@endif
