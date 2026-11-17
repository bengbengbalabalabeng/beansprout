@import dev.danvega.beansprout.view.Tag
@import java.util.List
@param List<Tag> tags
@param String date
@param String dateTime
@param String reading
<p class="dateline">@if(!tags.isEmpty())<a href="${tags.getFirst().url()}">${tags.getFirst().name()}</a><span class="sep" aria-hidden="true">·</span>@endif<time datetime="${dateTime}">${date}</time><span class="sep" aria-hidden="true">·</span><span>${reading}</span></p>
