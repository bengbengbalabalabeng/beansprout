@import dev.danvega.beansprout.view.IndexPage
@import gg.jte.support.ForSupport
@param IndexPage page
!{var cards = page.latest().cards();}
!{var first = page.latest().page().equals("1");}
!{var lead = Math.min(3, cards.size());}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = first ? page.site().name() : "Page " + page.latest().page() + " · " + page.site().name(),
  content = @`
    <h1 class="sr">${first ? "Latest posts" : "Latest posts, page " + page.latest().page()}</h1>
    @if(cards.isEmpty())
      <div class="win-grid">
        <section class="w dialog" aria-labelledby="empty-title">
          <div class="tb"><span class="t">Desktop · no posts yet</span><span class="x" aria-hidden="true"><i>–</i><i>×</i></span></div>
          <div class="wb">
            <div class="warn" aria-hidden="true">!</div>
            <h2 id="empty-title">No posts yet</h2>
            <div>
              <p>This desk is clear. When the first post is published, it opens here as a window.</p>
              <div class="btns">
                <a class="b pri" href="${page.site().feed()}">Follow the feed</a>
              </div>
            </div>
          </div>
        </section>
      </div>
    @elseif(first)
      <div class="win-grid">
        @for(var e : ForSupport.of(cards.subList(0, lead)))
          @template.card(card = e.get(), index = e.getIndex(), place = e.getIndex() == 0 ? "f-lead" : e.getIndex() == 1 ? "f-two" : "f-three", status = e.getIndex() == 0 ? "newest" : "")
        @endfor
        @if(cards.size() > lead)
          <div class="win-grid collage">
            @for(var e : ForSupport.of(cards.subList(lead, cards.size())))
              @template.card(card = e.get(), index = e.getIndex() + lead)
            @endfor
          </div>
        @endif
      </div>
    @else
      <div class="win-grid pile">
        @for(var e : ForSupport.of(cards))
          @template.card(card = e.get(), index = e.getIndex())
        @endfor
      </div>
    @endif
    @template.pager(listing = page.latest())
  `
)
