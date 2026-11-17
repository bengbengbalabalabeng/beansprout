@import dev.danvega.beansprout.view.TagPage
@param TagPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " · " + page.site().name(),
  content = @`
    <header class="page-head">
      <p class="page-kicker">Tag</p>
      <h1 class="tag-title">${page.name()}</h1>
      <p class="page-lede"><span class="tag-count">${page.count()}.</span></p>
    </header>
    @template.cards(listing = page.listing(), olderLabel = "Older posts tagged " + page.name())
  `,
  sidebar = @`@template.tags(site = page.site())`
)
