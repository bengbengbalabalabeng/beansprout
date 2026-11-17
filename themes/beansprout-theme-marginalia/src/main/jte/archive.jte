@import dev.danvega.beansprout.view.ArchivePage
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  content = @`
    <div class="page-head spread">
      <p class="spread-margin"><span class="margin-label">Archive</span></p>
      <div class="spread-text">
        <h1 class="page-title">Every entry, by year</h1>
        <p class="page-intro"><span class="archive-total">${archive.total()}</span>@if(!archive.since().isEmpty()) since <span class="archive-since">${archive.since()}</span>@endif.</p>
      </div>
    </div>
    @if(archive.years().isEmpty())
      <div class="spread">
        <div class="spread-margin"></div>
        <div class="spread-text">
          <div class="empty">
            <p class="empty-title">Nothing is in the notebook yet.</p>
          </div>
        </div>
      </div>
    @endif
    @for(var y : archive.years())
      <section class="year spread" aria-labelledby="year-${y.year()}">
        <p class="spread-margin"><span class="margin-label">${y.year()}</span><span class="margin-line year-count">${y.count()}</span></p>
        <div class="spread-text">
          <h2 class="visually-hidden" id="year-${y.year()}">${y.year()}</h2>
          <ul class="mini-list">
            @for(var r : y.posts())
              <li class="mini"><a class="mini-title" href="${r.url()}">${r.title()}</a> <span class="mini-meta"><time datetime="${r.dateTime()}">${r.date()}</time> · <span>${r.reading()}</span></span></li>
            @endfor
          </ul>
        </div>
      </section>
    @endfor
  `
)
