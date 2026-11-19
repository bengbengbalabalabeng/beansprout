@import dev.danvega.beansprout.view.TagPage
@param TagPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " / " + page.site().name(),
  content = @`
    <h1 class="page-title">Filed under: <span class="tag-title">${page.name()}</span></h1>
    <p class="page-intro"><span class="tag-count">${page.count()}</span>, newest first.@if(!page.listing().pages().equals("1")) Page ${page.listing().page()} of ${page.listing().pages()}.@endif</p>
    @template.memos(listing = page.listing(), empty = "Nothing is filed here yet.")
    @if(page.site().tags().size() > 1)
      <nav class="files" aria-label="Every tag">
        <h2 class="files-title">Other files</h2>
        <ul class="inline-list">
          @for(var t : page.site().tags())
            @if(!t.current())<li><a href="${t.url()}">${t.name()}</a> <span class="files-count">(${t.count()})</span></li>@endif
          @endfor
        </ul>
      </nav>
    @endif
  `
)
