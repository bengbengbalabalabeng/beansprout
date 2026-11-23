@import dev.danvega.beansprout.view.Meta
@import dev.danvega.beansprout.view.Site
@import gg.jte.Content
@param Site site
@param Meta meta
@param String title
@param String width = ""
@param Content content
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
    <link rel="stylesheet" href="${site.root()}/beansprout/dateline/dateline.css">
    <link rel="alternate" type="application/atom+xml" title="${site.name()}" href="${site.feed()}">
    @if(!tagFeed.isEmpty())<link rel="alternate" type="application/atom+xml" title="${tagFeedTitle}" href="${tagFeed}">@endif
    $unsafe{site.slots().head()}
  </head>
  <body>
    <a class="skip" href="#main">Skip to the page</a>
    <header class="mast">
      <div class="mast-bar">
        <a class="mast-name" href="${site.home()}"><span class="mast-dot" aria-hidden="true"></span><span>${site.name()}</span></a>
        <nav class="mast-nav" aria-label="Main">
          <a href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">Latest</a>
          <a href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">Archive</a>
          @for(var link : site.links())<a href="${link.href()}">${link.label()}</a>@endfor
        </nav>
      </div>
      <div class="mast-strip">
        <p class="mast-tag">${site.description()}</p>
        <p class="mast-links"><a href="${site.feed()}">Atom feed</a></p>
      </div>
    </header>
    <main class="page${width.isEmpty() ? "" : " page-" + width}" id="main">
      ${content}
    </main>
    <footer class="foot">
      <div class="foot-in">
        <p class="foot-name">${site.name()}</p>
        <p class="foot-note">© ${site.year()} ${site.name()}</p>
        <nav class="foot-nav" aria-label="Elsewhere">
          <a href="${site.feed()}">Atom feed</a>
          <a href="${site.archive()}">Archive</a>
        </nav>
      </div>
      $unsafe{site.slots().footer()}
    </footer>
  </body>
</html>
