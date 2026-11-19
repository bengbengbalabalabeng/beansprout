@import dev.danvega.beansprout.view.ArchivePage
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  content = @`
    <div class="page-head">
      <h1 class="page-title">Archive</h1>
      <p class="page-note"><span class="archive-total">${archive.total()}</span>@if(!archive.since().isEmpty()) since <span class="archive-since">${archive.since()}</span>@endif.</p>
    </div>
    @if(archive.years().isEmpty())
      <p class="empty">Nothing here yet.</p>
    @endif
    @for(var y : archive.years())
      <section class="year" aria-labelledby="year-${y.year()}">
        <h2 class="year-title" id="year-${y.year()}">${y.year()} <span class="year-count">${y.count()}</span></h2>
        <ul class="rows">
          @for(var r : y.posts())
            <li class="archive-row"><time class="date" datetime="${r.dateTime()}">${r.date()}</time> <a href="${r.url()}">${r.title()}</a></li>
          @endfor
        </ul>
      </section>
    @endfor
  `
)
