@import dev.danvega.beansprout.view.Meta
@import dev.danvega.beansprout.view.Site
@import gg.jte.Content
@param Site site
@param Meta meta
@param String title
@param Content content
@param Content sidebar
@param String tagFeed = ""
@param String tagFeedTitle = ""
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <meta name="description" content="${meta.description()}">
    @if(!meta.canonical().isEmpty())<link rel="canonical" href="${meta.canonical()}">@endif
    @if(!meta.robots().isEmpty())<meta name="robots" content="${meta.robots()}">@endif
    <meta property="og:type" content="${meta.type()}">
    <meta property="og:title" content="${meta.title()}">
    <meta property="og:description" content="${meta.description()}">
    @if(!meta.canonical().isEmpty())<meta property="og:url" content="${meta.canonical()}">@endif
    <meta property="og:site_name" content="${meta.siteName()}">
    @if(!meta.image().isEmpty())
      <meta property="og:image" content="${meta.image()}">
      @if(!meta.imageAlt().isEmpty())<meta property="og:image:alt" content="${meta.imageAlt()}">@endif
    @endif
    <meta name="twitter:card" content="${meta.twitterCard()}">
    @if(!meta.publishedTime().isEmpty())
      <meta property="article:published_time" content="${meta.publishedTime()}">
      <meta property="article:modified_time" content="${meta.modifiedTime()}">
      @for(var tag : meta.tags())
        <meta property="article:tag" content="${tag}">
      @endfor
    @endif
    @if(!meta.jsonLd().isEmpty())<script type="application/ld+json">$unsafe{meta.jsonLd()}</script>@endif
    <link rel="stylesheet" href="${site.root()}/beansprout/code.css">
    <link rel="stylesheet" href="${site.root()}/beansprout/longhand/longhand.css">
    <link rel="alternate" type="application/atom+xml" title="${site.name()}" href="${site.feed()}">
    @if(!tagFeed.isEmpty())<link rel="alternate" type="application/atom+xml" title="${tagFeedTitle}" href="${tagFeed}">@endif
    $unsafe{site.slots().head()}
  </head>
  <body>
    <a class="skip" href="#main">Skip to content</a>
    <div class="site">
      <header class="site-header">
        @if(!site.photo().isEmpty())<img class="site-avatar" src="${site.photo()}" alt="" width="240" height="240">@endif
        <p class="site-name"><a class="site-name-link" href="${site.home()}">${site.name()}</a></p>
        @if(!site.description().isEmpty())<p class="site-bio">${site.description()}</p>@endif
        <nav class="site-nav" aria-label="Main">
          <ul class="nav-list">
            <li><a class="nav-link" href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">Latest</a></li>
            <li><a class="nav-link" href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">Archive</a></li>
            @for(var link : site.links())<li><a class="nav-link" href="${link.href()}">${link.label()}</a></li>@endfor
          </ul>
        </nav>
        <button class="button menu-button" type="button" popovertarget="menu">Menu</button>
        <nav class="menu" id="menu" popover aria-label="Menu">
          <div class="menu-head">
            <p class="menu-name">${site.name()}</p>
            <button class="button menu-close" type="button" popovertarget="menu" popovertargetaction="hide">Close</button>
          </div>
          <ul class="menu-list">
            <li><a class="menu-link" href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">Latest</a></li>
            <li><a class="menu-link" href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">Archive</a></li>
            @for(var link : site.links())<li><a class="menu-link" href="${link.href()}">${link.label()}</a></li>@endfor
          </ul>
          @if(!site.tags().isEmpty())
            <div class="menu-tags">
              <p class="label" id="menu-tags-label">Tags</p>
              <ul class="chips" aria-labelledby="menu-tags-label">
                @for(var t : site.tags())
                  <li><a class="chip" href="${t.url()}" aria-current="${t.current() ? "page" : null}"><span class="chip-name">${t.name()}</span> · <span class="chip-count">${t.count()}</span></a></li>
                @endfor
              </ul>
            </div>
          @endif
          <div class="menu-subscribe">
            <p class="menu-rss"><a class="text-link" href="${site.feed()}">Atom feed</a></p>
          </div>
        </nav>
      </header>

      <main class="main" id="main">
        ${content}
      </main>

      <aside class="sidebar">
        ${sidebar}
        <div class="sidebar-subscribe">
          <p class="sidebar-rss"><a class="text-link" href="${site.feed()}">Atom feed</a></p>
        </div>
      </aside>

      <footer class="site-footer">
        <ul class="footer-links">
          <li><a class="footer-link" href="${site.feed()}">Atom feed</a></li>
        </ul>
        <p class="footer-copyright">© <span class="footer-year">${site.year()}</span> <span class="footer-owner">${site.name()}</span></p>
        $unsafe{site.slots().footer()}
      </footer>
    </div>
  </body>
</html>
