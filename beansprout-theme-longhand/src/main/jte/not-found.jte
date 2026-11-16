@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  title = "Page not found · " + page.site().name(),
  content = @`
    <section class="panel" aria-labelledby="panel-title">
      <p class="status status-neutral">404 · Page not found</p>
      <h1 class="panel-title" id="panel-title">There’s no page at this address</h1>
      <p class="panel-text">Nothing lives at <code class="panel-path">${page.path()}</code>. The link may have a typo, or the post was renamed.</p>
      @if(!page.recent().isEmpty())
        <h2 class="panel-subtitle">Latest posts</h2>
        <ul class="mini-posts mini-posts-side">
          @for(var m : page.recent())
            <li class="mini-post">
              <a class="mini-post-link" href="${m.url()}">${m.title()}</a>
              <p class="mini-post-meta"><time class="mini-post-date" datetime="${m.dateTime()}">${m.date()}</time><span class="mini-post-sep"> · </span><span class="mini-post-reading">${m.reading()}</span></p>
            </li>
          @endfor
        </ul>
      @endif
      <p class="panel-more"><a class="text-link" href="${page.site().archive()}">All <span class="panel-total">${page.postCount()}</span> in the archive</a></p>
    </section>
  `,
  sidebar = @`@template.tags(site = page.site())`
)
