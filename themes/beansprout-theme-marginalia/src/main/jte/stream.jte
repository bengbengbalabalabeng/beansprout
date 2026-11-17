@import dev.danvega.beansprout.view.Card
@import dev.danvega.beansprout.view.Listing
@import java.util.LinkedHashMap
@import java.util.stream.Collectors
@param Listing listing
@param String empty = "Nothing is in the notebook yet."
!{var days = listing.cards().stream().collect(Collectors.groupingBy(Card::dateTime, LinkedHashMap::new, Collectors.toList()));}
@if(days.isEmpty())
  <div class="spread">
    <div class="spread-margin"></div>
    <div class="spread-text">
      <div class="empty">
        <p class="empty-title">${empty}</p>
      </div>
    </div>
  </div>
@endif
@for(var day : days.values())
  <section class="day">
    <h2 class="day-title section-title"><time datetime="${day.getFirst().dateTime()}">${day.getFirst().date()}</time></h2>
    <ol class="day-list">
      @for(var c : day)
        <li>
          <article class="item spread item-post">
            <p class="item-meta spread-margin"><span class="margin-label item-kind">Post</span></p>
            <div class="item-body spread-text">
              <h3 class="item-title"><a class="item-title-link" href="${c.url()}">${c.title()}</a></h3>
              @if(!c.picture().isEmpty())<img class="item-picture" src="${c.picture()}" alt="${c.pictureAlt()}" width="1200" height="600" loading="lazy">@endif
              @if(!c.excerpt().isEmpty())<p class="item-comment">${c.excerpt()}</p>@endif
              <div class="item-foot">
                <span class="item-reading">${c.reading()}</span>
                @if(!c.tags().isEmpty())
                  <ul class="tag-list" aria-label="Tags">
                    @for(var t : c.tags())
                      <li><a class="tag-link" href="${t.url()}">#<span>${t.name()}</span></a></li>
                    @endfor
                  </ul>
                @endif
              </div>
            </div>
          </article>
        </li>
      @endfor
    </ol>
  </section>
@endfor
@if(!listing.pages().equals("1"))
  <nav class="pager" aria-label="More entries">
    <p class="pager-status">Page <span class="pager-page">${listing.page()}</span> of <span class="pager-pages">${listing.pages()}</span></p>
    <p class="pager-links">
      @if(!listing.newer().isEmpty())<a class="pager-newer" href="${listing.newer()}"><span aria-hidden="true">← </span>Newer entries</a>@endif
      @if(!listing.older().isEmpty())<a class="pager-older" href="${listing.older()}">Older entries<span aria-hidden="true"> →</span></a>@endif
    </p>
  </nav>
@endif
