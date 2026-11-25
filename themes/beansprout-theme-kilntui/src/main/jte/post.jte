@import dev.danvega.beansprout.view.PostPage
@param PostPage page
!{var post = page.post();}
!{var slug = post.url().substring(post.url().lastIndexOf('/') + 1);}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  content = @`
    <nav class="breadcrumb-bar cli-box" aria-label="Breadcrumb">
      <div class="crumb-left">
        <a class="return" href="${post.crumb().url()}"><span aria-hidden="true">&lt;</span> Return to ${post.crumb().name()}</a>
        <span class="crumb-path">posts/${slug}.md</span>
      </div>
      <a class="crumb-home" href="${page.site().home()}">[Home]</a>
    </nav>
    <article class="article-shell cli-box">
      <header class="article-header">
        <p class="article-meta">
          <span class="article-badge">[ARTICLE]</span>
          <span>Published: <strong id="detail-date"><time datetime="${post.dateTime()}">${post.date()}</time></strong></span>
          <span class="divider" aria-hidden="true">•</span>
          @if(!post.author().isEmpty())
            <span>Author: <strong id="detail-author">${post.author()}</strong></span>
            <span class="divider" aria-hidden="true">•</span>
          @endif
          <span>Read Time: <strong id="detail-readtime">${post.reading()}</strong></span>
        </p>
        <h1 class="article-h1">${post.title()}</h1>
        @if(!post.tags().isEmpty())
          <p class="detail-tags">
            @for(var t : post.tags())<a class="tag" href="${t.url()}"># ${t.name()}</a>@endfor
          </p>
        @endif
      </header>
      @if(!post.lede().isEmpty())
        <div class="tldr-box">
          <p class="box-tag">[TL;DR Summary]</p>
          <p class="tldr-txt">${post.lede()}</p>
        </div>
      @endif
      @if(!post.picture().isEmpty())
        <figure class="hero">
          <img src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1200" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "600" : post.pictureHeight()}">
          @if(!post.caption().isEmpty())<figcaption>${post.caption()}</figcaption>@endif
        </figure>
      @endif
      @if(!post.headings().isEmpty())
        <nav class="toc" aria-label="On this page">
          <p class="box-tag cyan">[Contents]</p>
          <ol class="toc-list">
            @for(var h : post.headings())
              <li><a href="#${h.id()}">${h.text()}</a></li>
            @endfor
          </ol>
        </nav>
      @endif
      <div class="article-prose">$unsafe{post.body()}</div>
      $unsafe{page.site().slots().afterPost()}
      @if(!post.related().isEmpty())
        <section class="related" aria-labelledby="related-posts">
          <h2 class="related-title" id="related-posts"><span class="box-tag">[Related]</span> More like this</h2>
          <ul class="related-list">
            @for(var r : post.related())
              <li><a href="${r.url()}"><span class="related-arrow" aria-hidden="true">➔</span> ${r.title()}</a></li>
            @endfor
          </ul>
        </section>
      @endif
      <footer class="article-footer">
        <nav class="af-nav" aria-label="More posts">
          @if(!post.older().url().isEmpty())<a class="pager-btn" href="${post.older().url()}"><span aria-hidden="true">←</span> Older: ${post.older().title()}</a>
          @else<span class="pager-btn disabled" aria-disabled="true">This is the oldest post</span>@endif
          @if(!post.newer().url().isEmpty())<a class="pager-btn" href="${post.newer().url()}">Newer: ${post.newer().title()} <span aria-hidden="true">→</span></a>
          @else<span class="pager-btn disabled" aria-disabled="true">This is the newest post</span>@endif
        </nav>
        <a class="return-list" href="${page.site().home()}">Return to post list <span aria-hidden="true">➔</span></a>
      </footer>
    </article>
  `
)
