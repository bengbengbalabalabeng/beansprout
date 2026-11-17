@import dev.danvega.beansprout.view.Card
@param Card card
@param String kind = ""
<article class="story${kind.isEmpty() ? "" : " " + kind}">
  @if(!card.picture().isEmpty())
    <figure class="story-pic"><img src="${card.picture()}" alt="${card.pictureAlt()}" width="1600" height="900" loading="lazy"></figure>
  @endif
  @template.dateline(tags = card.tags(), date = card.date(), dateTime = card.dateTime(), reading = card.reading())
  <h2 class="story-title"><a href="${card.url()}">${card.title()}</a></h2>
  @if(!card.excerpt().isEmpty())<p class="story-excerpt">${card.excerpt()}</p>@endif
  @if(card.tags().size() > 1)@template.filed(tags = card.tags().subList(1, card.tags().size()), lead = "Also filed under")@endif
</article>
