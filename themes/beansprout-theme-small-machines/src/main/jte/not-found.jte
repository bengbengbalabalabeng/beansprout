@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Page not found · " + page.site().name(),
  content = @`
    <section class="w alert" aria-labelledby="alert-title">
      <div class="tb"><span class="t">Error · file not found</span><span class="x" aria-hidden="true"><i>–</i><i>×</i></span></div>
      <div class="al">
        <div class="ico q" aria-hidden="true">?</div>
        <div>
          <p class="kicker">404</p>
          <h1 id="alert-title">There’s no page at this address</h1>
          <dl class="wwn">
            <dt>What happened</dt>
            <dd><p>You asked for <span class="code404">${page.path()}</span> and nothing lives there.</p></dd>
            <dt>Right now</dt>
            <dd><p>The address may have a typo, or the link that brought you here is old. The post may have been renamed.</p></dd>
            <dt>What you can do</dt>
            <dd>
              <p>Start from the latest posts, or look through all <span class="alert-total">${page.postCount()}</span> in the archive.</p>
              <div class="btns">
                <a class="b pri" href="${page.site().home()}">Latest posts</a>
                <a class="b" href="${page.site().archive()}">Archive</a>
              </div>
            </dd>
          </dl>
        </div>
      </div>
    </section>
    @if(!page.recent().isEmpty())
      <section class="w rel" aria-labelledby="recent-title">
        <div class="tb"><span class="t" id="recent-title">Recent posts</span><span class="x" aria-hidden="true"><i>–</i><i>×</i></span></div>
        <ul>
          @for(var m : page.recent())
            <li><a href="${m.url()}"><strong>${m.title()}</strong><span><time datetime="${m.dateTime()}">${m.date()}</time> · ${m.reading()}</span></a></li>
          @endfor
        </ul>
      </section>
    @endif
  `
)
