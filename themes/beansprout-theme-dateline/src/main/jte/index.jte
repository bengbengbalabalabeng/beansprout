@import dev.danvega.beansprout.view.IndexPage
@param IndexPage page
!{var latest = page.latest();}
!{var first = latest.page().equals("1");}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = first ? page.site().name() : "Page " + latest.page() + " · " + page.site().name(),
  content = @`
    @if(first)
      <h1 class="sr">Latest posts</h1>
      <div class="front">
        <div class="front-main">
          @if(latest.cards().isEmpty())
            @template.empty(site = page.site())
          @else
            !{var lead = latest.cards().getFirst();}
            <article class="lead${lead.picture().isEmpty() ? " lead-plain" : ""}">
              @if(!lead.picture().isEmpty())<figure class="lead-pic"><img src="${lead.picture()}" alt="${lead.pictureAlt()}" width="1600" height="900"></figure>@endif
              <div class="lead-text">
                @template.dateline(tags = lead.tags(), date = lead.date(), dateTime = lead.dateTime(), reading = lead.reading())
                <h2 class="lead-title"><a href="${lead.url()}">${lead.title()}</a></h2>
                @if(!lead.excerpt().isEmpty())<p class="lead-excerpt">${lead.excerpt()}</p>@endif
                @if(lead.tags().size() > 1)@template.filed(tags = lead.tags().subList(1, lead.tags().size()), lead = "Also filed under")@endif
              </div>
            </article>
            @if(latest.cards().size() > 1)
              <div class="columns">
                @for(var c : latest.cards().subList(1, latest.cards().size()))
                  @template.story(card = c)
                @endfor
              </div>
            @endif
          @endif
          @template.pager(listing = latest)
        </div>
        <aside class="front-side" aria-label="Tags and the feed">
          @template.tags(site = page.site())
          @template.follow(site = page.site())
        </aside>
      </div>
    @else
      <header class="page-head">
        <p class="kicker">Latest posts</p>
        <h1 class="page-title">Older posts</h1>
        <p class="page-sub">Page ${latest.page()} of ${latest.pages()}</p>
      </header>
      <div class="columns columns-3">
        @for(var c : latest.cards())
          @template.story(card = c)
        @endfor
      </div>
      @template.pager(listing = latest)
    @endif
  `
)
