@import dev.danvega.beansprout.view.ArchivePage
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive / " + page.site().name(),
  content = @`
    <h1 class="page-title">Archive</h1>
    <p class="page-intro"><span class="archive-total">${archive.total()}</span>@if(!archive.since().isEmpty()) since <span class="archive-since">${archive.since()}</span>@endif.</p>
    @if(archive.years().isEmpty())
      <p class="empty">No memos yet.</p>
    @endif
    @for(var y : archive.years())
      <section class="year" aria-labelledby="year-${y.year()}">
        <h2 class="year-title" id="year-${y.year()}">${y.year()} <span class="year-count">(${y.count()})</span></h2>
        <ul class="listing">
          @for(var r : y.posts())
            <li class="listing-row"><time class="listing-date" datetime="${r.dateTime()}">${r.dateTime()}</time><span class="listing-reading">${r.reading()}</span><a class="listing-title" href="${r.url()}">${r.title()}</a></li>
          @endfor
        </ul>
      </section>
    @endfor
  `
)
