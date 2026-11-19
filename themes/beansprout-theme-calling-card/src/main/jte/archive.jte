@import dev.danvega.beansprout.view.ArchivePage
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  top = @`@template.bar(site = page.site())`,
  content = @`
    <header class="card-head">
      <h1 class="card-page-title">Archive</h1>
      <p class="card-page-sub"><span class="card-archive-total">${archive.total()}</span>@if(!archive.since().isEmpty()) since <span class="card-archive-since">${archive.since()}</span>@endif.</p>
    </header>
    @if(archive.years().isEmpty())
      <p class="card-empty">Nothing is written here yet.</p>
    @endif
    @for(var y : archive.years())
      <section class="card-year" aria-labelledby="year-${y.year()}">
        <h2 class="card-year-num" id="year-${y.year()}">${y.year()}</h2>
        <div class="card-year-body">
          <p class="card-year-count">${y.count()}</p>
          <ul class="card-rows">
            @for(var r : y.posts())
              <li class="card-row"><a class="card-row-title" href="${r.url()}">${r.title()}</a> <time class="card-row-date" datetime="${r.dateTime()}">${r.date()}</time></li>
            @endfor
          </ul>
        </div>
      </section>
    @endfor
  `
)
