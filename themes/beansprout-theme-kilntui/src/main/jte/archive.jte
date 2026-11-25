@import dev.danvega.beansprout.view.ArchivePage
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  content = @`
    <div class="prompt-bar cli-box">
      <span class="prompt-cmd" aria-hidden="true">&gt; cli list-posts --all --sort=date<span class="cursor"></span></span>
    </div>
    <header class="t-head">
      <p class="page-tag">[ARCHIVE]</p>
      <h1 class="page-h1">Archive</h1>
      <p class="tally"><span class="cnt">${archive.total()}</span>@if(!archive.since().isEmpty()) since ${archive.since()}@endif, newest first. Also as an <a href="${page.site().feed()}">Atom feed</a>.</p>
    </header>
    @for(var y : archive.years())
      <section class="yr cli-box" aria-labelledby="year-${y.year()}">
        <div class="yr-head">
          <h2 class="yr-title" id="year-${y.year()}">posts/${y.year()}/</h2>
          <p class="yr-cnt">${y.count()}</p>
        </div>
        <ul class="entries">
          @for(var r : y.posts())
            <li class="entry">
              <time datetime="${r.dateTime()}">${r.date()}</time>
              <a href="${r.url()}">${r.title()}</a>
              <span class="rt">${r.reading()}</span>
            </li>
          @endfor
        </ul>
      </section>
    @endfor
  `
)
