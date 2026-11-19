@import dev.danvega.beansprout.view.IndexPage
@param IndexPage page
!{var first = page.latest().page().equals("1");}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = first ? page.site().name() : "Page " + page.latest().page() + " · " + page.site().name(),
  content = @`
    <h1 class="visually-hidden">${first ? "Writing" : "Writing, page " + page.latest().page() + " of " + page.latest().pages()}</h1>
    @template.posts(listing = page.latest(), year = page.site().year())
  `
)
