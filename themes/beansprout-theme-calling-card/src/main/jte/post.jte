@import dev.danvega.beansprout.view.PostPage
@param PostPage page
!{var post = page.post();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  top = @`@template.bar(site = page.site())`,
  content = @`
    <article class="card-post">
      <header class="card-post-head">
        <p class="card-crumb"><a class="card-crumb-link" href="${post.crumb().url()}">${post.crumb().name()}</a></p>
        <h1 class="card-title">${post.title()}</h1>
        @if(!post.lede().isEmpty())<p class="card-lede">${post.lede()}</p>@endif
        <p class="card-meta">
          @if(!post.author().isEmpty())<span class="card-meta-author">${post.author()}</span> · @endif
          <time class="card-meta-date" datetime="${post.dateTime()}">${post.date()}</time> · <span class="card-meta-reading">${post.reading()}</span>
          @if(!post.updated().isEmpty()) · <span class="card-meta-updated">Updated <time datetime="${post.updatedDateTime()}">${post.updated()}</time></span>@endif
        </p>
      </header>
      @if(!post.picture().isEmpty())
        <figure class="card-figure">
          <img class="card-picture" src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1200" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "600" : post.pictureHeight()}">
          @if(!post.caption().isEmpty())<figcaption class="card-caption">${post.caption()}</figcaption>@endif
        </figure>
      @endif
      <div class="card-prose">$unsafe{post.body()}</div>
      @if(!post.tags().isEmpty())
        <ul class="card-pills" aria-label="Tags">
          @for(var t : post.tags())
            <li><a class="card-pill" href="${t.url()}">${t.name()}</a></li>
          @endfor
        </ul>
      @endif
      @if(!post.newer().url().isEmpty() || !post.older().url().isEmpty())
        <nav class="card-near" aria-label="Newer and older">
          @if(!post.newer().url().isEmpty())<a class="card-near-link card-near-newer" href="${post.newer().url()}"><span class="card-near-label">Newer</span> <span class="card-near-title">${post.newer().title()}</span></a>@endif
          @if(!post.older().url().isEmpty())<a class="card-near-link card-near-older" href="${post.older().url()}"><span class="card-near-label">Older</span> <span class="card-near-title">${post.older().title()}</span></a>@endif
        </nav>
      @endif
    </article>
  `
)
