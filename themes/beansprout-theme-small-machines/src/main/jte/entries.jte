@import dev.danvega.beansprout.view.Year
@param Year year
<ul class="entries">
  @for(var r : year.posts())
    <li class="entry"><a href="${r.url()}">${r.title()}</a><span><time datetime="${r.dateTime()}">${r.date()}</time> · ${r.reading()}</span></li>
  @endfor
</ul>
