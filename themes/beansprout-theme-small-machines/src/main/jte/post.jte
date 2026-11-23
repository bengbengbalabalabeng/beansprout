@import dev.danvega.beansprout.view.PostPage
@import gg.jte.support.ForSupport
@param PostPage page
!{var post = page.post();}
!{var address = post.url();}
!{var file = address.isEmpty() ? "post" : address.substring(address.lastIndexOf('/') + 1) + ".post";}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  content = @`
    <article class="w doc tint-sky">
      <div class="tb"><span class="t">${file} · ${page.site().name()}</span><span class="x" aria-hidden="true"><i>–</i><i>×</i></span></div>
      <div class="tools">
        @if(!post.author().isEmpty())<span class="tools-author">By ${post.author()}</span>@endif
        <time class="tools-date" datetime="${post.dateTime()}">${post.date()}</time>
        <span class="tools-reading">${post.reading()}</span>
        @if(!post.updated().isEmpty())<span class="tools-updated">Updated <time datetime="${post.updatedDateTime()}">${post.updated()}</time></span>@endif
        @if(!post.tags().isEmpty())<span class="tools-tags"><span class="sr">Tags: </span>@for(var tg : ForSupport.of(post.tags()))<a href="${tg.get().url()}">${tg.get().name()}</a>@if(!tg.isLast()), @endif@endfor</span>@endif
      </div>
      <div class="page">
        <header class="p-head">
          <p class="kind">@if(post.crumb().url().equals(page.site().home()))Post@else<a href="${post.crumb().url()}">Post · ${post.crumb().name()}</a>@endif</p>
          <h1>${post.title()}</h1>
          @if(!post.lede().isEmpty())<p class="stand">${post.lede()}</p>@endif
        </header>
        @if(!post.headings().isEmpty())
          <nav class="snav" aria-labelledby="contents-title">
            <div class="h"><span id="contents-title">contents.txt</span><span>${post.headings().size() == 1 ? "1 section" : post.headings().size() + " sections"}</span></div>
            <ol>
              @for(var h : ForSupport.of(post.headings()))
                <li><a href="#${h.get().id()}"><b>${h.getIndex() + 1}</b><span>${h.get().text()}</span></a></li>
              @endfor
            </ol>
          </nav>
        @endif
        @if(!post.picture().isEmpty())
          <figure class="hero">
            <img class="ph" src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1200" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "600" : post.pictureHeight()}">
            @if(!post.caption().isEmpty())<figcaption>${post.caption()}</figcaption>@endif
          </figure>
        @endif
        <div class="body">$unsafe{post.body()}</div>
        $unsafe{page.site().slots().afterPost()}
        @if(!post.related().isEmpty())
          <section class="related" aria-labelledby="related-posts">
            <h2 class="related-title" id="related-posts">Related</h2>
            <ul class="related-list">
              @for(var r : post.related())
                <li><a href="${r.url()}">${r.title()}</a></li>
              @endfor
            </ul>
          </section>
        @endif
        <nav class="p-foot" aria-label="Older and newer posts">
          @if(!post.older().url().isEmpty())<a class="b p-older" href="${post.older().url()}"><span aria-hidden="true">←</span> Older: ${post.older().title()}</a>@else<span class="p-end">This is the oldest post</span>@endif
          @if(!post.newer().url().isEmpty())<a class="b p-newer" href="${post.newer().url()}">Newer: ${post.newer().title()} <span aria-hidden="true">→</span></a>@else<span class="p-end">This is the newest post</span>@endif
        </nav>
      </div>
      <div class="sbar"><span>${post.reading()}</span><time datetime="${post.dateTime()}">${post.date()}</time></div>
    </article>
  `
)
