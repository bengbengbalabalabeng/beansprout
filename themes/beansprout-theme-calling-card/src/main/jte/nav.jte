@import dev.danvega.beansprout.view.Site
@param Site site
<nav class="card-nav" aria-label="Main">
  <ul class="card-nav-list">
    <li><a class="card-nav-link" href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">Writing</a></li>
    <li><a class="card-nav-link" href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">Archive</a></li>
    <li><a class="card-nav-link" href="${site.feed()}">Feed</a></li>
  </ul>
</nav>
