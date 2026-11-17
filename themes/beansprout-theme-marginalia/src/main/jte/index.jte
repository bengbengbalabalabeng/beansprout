@import dev.danvega.beansprout.view.IndexPage
@param IndexPage page
!{var first = page.latest().page().equals("1");}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = first ? page.site().name() : "Page " + page.latest().page() + " · " + page.site().name(),
  content = @`
    <div class="page-head spread">
      <p class="spread-margin"><span class="margin-label">The notebook</span>@if(!first)<span class="margin-line">Page ${page.latest().page()} of ${page.latest().pages()}</span>@endif</p>
      <div class="spread-text">
        <h1 class="page-title">${first ? "Latest entries" : "Earlier entries"}</h1>
        <p class="page-intro">Newest first.</p>
      </div>
    </div>
    @template.stream(listing = page.latest())
  `
)
