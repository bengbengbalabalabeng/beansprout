@import dev.danvega.beansprout.view.TagPage
@param TagPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " · " + page.site().name(),
  content = @`
    <header class="t-head">
      <p class="kicker">Tag</p>
      <h1>${page.name()}</h1>
    </header>
    @template.chips(site = page.site())
    <p class="tally"><span class="tag-count">${page.count()}</span>, newest first.</p>
    <ul class="rows">
      @for(var r : page.listing().cards())
        <li class="post-row">
          <div>
            <h2><a href="${r.url()}">${r.title()}</a></h2>
            @if(!r.excerpt().isEmpty())<p>${r.excerpt()}</p>@endif
          </div>
          <p class="meta"><time datetime="${r.dateTime()}">${r.date()}</time><span>${r.reading()}</span></p>
        </li>
      @endfor
    </ul>
    @template.pager(listing = page.listing(), olderLabel = "Older posts tagged " + page.name())
  `
)
