@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
!{var site = page.site();}
@template.layout(
  site = site,
  meta = page.meta(),
  title = "Page not found · " + site.name(),
  top = @`
    <header class="card-block card-block-short">
      <div class="card-block-inner">
        <div class="card-block-top">
          <p class="card-block-home"><a class="card-block-home-link" href="${site.home()}">${site.name()}</a></p>
          @template.nav(site = site)
        </div>
        <div class="card-block-text">
          <p class="card-kicker">404</p>
          <h1 class="card-name">Not here</h1>
          <p class="card-bio">There is no page at this address. A link may be wrong, or the post was renamed.</p>
        </div>
      </div>
    </header>
  `,
  content = @`
    @if(!page.path().isEmpty())
      <p class="card-lost">You asked for <span class="card-path">${page.path()}</span>.</p>
    @endif
    @if(!page.recent().isEmpty())
      <section class="card-section" aria-labelledby="recent-title">
        <h2 class="card-label" id="recent-title">Newest writing</h2>
        <ul class="card-rows">
          @for(var m : page.recent())
            <li class="card-row"><a class="card-row-title" href="${m.url()}">${m.title()}</a> <time class="card-row-date" datetime="${m.dateTime()}">${m.date()}</time></li>
          @endfor
        </ul>
        <p class="card-more">The <a href="${site.archive()}">archive</a> has all <span class="card-not-found-total">${page.postCount()}</span>.</p>
      </section>
    @endif
    @template.topics(site = site)
  `
)
