@import dev.danvega.beansprout.view.Listing
@param Listing listing
@param String olderLabel = "Older posts"
@if(listing.cards().isEmpty())
  <p class="page-lede">No posts yet.</p>
@else
  <ul class="cards">
    @for(var c : listing.cards())
      <li class="card">
        <div class="card-body">
          <h2 class="card-title"><a class="card-link" href="${c.url()}">${c.title()}</a></h2>
          <p class="card-meta"><time class="card-date" datetime="${c.dateTime()}">${c.date()}</time> · <span class="card-reading">${c.reading()}</span></p>
          @if(!c.excerpt().isEmpty())<p class="card-excerpt">${c.excerpt()}</p>@endif
          @if(!c.tags().isEmpty())
            <ul class="card-tags" aria-label="Tags">
              @for(var tg : c.tags())
                <li><a class="tag" href="${tg.url()}">${tg.name()}</a></li>
              @endfor
            </ul>
          @endif
        </div>
      </li>
    @endfor
  </ul>
@endif
@if(!listing.pages().equals("1"))
  <nav class="pager" aria-label="More posts">
    @if(!listing.newer().isEmpty())<a class="button pager-link pager-newer" href="${listing.newer()}"><span aria-hidden="true">←</span> Newer<span class="pager-more"> posts</span></a>@endif
    <p class="pager-status">Page <span class="pager-page">${listing.page()}</span> of <span class="pager-pages">${listing.pages()}</span></p>
    @if(!listing.older().isEmpty())<a class="button pager-link pager-older" href="${listing.older()}">${olderLabel} <span aria-hidden="true">→</span></a>@endif
  </nav>
@endif
