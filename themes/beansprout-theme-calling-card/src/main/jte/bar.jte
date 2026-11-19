@import dev.danvega.beansprout.view.Site
@param Site site
<header class="card-bar">
  <div class="card-bar-inner">
    <p class="card-bar-name"><a class="card-bar-home" href="${site.home()}">${site.name()}</a></p>
    @template.nav(site = site)
  </div>
</header>
