@import dev.danvega.beansprout.view.TagPage
@param TagPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " · " + page.site().name(),
  tagFeed = page.feed(),
  tagFeedTitle = page.site().name() + ": " + page.name(),
  content = @`
    <div class="page-head spread">
      <p class="spread-margin"><span class="margin-label">Tag</span><span class="margin-line tag-count">${page.count()}</span>@if(!page.listing().pages().equals("1"))<span class="margin-line">Page ${page.listing().page()} of ${page.listing().pages()}</span>@endif</p>
      <div class="spread-text">
        <h1 class="page-title">#<span class="tag-title">${page.name()}</span></h1>
        <p class="page-intro">Every entry tagged ${page.name()}, newest first.</p>
      </div>
    </div>
    @template.stream(listing = page.listing(), empty = "Nothing is filed under this tag yet.")
  `
)
