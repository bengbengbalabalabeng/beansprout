@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Page not found · " + page.site().name(),
  content = @`
    <div class="page-head">
      <p class="page-kicker">Error 404</p>
      <h1 class="page-title">Nothing here.</h1>
      @if(!page.path().isEmpty())
        <p class="page-note">There is no page at <code class="plainsong-path">${page.path()}</code>.</p>
      @else
        <p class="page-note">There is no page at this address.</p>
      @endif
    </div>
    @if(!page.recent().isEmpty())
      <section class="recent" aria-labelledby="recent-title">
        <h2 class="section-title" id="recent-title">Recent posts</h2>
        <ul class="rows">
          @for(var m : page.recent())
            <li class="recent-row"><a href="${m.url()}">${m.title()}</a> <time class="date" datetime="${m.dateTime()}">${m.date()}</time></li>
          @endfor
        </ul>
      </section>
    @endif
    <p class="see-all">See <a href="${page.site().archive()}">all <span class="not-found-total">${page.postCount()}</span> in the archive</a>, or start from the <a href="${page.site().home()}">newest</a>.</p>
  `
)
