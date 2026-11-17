@import dev.danvega.beansprout.view.Site
@param Site site
<section class="box" aria-labelledby="follow-h">
  <h2 class="box-title" id="follow-h">Follow along</h2>
  <p>Every new post lands in the Atom feed. Older ones are in the archive.</p>
  <p class="actions"><a class="btn btn-ink" href="${site.feed()}">Atom feed</a><a class="btn" href="${site.archive()}">Archive</a></p>
</section>
