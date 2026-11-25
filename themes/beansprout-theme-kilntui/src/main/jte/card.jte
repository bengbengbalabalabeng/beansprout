@import dev.danvega.beansprout.view.Card
@param Card card
!{var slug = card.url().substring(card.url().lastIndexOf('/') + 1);}
<li class="post-card cli-box">
  <div class="card-meta">
    <p class="meta-line">
      <time datetime="${card.dateTime()}">${card.date()}</time>
      <span>${card.reading()}</span>
    </p>
    @if(!card.tags().isEmpty())
      <p class="card-tags">
        @for(var t : card.tags())<a class="tag" href="${t.url()}"># ${t.name()}</a>@endfor
      </p>
    @endif
  </div>
  <h2 class="card-title"><a href="${card.url()}">${card.title()}<span class="go" aria-hidden="true">Read Post ➔</span></a></h2>
  @if(!card.excerpt().isEmpty())<p class="card-excerpt">${card.excerpt()}</p>@endif
  <p class="card-foot"><span class="card-path">posts/${slug}.md</span></p>
</li>
