@import dev.danvega.beansprout.view.Site
@param Site site
@param String label = "Topics"
@if(!site.tags().isEmpty())
  <section class="card-section" aria-labelledby="topics-title">
    <h2 class="card-label" id="topics-title">${label}</h2>
    <ul class="card-topics">
      @for(var t : site.tags())
        <li><a class="card-topic" href="${t.url()}"><span class="card-topic-name">${t.name()}</span> <span class="card-topic-count">${t.count()}</span></a></li>
      @endfor
    </ul>
  </section>
@endif
