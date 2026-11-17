@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Page not found · " + page.site().name(),
  width = "narrow",
  content = @`
    <section class="state" aria-labelledby="state-h">
      <p class="kicker">Error 404</p>
      <h1 class="page-title" id="state-h">There is no page at this address</h1>
      <p class="state-lead">You asked for <code class="path">${page.path()}</code>, and nothing lives there.</p>
      <h2 class="state-sub">What it means</h2>
      <p>Most likely there is a typo in the address, or the link that brought you here was cut short.</p>
      @if(!page.recent().isEmpty())
        <h2 class="state-sub">The latest posts</h2>
        <ul class="minis">
          @for(var m : page.recent())
            <li class="mini">
              <a class="mini-title" href="${m.url()}">${m.title()}</a>
              <span class="mini-meta"><time datetime="${m.dateTime()}">${m.date()}</time><span class="sep" aria-hidden="true">·</span>${m.reading()}</span>
            </li>
          @endfor
        </ul>
      @endif
      <p class="actions"><a class="btn btn-ink" href="${page.site().home()}">Latest posts</a><a class="btn" href="${page.site().archive()}">All ${page.postCount()} in the archive</a></p>
    </section>
  `
)
