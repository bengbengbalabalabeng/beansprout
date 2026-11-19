@import dev.danvega.beansprout.view.TagPage
@param TagPage page
!{var site = page.site();}
!{var others = site.tags().stream().filter(t -> !t.current()).toList();}
@template.layout(
  site = site,
  meta = page.meta(),
  title = page.name() + " · " + site.name(),
  top = @`@template.bar(site = site)`,
  content = @`
    <header class="card-head">
      <p class="card-label">Topic</p>
      <h1 class="card-poster">${page.name()}</h1>
      <p class="card-poster-count">${page.count()}</p>
    </header>
    @if(!others.isEmpty())
      <nav class="card-strip" aria-label="Other topics">
        <ul class="card-strip-list">
          @for(var t : others)
            <li><a class="card-strip-link" href="${t.url()}"><span class="card-strip-name">${t.name()}</span> <span class="card-strip-count">${t.count()}</span></a></li>
          @endfor
        </ul>
      </nav>
    @endif
    <section class="card-section" aria-labelledby="writing-title">
      <h2 class="card-label" id="writing-title">Writing</h2>
      @template.rows(listing = page.listing(), empty = "Nothing is filed under this topic yet.")
    </section>
  `
)
