@import dev.danvega.beansprout.view.PostPage
@import gg.jte.support.ForSupport
@param PostPage page
!{var post = page.post();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  content = @`
    <article class="post">
      <header class="${post.picture().isEmpty() ? "p-head bare" : "p-head"}">
        <p class="kicker"><a href="${post.crumb().url()}">${post.crumb().name()}</a></p>
        <h1>${post.title()}</h1>
        @if(!post.lede().isEmpty())<p class="stand">${post.lede()}</p>@endif
        <div class="metabar">
          @if(!post.author().isEmpty())<span>By ${post.author()}</span>@endif
          <time datetime="${post.dateTime()}">${post.date()}</time>
          <span>${post.reading()}</span>
          @if(!post.updated().isEmpty())<span class="updated">Updated <time datetime="${post.updatedDateTime()}">${post.updated()}</time></span>@endif
          @if(!post.tags().isEmpty())<span class="tags">@for(var t : ForSupport.of(post.tags()))@if(!t.isFirst()), @endif<a href="${t.get().url()}">${t.get().name()}</a>@endfor</span>@endif
        </div>
      </header>
      @if(!post.picture().isEmpty())
        <figure class="hero">
          <img src="${post.picture()}" alt="${post.pictureAlt()}" width="1200" height="600">
          @if(!post.caption().isEmpty())<figcaption>${post.caption()}</figcaption>@endif
        </figure>
      @endif
      <div class="body">$unsafe{post.body()}</div>
      <nav class="p-foot" aria-label="More posts">
        @if(!post.older().url().isEmpty())<a class="btn alt" href="${post.older().url()}"><span aria-hidden="true">←</span> Older: ${post.older().title()}</a>@else<span class="last">This is the oldest post</span>@endif
        @if(!post.newer().url().isEmpty())<a class="btn alt" href="${post.newer().url()}">Newer: ${post.newer().title()} <span aria-hidden="true">→</span></a>@else<span class="last">This is the newest post</span>@endif
      </nav>
    </article>
  `
)
