@import dev.danvega.beansprout.view.Card
@import gg.jte.support.ForSupport
@param Card card
@param int index
@param String place = ""
@param String status = ""
<article class="w${place.isEmpty() ? "" : " " + place} tint-${java.util.List.of("butter", "pink", "mint", "sky", "lilac").get(index % 5)}">
  <div class="tb"><span class="t">${card.url().substring(card.url().lastIndexOf('/') + 1)}.post</span><span class="x" aria-hidden="true"><i>–</i><i>×</i></span></div>
  <div class="wb${card.picture().isEmpty() ? "" : " has-pic"}">
    @if(!card.picture().isEmpty())<figure><img class="ph" src="${card.picture()}" alt="${card.pictureAlt()}" width="1200" height="600" loading="lazy"></figure>@endif
    <div class="wt">
      <h2><a href="${card.url()}">${card.title()}</a></h2>
      @if(!card.excerpt().isEmpty())<p class="excerpt">${card.excerpt()}</p>@endif
      <p class="meta"><time datetime="${card.dateTime()}">${card.date()}</time><span>${card.reading()}</span></p>
    </div>
  </div>
  @if(!card.tags().isEmpty() || !status.isEmpty())
    <div class="sbar">
      @if(!card.tags().isEmpty())<span class="sbar-tags"><span class="sr">Tags: </span>@for(var tg : ForSupport.of(card.tags()))<a href="${tg.get().url()}">${tg.get().name()}</a>@if(!tg.isLast()), @endif@endfor</span>@endif
      @if(!status.isEmpty())<span>${status}</span>@endif
    </div>
  @endif
</article>
