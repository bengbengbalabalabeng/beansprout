@import dev.danvega.beansprout.view.PostPage
@param PostPage page
!{var post = page.post();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " / " + page.site().name(),
  content = @`
    <article class="entry">
      <dl class="memo-head entry-head">
        <dt>Date:</dt>
        <dd><time class="entry-date" datetime="${post.dateTime()}">${post.dateTime()}</time> <span class="memo-also">(${post.date()})</span></dd>
        @if(!post.updated().isEmpty())
          <dt>Revised:</dt>
          <dd><time class="entry-updated" datetime="${post.updatedDateTime()}">${post.updatedDateTime()}</time> <span class="memo-also">(${post.updated()})</span></dd>
        @endif
        @if(!post.author().isEmpty())
          <dt>From:</dt>
          <dd class="entry-author">${post.author()}</dd>
        @endif
        <dt>Filed:</dt>
        <dd>
          @if(post.tags().isEmpty())
            <span class="memo-none">none</span>
          @else
            <ul class="inline-list" aria-label="Tags">
              @for(var t : post.tags())
                <li><a href="${t.url()}">${t.name()}</a></li>
              @endfor
            </ul>
          @endif
        </dd>
        <dt>Length:</dt>
        <dd class="entry-reading">${post.reading()}</dd>
      </dl>
      <h1 class="entry-title">${post.title()}</h1>
      @if(!post.lede().isEmpty())<p class="entry-lede">${post.lede()}</p>@endif
      @if(!post.picture().isEmpty())
        <figure class="entry-figure">
          <img class="entry-picture" src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1200" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "600" : post.pictureHeight()}">
          @if(!post.caption().isEmpty())<figcaption class="entry-caption">Fig. 1. ${post.caption()}</figcaption>@endif
        </figure>
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
      <div class="prose">$unsafe{post.body()}</div>
      $unsafe{page.site().slots().afterPost()}
      @if(!post.related().isEmpty())
        <section class="related" aria-labelledby="related-posts">
          <h2 class="related-title" id="related-posts">related memos</h2>
          <ul class="related-list">
            @for(var r : post.related())
              <li><a href="${r.url()}">${r.title()}</a></li>
            @endfor
          </ul>
        </section>
      @endif
      <nav class="entry-foot" aria-label="Previous and next memo">
        @if(!post.older().url().isEmpty())<p><a class="entry-foot-older" href="${post.older().url()}"><span aria-hidden="true">&lt; </span>previous memo: ${post.older().title()}</a></p>@else<p class="entry-foot-end">This is the first memo.</p>@endif
        @if(!post.newer().url().isEmpty())<p><a class="entry-foot-newer" href="${post.newer().url()}"><span aria-hidden="true">&gt; </span>next memo: ${post.newer().title()}</a></p>@else<p class="entry-foot-end">This is the latest memo.</p>@endif
      </nav>
    </article>
  `
)
