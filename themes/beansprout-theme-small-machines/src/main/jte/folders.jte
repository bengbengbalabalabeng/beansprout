@import dev.danvega.beansprout.view.Site
@param Site site
@if(!site.tags().isEmpty())
  <nav class="topics" aria-label="Tags">
    @for(var t : site.tags())
      <a href="${t.url()}" aria-current="${t.current() ? "page" : null}"><i class="fold"></i><span><span class="topic-name">${t.name()}</span> (<span class="topic-count">${t.count()}</span>)</span></a>
    @endfor
  </nav>
@endif
