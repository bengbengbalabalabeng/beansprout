@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Page not found · " + page.site().name(),
  content = @`
    <section class="notice red" aria-labelledby="nh">
      <div class="notice-head">
        <p class="kicker">404 · Page not found</p>
        <h1 id="nh">Nothing is printed at this address</h1>
      </div>
      <dl class="wwn">
        @if(!page.path().isEmpty())
          <dt>What happened</dt>
          <dd><p>You asked for <span class="code404">${page.path()}</span>, and there is no page there.</p></dd>
        @endif
        <dt>What it means now</dt>
        <dd><p>The address may have a typo, or the post was renamed.</p></dd>
        <dt>What to do next</dt>
        <dd>
          <p>Start from the latest posts, or try the archive, which holds <span class="notice-total">${page.postCount()}</span>.</p>
          <div class="btns">
            <a class="btn alt" href="${page.site().home()}">Latest posts</a>
            <a class="btn alt" href="${page.site().archive()}">Archive</a>
          </div>
        </dd>
      </dl>
    </section>
    @if(!page.recent().isEmpty())
      <section class="rel" aria-labelledby="rel-h">
        <h2 id="rel-h">Latest posts</h2>
        <ul class="minis">
          @for(var m : page.recent())
            <li><a href="${m.url()}"><strong>${m.title()}</strong><span><time datetime="${m.dateTime()}">${m.date()}</time> · ${m.reading()}</span></a></li>
          @endfor
        </ul>
      </section>
    @endif
  `
)
