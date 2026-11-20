@import dev.danvega.beansprout.view.Meta
@import dev.danvega.beansprout.view.Site
@import gg.jte.Content
@param Site site
@param Meta meta
@param String title
@param Content content
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <meta name="description" content="${meta.description()}">
    @if(!meta.canonical().isEmpty())<link rel="canonical" href="${meta.canonical()}">@endif
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
    <link rel="stylesheet" href="${site.root()}/beansprout/onionskin/onionskin.css">
    <link rel="alternate" type="application/atom+xml" title="${site.name()}" href="${site.feed()}">
  </head>
  <body>
    <a class="skip" href="#main">Skip to content</a>
    <div class="site">
      <header class="masthead">
        <p class="masthead-line"><a class="masthead-link" href="${site.home()}">${site.name()}</a>@if(!site.description().isEmpty()) <span class="masthead-tagline">/ ${site.description()}</span>@endif</p>
        <nav class="nav" aria-label="Main">
          <ul class="nav-list">
            <li><a class="nav-link" href="${site.home()}" aria-current="${site.section().equals("latest") ? "page" : null}">writing</a></li>
            <li><a class="nav-link" href="${site.archive()}" aria-current="${site.section().equals("archive") ? "page" : null}">archive</a></li>
            <li><a class="nav-link" href="${site.feed()}">feed</a></li>
            @for(var link : site.links())<li><a class="nav-link" href="${link.href()}">${link.label()}</a></li>@endfor
          </ul>
        </nav>
      </header>
      <main class="page" id="main">
        ${content}
      </main>
      <footer class="footer">
        <p>(c) <span class="footer-year">${site.year()}</span> <span class="footer-owner">${site.name()}</span>. Typed with Beansprout. <a href="${site.feed()}">Atom feed</a>.</p>
      </footer>
    </div>
  </body>
</html>
