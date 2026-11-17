@import dev.danvega.beansprout.view.Site
@param Site site
@if(!site.tags().isEmpty())
  <nav class="box" aria-labelledby="tags-h">
    <h2 class="box-title" id="tags-h">Tags</h2>
    <ul class="tag-list">
      @for(var t : site.tags())
        <li><a href="${t.url()}" aria-current="${t.current() ? "page" : null}"><span>${t.name()}</span> <span class="count">${t.count()}</span></a></li>
      @endfor
    </ul>
  </nav>
@endif
