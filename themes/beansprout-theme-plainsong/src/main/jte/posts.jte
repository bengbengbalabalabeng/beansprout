@import dev.danvega.beansprout.view.Listing
@param Listing listing
@param String year
@param String empty = "Nothing here yet."
@if(listing.cards().isEmpty())
  <p class="empty">${empty}</p>
@else
  <ol class="posts">
    @for(var c : listing.cards())
      !{var date = c.date().endsWith(" " + year) ? c.date().substring(0, c.date().length() - year.length() - 1) : c.date();}
      <li class="post-item">
        <div class="row"><a class="title" href="${c.url()}">${c.title()}</a> <time class="date" datetime="${c.dateTime()}">${date}</time></div>
        @if(!c.excerpt().isEmpty())<p class="sub">${c.excerpt()}</p>@endif
      </li>
    @endfor
  </ol>
@endif
@if(!listing.pages().equals("1"))
  <nav class="pager" aria-label="More posts">
    @if(!listing.newer().isEmpty())<a class="pager-newer" href="${listing.newer()}">Newer posts</a>@else<span></span>@endif
    @if(!listing.older().isEmpty())<a class="pager-older" href="${listing.older()}">Older posts</a>@endif
  </nav>
@endif
