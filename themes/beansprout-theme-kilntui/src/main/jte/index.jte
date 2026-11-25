@import dev.danvega.beansprout.view.IndexPage
@param IndexPage page
!{var listing = page.latest();}
!{var first = listing.page().equals("1");}
!{var cards = listing.cards();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = first ? page.site().name() : "Page " + listing.page() + " · " + page.site().name(),
  content = @`
    <section class="banner cli-box">
      <div class="banner-head">
        <span class="banner-icon" aria-hidden="true">✴</span>
        <div class="banner-txt">
          <h1>${page.site().name()}</h1>
          @if(!page.site().description().isEmpty())<p>${page.site().description()}</p>@endif
        </div>
      </div>
    </section>
    <div class="prompt-bar cli-box">
      <span class="prompt-cmd" aria-hidden="true">&gt; cli list-posts --status=published --sort=date@if(!first) --page=${listing.page()}@endif<span class="cursor"></span></span>
    </div>
    @template.chips(site = page.site(), allCurrent = first)
    @if(cards.isEmpty())
      <div class="empty cli-box">
        <p class="page-tag">[empty]</p>
        <h1 class="page-h1">Nothing published yet</h1>
        <p>The first post will be listed here as soon as it is saved in the posts folder.</p>
      </div>
    @else
      <h2 class="sr">${first ? "Latest posts" : "Posts on page " + listing.page()}</h2>
      <ol class="post-list">
        @for(var c : cards)
          @template.card(card = c)
        @endfor
      </ol>
    @endif
    @template.pager(listing = listing)
  `
)
