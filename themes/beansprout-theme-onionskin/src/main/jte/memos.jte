@import dev.danvega.beansprout.view.Listing
@param Listing listing
@param String empty = "No memos yet."
@if(listing.cards().isEmpty())
  <p class="empty">${empty}</p>
@else
  <ol class="memo-list">
    @for(var c : listing.cards())
      <li>
        <article class="memo">
          <dl class="memo-head">
            <dt>Date:</dt>
            <dd><time datetime="${c.dateTime()}">${c.dateTime()}</time></dd>
            <dt>Subject:</dt>
            <dd><h2 class="memo-subject"><a href="${c.url()}">${c.title()}</a></h2></dd>
            <dt>Filed:</dt>
            <dd>
              @if(c.tags().isEmpty())
                <span class="memo-none">none</span>
              @else
                <ul class="inline-list" aria-label="Tags">
                  @for(var t : c.tags())
                    <li><a href="${t.url()}">${t.name()}</a></li>
                  @endfor
                </ul>
              @endif
            </dd>
            <dt>Length:</dt>
            <dd>${c.reading()}</dd>
          </dl>
          @if(!c.excerpt().isEmpty())<p class="memo-excerpt">${c.excerpt()}</p>@endif
        </article>
      </li>
    @endfor
  </ol>
@endif
@if(!listing.pages().equals("1"))
  <nav class="pager" aria-label="More memos">
    <p class="pager-status">Page <span class="pager-page">${listing.page()}</span> of <span class="pager-pages">${listing.pages()}</span></p>
    <p class="pager-links">
      @if(!listing.newer().isEmpty())<a class="pager-newer" href="${listing.newer()}"><span aria-hidden="true">&lt; </span>newer memos</a>@endif
      @if(!listing.older().isEmpty())<a class="pager-older" href="${listing.older()}">older memos<span aria-hidden="true"> &gt;</span></a>@endif
    </p>
  </nav>
@endif
