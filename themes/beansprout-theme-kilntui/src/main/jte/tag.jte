@import dev.danvega.beansprout.view.TagPage
@param TagPage page
!{var listing = page.listing();}
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = page.name() + " · " + page.site().name(),
  tagFeed = page.feed(),
  tagFeedTitle = page.site().name() + ": " + page.name(),
  content = @`
    <h1 class="sr">Posts tagged ${page.name()}</h1>
    <div class="prompt-bar cli-box">
      <span class="prompt-cmd" aria-hidden="true">&gt; cli list-posts --tag=${page.name()} --sort=date@if(!listing.page().equals("1")) --page=${listing.page()}@endif<span class="cursor"></span></span>
    </div>
    @template.chips(site = page.site())
    @if(listing.cards().isEmpty())
      <div class="empty cli-box">
        <p class="page-tag">[empty]</p>
        <h1 class="page-h1">No posts with this tag</h1>
        <p>Nothing has been published under # ${page.name()} yet.</p>
      </div>
    @else
      <h2 class="sr">Posts tagged ${page.name()}</h2>
      <ol class="post-list">
        @for(var c : listing.cards())
          @template.card(card = c)
        @endfor
      </ol>
    @endif
    @template.pager(listing = listing)
  `
)
