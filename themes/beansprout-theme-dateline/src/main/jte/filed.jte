@import dev.danvega.beansprout.view.Tag
@import gg.jte.support.ForSupport
@import java.util.List
@param List<Tag> tags
@param String lead = "Filed under"
@if(!tags.isEmpty())
  <p class="byline">${lead} @for(var t : ForSupport.of(tags))${t.isFirst() ? "" : t.isLast() ? " and " : ", "}<a href="${t.get().url()}">${t.get().name()}</a>@endfor</p>
@endif
