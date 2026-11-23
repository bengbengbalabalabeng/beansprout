@import dev.danvega.beansprout.view.PostPage
@param PostPage page
!{var post = page.post();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  content = @`
    <article class="entry spread">
      <div class="spread-margin entry-meta">
        <span class="margin-label entry-kind">Post</span>
        <span class="margin-line"><time class="entry-date" datetime="${post.dateTime()}">${post.date()}</time></span>
        <span class="margin-line entry-reading">${post.reading()}</span>
        @if(!post.updated().isEmpty())<span class="margin-line entry-updated">Updated <time datetime="${post.updatedDateTime()}">${post.updated()}</time></span>@endif
        @if(!post.author().isEmpty())<span class="margin-line entry-author">${post.author()}</span>@endif
        @if(!post.tags().isEmpty())
          <ul class="margin-tags" aria-label="Tags">
            @for(var t : post.tags())
              <li><a class="tag-link" href="${t.url()}">#<span>${t.name()}</span></a></li>
            @endfor
          </ul>
        @endif
        @if(!post.headings().isEmpty())
          <nav class="toc" aria-label="On this page">
            <p class="toc-title">On this page</p>
            <ol class="toc-list">
              @for(var h : post.headings())
                <li><a href="#${h.id()}">${h.text()}</a></li>
              @endfor
            </ol>
          </nav>
        @endif
      </div>
      <div class="spread-text">
        <h1 class="entry-title">${post.title()}</h1>
        @if(!post.lede().isEmpty())<p class="entry-lede">${post.lede()}</p>@endif
        @if(!post.picture().isEmpty())
          <figure class="entry-figure">
            <img class="entry-picture" src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1200" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "600" : post.pictureHeight()}">
            @if(!post.caption().isEmpty())<figcaption class="entry-caption">${post.caption()}</figcaption>@endif
          </figure>
        @endif
        <div class="prose">$unsafe{post.body()}</div>
        $unsafe{page.site().slots().afterPost()}
        @if(!post.related().isEmpty())
          <section class="related" aria-labelledby="related-posts">
            <h2 class="related-title" id="related-posts">See also</h2>
            <ul class="related-list">
              @for(var r : post.related())
                <li><a href="${r.url()}">${r.title()}</a></li>
              @endfor
            </ul>
          </section>
        @endif
        <nav class="entry-foot" aria-label="Newer and older">
          @if(!post.newer().url().isEmpty())<a class="entry-foot-link entry-foot-newer" href="${post.newer().url()}"><span class="entry-foot-label"><span aria-hidden="true">← </span>Newer</span> <span class="entry-foot-title">${post.newer().title()}</span></a>@else<p class="entry-foot-end entry-foot-newer">This is the newest entry.</p>@endif
          @if(!post.older().url().isEmpty())<a class="entry-foot-link entry-foot-older" href="${post.older().url()}"><span class="entry-foot-label">Older<span aria-hidden="true"> →</span></span> <span class="entry-foot-title">${post.older().title()}</span></a>@else<p class="entry-foot-end entry-foot-older">This is the oldest entry.</p>@endif
        </nav>
      </div>
    </article>
  `
)
