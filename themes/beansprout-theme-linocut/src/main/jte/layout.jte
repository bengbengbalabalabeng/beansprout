@import dev.danvega.beansprout.view.Meta
@import dev.danvega.beansprout.view.Site
@import gg.jte.Content
@param Site site
@param Meta meta
@param String title
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
    <link rel="stylesheet" href="${site.root()}/beansprout/linocut/linocut.css">
    <link rel="alternate" type="application/atom+xml" title="${site.name()}" href="${site.feed()}">
    @if(!tagFeed.isEmpty())<link rel="alternate" type="application/atom+xml" title="${tagFeedTitle}" href="${tagFeed}">@endif
    $unsafe{site.slots().head()}
  </head>
  <body>
    <a class="skip" href="#main">Skip to content</a>
    <header class="top">
      <div class="wrap">
        <a class="brand" href="${site.home()}">${site.name()}</a>
        <nav class="nav" aria-label="Main">
          <a href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">Posts</a>
          <a href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">Archive</a>
          @for(var link : site.links())<a href="${link.href()}">${link.label()}</a>@endfor
        </nav>
        <a class="btn feed" href="${site.feed()}">Atom feed</a>
      </div>
    </header>
    <main class="wrap" id="main">
      ${content}
    </main>
    <footer class="foot">
      <div class="wrap">
        <span>© <span class="foot-year">${site.year()}</span> <span class="foot-owner">${site.name()}</span> · printed without JavaScript</span>
        <nav aria-label="Elsewhere">
          <a href="${site.archive()}">Archive</a>
          <a href="${site.feed()}">Atom feed</a>
        </nav>
      </div>
      $unsafe{site.slots().footer()}
    </footer>
  </body>
</html>
