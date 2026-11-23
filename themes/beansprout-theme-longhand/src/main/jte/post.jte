@import dev.danvega.beansprout.view.PostPage
@param PostPage page
!{var post = page.post();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  content = @`
    <article class="post">
      <header class="post-header">
        <nav class="breadcrumb" aria-label="Breadcrumb">
          <ol class="breadcrumb-list">
            <li><a class="breadcrumb-link" href="${page.site().home()}">Latest</a></li>
            @if(!post.crumb().url().equals(page.site().home()))<li><a class="breadcrumb-link" href="${post.crumb().url()}">${post.crumb().name()}</a></li>@endif
          </ol>
        </nav>
        <h1 class="post-title">${post.title()}</h1>
        @if(!post.lede().isEmpty())<p class="post-lede">${post.lede()}</p>@endif
        <div class="post-info">
          @if(!post.author().isEmpty())<p class="post-author">${post.author()}</p>@endif
          <p class="post-meta"><time class="post-date" datetime="${post.dateTime()}">${post.date()}</time> · <span class="post-reading">${post.reading()}</span>@if(!post.updated().isEmpty()) · Updated <time class="post-updated" datetime="${post.updatedDateTime()}">${post.updated()}</time>@endif</p>
          @if(!post.tags().isEmpty())
            <ul class="post-tags" aria-label="Tags">
              @for(var tg : post.tags())
                <li><a class="tag" href="${tg.url()}">${tg.name()}</a></li>
              @endfor
            </ul>
          @endif
        </div>
      </header>
      @if(!post.picture().isEmpty())
        <figure class="post-figure">
          <img class="post-figure-img" src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1200" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "600" : post.pictureHeight()}">
          @if(!post.caption().isEmpty())<figcaption class="post-figure-caption">${post.caption()}</figcaption>@endif
        </figure>
      @endif
      <div class="post-body">$unsafe{post.body()}</div>
      $unsafe{page.site().slots().afterPost()}
    </article>
    @if(!post.related().isEmpty())
      <section class="related" aria-labelledby="related-posts">
        <h2 class="related-title" id="related-posts">Keep reading</h2>
        <ul class="related-list">
          @for(var r : post.related())
            <li><a href="${r.url()}">${r.title()}</a></li>
          @endfor
        </ul>
      </section>
    @endif
    <nav class="post-nav" aria-label="Newer and older posts">
      @if(!post.newer().url().isEmpty())<a class="post-nav-item post-nav-newer" href="${post.newer().url()}"><span class="post-nav-label"><span aria-hidden="true">←</span> Newer</span> <span class="post-nav-title">${post.newer().title()}</span></a>@else<p class="post-nav-item post-nav-newer post-nav-empty">This is the newest post.</p>@endif
      @if(!post.older().url().isEmpty())<a class="post-nav-item post-nav-older" href="${post.older().url()}"><span class="post-nav-label">Older <span aria-hidden="true">→</span></span> <span class="post-nav-title">${post.older().title()}</span></a>@else<p class="post-nav-item post-nav-older post-nav-empty">This is the oldest post.</p>@endif
    </nav>
  `,
  sidebar = @`
    @if(!post.headings().isEmpty())
      <h2 class="label sidebar-label">On this page</h2>
      <ul class="toc-list">
        @for(var h : post.headings())
          <li><a class="toc-link" href="#${h.id()}">${h.text()}</a></li>
        @endfor
      </ul>
    @else
      @template.tags(site = page.site())
    @endif
  `
)
