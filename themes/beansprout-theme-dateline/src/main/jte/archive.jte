@import dev.danvega.beansprout.view.ArchivePage
@import gg.jte.support.ForSupport
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  width = "mid",
  content = @`
    <header class="page-head">
      <p class="kicker">Archive</p>
      <h1 class="page-title">Every post, by year</h1>
      <p class="page-sub">${archive.total()}@if(!archive.since().isEmpty()) since ${archive.since()}@endif.</p>
    </header>
    @if(archive.years().isEmpty())
      @template.empty(site = page.site())
    @endif
    @for(var y : ForSupport.of(archive.years()))
      <details class="year" open="${y.isFirst()}">
        <summary class="year-head"><span class="year-name">${y.get().year()}</span> <span class="year-count">${y.get().count()}</span></summary>
        <ol class="year-list">
          @for(var r : y.get().posts())
            <li class="arow">
              <time class="arow-day" datetime="${r.dateTime()}">${r.date()}</time>
              <a class="arow-title" href="${r.url()}">${r.title()}</a>
              <span class="arow-reading">${r.reading()}</span>
            </li>
          @endfor
        </ol>
      </details>
    @endfor
  `
)
