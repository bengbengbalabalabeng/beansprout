@import dev.danvega.beansprout.view.PostPage
@import java.util.Locale
@param PostPage page
!{var post = page.post();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = post.title() + " · " + page.site().name(),
  content = @`
    <article class="article">
      <header class="article-head">
        <p class="kicker"><a href="${post.crumb().url()}">${post.crumb().name()}</a></p>
        <h1 class="article-title">${post.title()}</h1>
        @if(!post.lede().isEmpty())<p class="stand">${post.lede()}</p>@endif
        <p class="meta">
          <span>Published <time datetime="${post.dateTime()}">${post.date()}</time></span>
          <span>${post.reading()}</span>
          @if(!post.updated().isEmpty())<span class="meta-updated">Updated <time datetime="${post.updatedDateTime()}">${post.updated()}</time></span>@endif
          @if(!post.tags().isEmpty())<span>Filed under @for(var t : gg.jte.support.ForSupport.of(post.tags()))${t.isFirst() ? "" : ", "}<a href="${t.get().url()}">${t.get().name()}</a>@endfor</span>@endif
        </p>
        @if(!post.author().isBlank())
          !{var names = post.author().strip().split("\\s+");}
          !{var initials = (new String(Character.toChars(names[0].codePointAt(0))) + (names.length > 1 ? new String(Character.toChars(names[names.length - 1].codePointAt(0))) : "")).toUpperCase(Locale.ROOT);}
          <ul class="authors" aria-label="Written by">
            <li class="author">
              @if(!page.site().photo().isEmpty())<img class="initials" src="${page.site().photo()}" alt="" width="72" height="72">@else<span class="initials" aria-hidden="true">${initials}</span>@endif
              <span class="author-text"><span class="author-name">${post.author()}</span></span>
            </li>
          </ul>
        @endif
      </header>
      @if(!post.picture().isEmpty())
        <figure class="hero">
          <img src="${post.picture()}" alt="${post.pictureAlt()}" width="${post.pictureWidth().isEmpty() ? "1600" : post.pictureWidth()}" height="${post.pictureHeight().isEmpty() ? "700" : post.pictureHeight()}">
          @if(!post.caption().isEmpty())<figcaption>${post.caption()}</figcaption>@endif
        </figure>
      @endif
      <div class="prose">$unsafe{post.body()}</div>
    </article>
    <nav class="article-nav" aria-label="Older and newer posts">
      @if(!post.older().url().isEmpty())<a class="article-older" href="${post.older().url()}"><span class="nav-k"><span aria-hidden="true">←</span> Older</span> <span>${post.older().title()}</span></a>@endif
      @if(!post.newer().url().isEmpty())<a class="article-newer" href="${post.newer().url()}"><span class="nav-k">Newer <span aria-hidden="true">→</span></span> <span>${post.newer().title()}</span></a>@else<p class="article-newest">This is the newest post. <a href="${page.site().feed()}">The Atom feed</a> gets the next one.</p>@endif
    </nav>
  `
)
