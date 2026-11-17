@import dev.danvega.beansprout.view.TagPage
@param TagPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " · " + page.site().name(),
  content = @`
    <article class="w folder tint-sky">
      <div class="tb"><span class="t">Tags / ${page.name()}</span><span class="x" aria-hidden="true"><i>–</i><i>×</i></span></div>
      <nav class="path" aria-label="Breadcrumb">
        <a href="${page.site().home()}">${page.site().name()}</a>
        <span aria-hidden="true">›</span>
        <span>Tags</span>
        <span aria-hidden="true">›</span>
        <b aria-current="page">${page.name()}</b>
      </nav>
      @template.folders(site = page.site())
      <div class="s-intro topic-intro">
        <div><p class="kicker">Tag</p><h1>${page.name()}</h1><p class="tag-count">${page.count()}, newest first.</p></div>
      </div>
      <table class="files">
        <caption class="sr">Posts tagged ${page.name()}, newest first</caption>
        <thead><tr><th scope="col">Name</th><th scope="col">Date</th><th scope="col">Reading time</th></tr></thead>
        <tbody>
          @for(var c : page.listing().cards())
            <tr class="file">
              <td><div class="nm"><i class="docic"></i><div><h2><a href="${c.url()}">${c.title()}</a></h2>@if(!c.excerpt().isEmpty())<p>${c.excerpt()}</p>@endif</div></div></td>
              <td class="m"><time datetime="${c.dateTime()}">${c.date()}</time></td>
              <td class="m">${c.reading()}</td>
            </tr>
          @endfor
        </tbody>
      </table>
      <div class="sbar"><span>${page.count()}</span><span>Page ${page.listing().page()} of ${page.listing().pages()}</span></div>
    </article>
    @template.pager(listing = page.listing(), olderLabel = "Older posts tagged " + page.name())
  `
)
