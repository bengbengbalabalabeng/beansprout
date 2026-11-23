@import dev.danvega.beansprout.view.PostPage
@import gg.jte.support.ForSupport
@param PostPage page
!{var post = page.post();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  compact = true,
  content = @`
    <article class="entry">
      <h1 class="entry-title">${post.title()}</h1>
      @if(!post.lede().isEmpty())<p class="lede">${post.lede()}</p>@endif
      <p class="meta">@if(!post.author().isEmpty())<span class="entry-author">${post.author()}</span> · @endif<time class="entry-date" datetime="${post.dateTime()}">${post.date()}</time> · <span class="entry-reading">${post.reading()}</span>@if(!post.updated().isEmpty()) · <span class="entry-updated">Updated <time datetime="${post.updatedDateTime()}">${post.updated()}</time></span>@endif</p>
      @if(post.headings().size() >= 2)
        <details class="contents" open>
          <summary>Contents</summary>
          <ul>
            @for(var h : post.headings())
              <li><a href="#${h.id()}">${h.text()}</a></li>
            @endfor
          </ul>
        </details>
      @endif
      @if(!post.picture().isEmpty())
        <figure class="figure">
          <img class="picture" src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1200" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "600" : post.pictureHeight()}">
          @if(!post.caption().isEmpty())<figcaption>${post.caption()}</figcaption>@endif
        </figure>
      @endif
      <div class="prose">$unsafe{post.body()}</div>
      @if(!post.tags().isEmpty())
        <p class="tags">Tagged @for(var t : ForSupport.of(post.tags()))<a href="${t.get().url()}">${t.get().name()}</a>@if(!t.isLast()), @endif@endfor</p>
      @endif
      @if(!post.related().isEmpty())
        <section class="related" aria-labelledby="related-posts">
          <h2 class="related-title" id="related-posts">Read next</h2>
          <ul class="related-list">
            @for(var r : post.related())
              <li><a href="${r.url()}">${r.title()}</a></li>
            @endfor
          </ul>
        </section>
      @endif
      @if(!post.newer().url().isEmpty() || !post.older().url().isEmpty())
        <nav class="near" aria-label="Newer and older posts">
          @if(!post.newer().url().isEmpty())<a class="near-newer" href="${post.newer().url()}">Newer: ${post.newer().title()}</a>@endif
          @if(!post.older().url().isEmpty())<a class="near-older" href="${post.older().url()}">Older: ${post.older().title()}</a>@endif
        </nav>
      @endif
    </article>
  `
)
