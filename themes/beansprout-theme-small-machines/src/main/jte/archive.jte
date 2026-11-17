@import dev.danvega.beansprout.view.ArchivePage
@import gg.jte.support.ForSupport
@param ArchivePage page
!{var archive = page.archive();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  content = @`
    <article class="w folder tint-pink">
      <div class="tb"><span class="t">Archive · ${archive.total()}</span><span class="x" aria-hidden="true"><i>–</i><i>×</i></span></div>
      <nav class="path" aria-label="Breadcrumb">
        <a href="${page.site().home()}">${page.site().name()}</a>
        <span aria-hidden="true">›</span>
        <b aria-current="page">Archive</b>
      </nav>
      <h1 class="sr">Archive</h1>
      @if(archive.years().isEmpty())
        <div class="empty">
          <i class="fold" aria-hidden="true"></i>
          <h2>Nothing in the archive yet</h2>
          <p>When the first post is published, it is filed here under its year.</p>
          <div class="btns"><a class="b" href="${page.site().home()}">Latest posts</a></div>
        </div>
      @else
        <p class="sum"><span class="archive-total">${archive.total()}</span>@if(!archive.since().isEmpty()) since <span class="archive-since">${archive.since()}</span>@endif.@if(archive.years().size() > 1) The newest year is open. Earlier years open with a click.@endif</p>
        @for(var y : ForSupport.of(archive.years()))
          @if(y.isFirst())
            <section class="yr" aria-labelledby="year-${y.get().year()}">
              <h2 id="year-${y.get().year()}">${y.get().year()}</h2>
              <p class="cnt">${y.get().count()}</p>
              @template.entries(year = y.get())
            </section>
          @else
            <details class="yr">
              <summary><span><span class="yr-year" role="heading" aria-level="2">${y.get().year()}</span> <span class="cnt">${y.get().count()}</span></span></summary>
              @template.entries(year = y.get())
            </details>
          @endif
        @endfor
      @endif
    </article>
  `
)
