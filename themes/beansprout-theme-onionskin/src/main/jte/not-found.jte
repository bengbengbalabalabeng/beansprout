@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Memo not found / " + page.site().name(),
  content = @`
    <p class="page-status">Status: 404, not found</p>
    <h1 class="page-title">Memo not found</h1>
    @if(!page.path().isEmpty())
      <p>Nothing is filed at <span class="onionskin-path">${page.path()}</span>.</p>
    @endif
    <p>The <a href="${page.site().archive()}">archive</a> holds <span class="not-found-total">${page.postCount()}</span>. Or start from the <a href="${page.site().home()}">latest memos</a>.</p>
    @if(!page.recent().isEmpty())
      <section class="recent" aria-labelledby="recent-title">
        <h2 class="recent-title" id="recent-title">Recent memos</h2>
        <ul class="listing">
          @for(var m : page.recent())
            <li class="listing-row"><time class="listing-date" datetime="${m.dateTime()}">${m.dateTime()}</time><a class="listing-title" href="${m.url()}">${m.title()}</a><span class="listing-reading">${m.reading()}</span></li>
          @endfor
        </ul>
      </section>
    @endif
  `
)
