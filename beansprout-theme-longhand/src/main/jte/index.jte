@import dev.danvega.beansprout.view.IndexPage
@param IndexPage page
@template.layout(
  site = page.site(),
  title = page.latest().page().equals("1") ? page.site().name() : "Page " + page.latest().page() + " · " + page.site().name(),
  content = @`
    <h1 class="page-title">Latest posts</h1>
    @template.cards(listing = page.latest())
  `,
  sidebar = @`@template.tags(site = page.site())`
)
