@import dev.danvega.beansprout.view.Card
@import gg.jte.support.ForSupport
@param Card card
@param String look
<article class="card ${look}">
  @if(!card.picture().isEmpty())<figure><img src="${card.picture()}" alt="${card.pictureAlt()}" width="1200" height="600" loading="lazy"></figure>@endif
  @if(!card.tags().isEmpty())<p class="kicker">@for(var t : ForSupport.of(card.tags()))@if(!t.isFirst()) · @endif<a href="${t.get().url()}">${t.get().name()}</a>@endfor</p>@endif
  <h2><a href="${card.url()}">${card.title()}</a></h2>
  @if(!card.excerpt().isEmpty())<p class="excerpt">${card.excerpt()}</p>@endif
  <p class="meta"><time datetime="${card.dateTime()}">${card.date()}</time><span>${card.reading()}</span></p>
</article>
