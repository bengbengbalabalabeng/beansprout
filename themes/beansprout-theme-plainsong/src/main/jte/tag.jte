@import dev.danvega.beansprout.view.TagPage
@param TagPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " · " + page.site().name(),
  tagFeed = page.feed(),
  tagFeedTitle = page.site().name() + ": " + page.name(),
  content = @`
    <div class="page-head">
      <h1 class="page-title">Tagged <span class="tag-title">${page.name()}</span></h1>
      <p class="page-note"><span class="tag-count">${page.count()}</span>@if(!page.listing().pages().equals("1")) · page ${page.listing().page()} of ${page.listing().pages()}@endif</p>
    </div>
    @template.posts(listing = page.listing(), year = page.site().year(), empty = "Nothing is tagged with this yet.")
  `
)
