@import dev.danvega.beansprout.view.TagPage
@param TagPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " · " + page.site().name(),
  tagFeed = page.feed(),
  tagFeedTitle = page.site().name() + ": " + page.name(),
  content = @`
    <div class="split">
      <div class="split-main">
        <header class="page-head">
          <p class="kicker">Tag</p>
          <h1 class="page-title">${page.name()}</h1>
          <p class="meta"><span>${page.count()}</span>@if(!page.listing().pages().equals("1"))<span>Page ${page.listing().page()} of ${page.listing().pages()}</span>@endif</p>
        </header>
        <div class="rows">
          @for(var c : page.listing().cards())
            @template.story(card = c, kind = "story-row")
          @endfor
        </div>
        @template.pager(listing = page.listing(), olderLabel = "Older posts tagged " + page.name())
      </div>
      <aside class="split-side">
        @template.tags(site = page.site())
      </aside>
    </div>
  `
)
