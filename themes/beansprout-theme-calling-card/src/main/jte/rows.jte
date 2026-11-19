@import dev.danvega.beansprout.view.Listing
@param Listing listing
@param String empty = "Nothing is written here yet."
@if(listing.cards().isEmpty())
  <p class="card-empty">${empty}</p>
@else
  <ul class="card-rows">
    @for(var c : listing.cards())
      <li class="card-row"><a class="card-row-title" href="${c.url()}">${c.title()}</a> <time class="card-row-date" datetime="${c.dateTime()}">${c.date()}</time></li>
    @endfor
  </ul>
@endif
@if(!listing.pages().equals("1"))
  <nav class="card-pager" aria-label="More posts">
    <p class="card-pager-status">Page <span class="card-pager-page">${listing.page()}</span> of <span class="card-pager-pages">${listing.pages()}</span></p>
    <p class="card-pager-links">
      @if(!listing.newer().isEmpty())<a class="card-pager-link card-pager-newer" href="${listing.newer()}"><span aria-hidden="true">← </span>Newer</a>@endif
      @if(!listing.older().isEmpty())<a class="card-pager-link card-pager-older" href="${listing.older()}">Older<span aria-hidden="true"> →</span></a>@endif
    </p>
  </nav>
@endif
