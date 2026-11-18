@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Page not found · " + page.site().name(),
  content = @`
    <div class="page-head spread">
      <p class="spread-margin"><span class="margin-label">Not found</span><span class="margin-line">Error 404</span></p>
      <div class="spread-text">
        <h1 class="page-title">Nothing lives at this address</h1>
        @if(!page.path().isEmpty())
          <p class="page-intro"><span class="address">${page.path()}</span></p>
        @endif
        <div class="empty">
          <h2 class="empty-title">What this means</h2>
          <p>There is no entry at this address. It is most likely a typo in a link, an address cut short in an email, or an entry that was renamed.</p>
          <ul class="steps">
            <li>Look by year in the <a href="${page.site().archive()}">archive</a>, all <span class="not-found-total">${page.postCount()}</span> of it.</li>
            <li>Or start from the <a href="${page.site().home()}">newest entries</a>.</li>
          </ul>
        </div>
        @if(!page.recent().isEmpty())
          <section class="block" aria-labelledby="recent-title">
            <h2 class="section-title" id="recent-title">The newest entries</h2>
            <ul class="mini-list">
              @for(var m : page.recent())
                <li class="mini"><a class="mini-title" href="${m.url()}">${m.title()}</a> <span class="mini-meta"><time datetime="${m.dateTime()}">${m.date()}</time> · <span>${m.reading()}</span></span></li>
              @endfor
            </ul>
          </section>
        @endif
      </div>
    </div>
  `
)
