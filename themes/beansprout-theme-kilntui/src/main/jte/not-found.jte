@import dev.danvega.beansprout.view.NotFoundPage
@param NotFoundPage page
@template.layout(
  site = page.site(),
  meta = page.meta(),
  title = "Page not found · " + page.site().name(),
  content = @`
    @if(!page.path().isEmpty())
      <div class="prompt-bar cli-box">
        <span class="prompt-cmd" aria-hidden="true">&gt; cat ${page.path()}<span class="cursor"></span></span>
      </div>
    @endif
    <section class="error-box cli-box" aria-labelledby="error-h">
      <div class="error-head">
        <p class="page-tag err">✗ [ERROR 404]</p>
        <h1 class="page-h1" id="error-h">no such file or directory</h1>
      </div>
      @if(!page.path().isEmpty())<p class="error-detail">The address <span class="inline-code">${page.path()}</span> does not exist here.</p>@endif
      <p>It may be mistyped, or the post was renamed or moved.</p>
      <div class="btns">
        <a class="cmd-btn" href="${page.site().home()}">[home]</a>
        <a class="cmd-btn" href="${page.site().archive()}">[archive]</a>
      </div>
      <p>The archive holds <span class="cnt">${page.postCount()}</span>.</p>
    </section>
    @if(!page.recent().isEmpty())
      <section class="rel cli-box" aria-labelledby="rel-h">
        <h2 class="rel-title" id="rel-h"><span class="box-tag">[recent]</span> Latest posts</h2>
        <ul class="minis">
          @for(var m : page.recent())
            <li>
              <a href="${m.url()}"><strong>${m.title()}</strong><span><time datetime="${m.dateTime()}">${m.date()}</time> · ${m.reading()}</span></a>
            </li>
          @endfor
        </ul>
      </section>
    @endif
  `
)
