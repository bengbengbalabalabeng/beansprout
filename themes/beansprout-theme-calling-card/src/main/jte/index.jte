@import dev.danvega.beansprout.view.IndexPage
@param IndexPage page
!{var site = page.site();}
!{var first = page.latest().page().equals("1");}
@template.layout(
  site = site,
  meta = page.meta(),
  title = first ? site.name() : "Page " + page.latest().page() + " · " + site.name(),
  top = @`
    @if(first)
      <header class="card-block">
        <div class="card-block-inner">
          @template.nav(site = site)
          <div class="card-block-text">
            <h1 class="card-name">${site.name()}</h1>
            @if(!site.description().isEmpty())<p class="card-bio">${site.description()}</p>@endif
          </div>
          @if(!site.photo().isEmpty())<img class="card-photo" src="${site.photo()}" alt="" width="384" height="384">@endif
        </div>
      </header>
    @else
      @template.bar(site = site)
    @endif
  `,
  content = @`
    @if(first)
      @template.topics(site = site)
      <section class="card-section" aria-labelledby="writing-title">
        <h2 class="card-label" id="writing-title">Writing</h2>
        @template.rows(listing = page.latest())
      </section>
    @else
      <header class="card-head">
        <h1 class="card-page-title">Writing</h1>
        <p class="card-page-sub">Older posts, newest first.</p>
      </header>
      @template.rows(listing = page.latest())
    @endif
  `
)
