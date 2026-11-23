@import dev.danvega.beansprout.view.Meta
@import dev.danvega.beansprout.view.Site
@import gg.jte.Content
@param Site site
@param Meta meta
@param String title
@param Content top
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
    <link rel="stylesheet" href="${site.root()}/beansprout/calling-card/calling-card.css">
    <link rel="alternate" type="application/atom+xml" title="${site.name()}" href="${site.feed()}">
    @if(!tagFeed.isEmpty())<link rel="alternate" type="application/atom+xml" title="${tagFeedTitle}" href="${tagFeed}">@endif
  </head>
  <body>
    <a class="card-skip" href="#main">Skip to content</a>
    ${top}
    <main class="card-main" id="main">
      <div class="card-inner">
        ${content}
      </div>
    </main>
    <footer class="card-footer">
      <div class="card-inner">
        <p>© <span class="card-footer-year">${site.year()}</span> <span class="card-footer-owner">${site.name()}</span> · <a href="${site.feed()}">Atom feed</a> · Built with Beansprout</p>
      </div>
    </footer>
  </body>
</html>
