@import dev.danvega.beansprout.view.ArchivePage
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  content = @`
    <div class="arch">
      <h1>Archive</h1>
      <p><span class="arch-total">${archive.total()}</span>@if(!archive.since().isEmpty()) since <span class="arch-since">${archive.since()}</span>, newest first@endif. Also as an <a href="${page.site().feed()}">Atom feed</a>.</p>
      @for(var y : archive.years())
        <section class="yr" aria-labelledby="year-${y.year()}">
          <div><h2 id="year-${y.year()}">${y.year()}</h2><p class="cnt">${y.count()}</p></div>
          <ul class="entries">
            @for(var r : y.posts())
              <li class="entry"><time datetime="${r.dateTime()}">${r.date()}</time><a href="${r.url()}">${r.title()}</a><span class="rt">${r.reading()}</span></li>
            @endfor
          </ul>
        </section>
      @endfor
    </div>
  `
)
