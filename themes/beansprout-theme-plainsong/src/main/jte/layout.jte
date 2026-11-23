@import dev.danvega.beansprout.view.Meta
@import dev.danvega.beansprout.view.Site
@import gg.jte.Content
@param Site site
@param Meta meta
@param String title
@param Content content
@param boolean compact = false
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
    <link rel="stylesheet" href="${site.root()}/beansprout/plainsong/plainsong.css">
    <link rel="alternate" type="application/atom+xml" title="${site.name()}" href="${site.feed()}">
    @if(!tagFeed.isEmpty())<link rel="alternate" type="application/atom+xml" title="${tagFeedTitle}" href="${tagFeed}">@endif
  </head>
  <body>
    <a class="skip" href="#main">Skip to content</a>
    <div class="wrap">
      <header class="masthead${compact ? " masthead-compact" : ""}">
        <p class="name"><a class="name-link" href="${site.home()}">${site.name()}</a></p>
        @if(!compact)
          @if(!site.description().isEmpty())<p class="desc">${site.description()}</p>@endif
          <nav class="nav" aria-label="Main">
            <ul class="nav-list">
              <li><a href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">Writing</a></li>
              <li><a href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">Archive</a></li>
              <li><a href="${site.feed()}">Feed</a></li>
              @for(var link : site.links())<li><a href="${link.href()}">${link.label()}</a></li>@endfor
            </ul>
          </nav>
        @endif
      </header>
      <main id="main">
        ${content}
      </main>
      <footer class="footer">
        <p>© <span class="footer-year">${site.year()}</span> <span class="footer-owner">${site.name()}</span> · <a href="${site.archive()}">Archive</a> · <a href="${site.feed()}">Atom feed</a> · Built with Beansprout</p>
      </footer>
    </div>
  </body>
</html>
