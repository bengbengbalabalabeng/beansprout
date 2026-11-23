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
    <link rel="stylesheet" href="${site.root()}/beansprout/small-machines/small-machines.css">
    <link rel="alternate" type="application/atom+xml" title="${site.name()}" href="${site.feed()}">
    @if(!tagFeed.isEmpty())<link rel="alternate" type="application/atom+xml" title="${tagFeedTitle}" href="${tagFeed}">@endif
    $unsafe{site.slots().head()}
  </head>
  <body>
    <a class="skip" href="#main">Skip to content</a>
    <header class="menu">
      <a class="brand" href="${site.home()}">@if(!site.photo().isEmpty())<img class="brand-photo" src="${site.photo()}" alt="" width="20" height="20">@else<i></i>@endif<span>${site.name()}</span></a>
      <nav aria-label="Main">
        <a href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">Latest</a>
        <a href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">Archive</a>
        @for(var link : site.links())<a href="${link.href()}">${link.label()}</a>@endfor
      </nav>
      <a class="sub" href="${site.feed()}">Atom feed</a>
    </header>
    <div class="desk">
      <nav class="icons" aria-label="Shortcuts">
        <a class="ic" href="${site.home()}"><i class="fold"></i><span>Latest</span></a>
        <a class="ic" href="${site.archive()}"><i class="fold p"></i><span>Archive</span></a>
        <a class="ic" href="${site.feed()}"><i class="docic"></i><span>feed.xml</span></a>
      </nav>
      <main class="stack" id="main">
        ${content}
      </main>
      <footer class="foot">
        <div class="foot-text">
          <p>© <span class="foot-year">${site.year()}</span> <span class="foot-owner">${site.name()}</span> · ${site.name()} OS · no JavaScript was harmed</p>
          @if(!site.description().isEmpty())<p class="foot-about">${site.description()}</p>@endif
        </div>
        <nav aria-label="Elsewhere">
          <a href="${site.archive()}">Archive</a>
          <a href="${site.feed()}">Atom feed</a>
        </nav>
        $unsafe{site.slots().footer()}
      </footer>
    </div>
  </body>
</html>
