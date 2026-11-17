@import dev.danvega.beansprout.view.IndexPage
@import gg.jte.support.ForSupport
@param IndexPage page
!{var listing = page.latest();}
!{var first = listing.page().equals("1");}
!{var cards = listing.cards();}
!{var looks = java.util.List.of("w2 ink tr", "w2", "w2 tl", "w4 red", "w2 tr", "w3", "w3 ochre tl");}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = first ? page.site().name() : "Page " + listing.page() + " · " + page.site().name(),
  content = @`
    <div class="intro">
      @if(!page.site().photo().isEmpty())<img class="intro-photo" src="${page.site().photo()}" alt="" width="240" height="240">@endif
      @if(first)
        <h1>Latest posts</h1>
        @if(!page.site().description().isEmpty())<p>${page.site().description()}</p>@endif
      @else
        <h1>Older posts</h1>
        <p>Page ${listing.page()} of ${listing.pages()}, newest first. Everything is in the <a href="${page.site().archive()}">archive</a> too.</p>
      @endif
    </div>
    @if(cards.isEmpty())
      <div class="empty">
        <h2>Nothing printed yet</h2>
        <p>There are no posts here yet. The first one will be pinned up on this page.</p>
      </div>
    @else
      @if(first)
        !{var lead = cards.getFirst();}
        <article class="${lead.picture().isEmpty() ? "lead bare" : "lead"}">
          @if(!lead.picture().isEmpty())<figure><img src="${lead.picture()}" alt="${lead.pictureAlt()}" width="1200" height="600"></figure>@endif
          <div class="txt">
            @if(!lead.tags().isEmpty())<p class="kicker">@for(var t : ForSupport.of(lead.tags()))@if(!t.isFirst()) · @endif<a href="${t.get().url()}">${t.get().name()}</a>@endfor</p>@endif
            <h2><a href="${lead.url()}">${lead.title()}</a></h2>
            @if(!lead.excerpt().isEmpty())<p class="excerpt">${lead.excerpt()}</p>@endif
            <p class="meta"><time datetime="${lead.dateTime()}">${lead.date()}</time><span>${lead.reading()}</span><span>Newest</span></p>
          </div>
          <p class="stamp" aria-hidden="true">${lead.reading()}</p>
        </article>
      @endif
      !{var rest = first ? cards.subList(1, cards.size()) : cards;}
      @if(!rest.isEmpty())
        <h2 class="sr">${first ? "More recent posts" : "Posts on this page"}</h2>
        <div class="collage">
          @for(var c : ForSupport.of(rest))
            @template.card(card = c.get(), look = looks.get(c.getIndex() % looks.size()))
          @endfor
        </div>
      @endif
    @endif
    @template.pager(listing = listing)
  `
)
