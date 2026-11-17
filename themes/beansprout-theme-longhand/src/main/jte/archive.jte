@import dev.danvega.beansprout.view.ArchivePage
@param ArchivePage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Archive · " + page.site().name(),
  content = @`
    <div class="archive">
      <header class="page-head">
        <h1 class="page-title">Archive</h1>
        <p class="page-lede page-lede-small"><span class="archive-total">${page.archive().total()}</span>@if(!page.archive().since().isEmpty()) since <span class="archive-since">${page.archive().since()}</span>@endif.</p>
      </header>
      @for(var y : page.archive().years())
        <section class="archive-year" aria-labelledby="year-${y.year()}">
          <h2 class="archive-year-title" id="year-${y.year()}"><span class="archive-year-number">${y.year()}</span> <span class="archive-year-count">${y.count()}</span></h2>
          <ul class="archive-list">
            @for(var r : y.posts())
              <li class="archive-item">
                <a class="archive-link" href="${r.url()}">${r.title()}</a>
                <time class="archive-date" datetime="${r.dateTime()}">${r.date()}</time>
                <span class="archive-reading">${r.reading()}</span>
              </li>
            @endfor
          </ul>
        </section>
      @endfor
    </div>
  `,
  sidebar = @`@template.tags(site = page.site())`
)
