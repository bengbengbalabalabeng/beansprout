@import dev.danvega.beansprout.view.Listing
@param Listing listing
@param String olderLabel = "Older posts"
@if(!listing.pages().equals("1"))
  <nav class="pgr" aria-label="More posts">
    @if(!listing.newer().isEmpty())<a class="b pgr-newer" href="${listing.newer()}"><span aria-hidden="true">←</span> Newer posts</a>@else<span class="pgr-gap"></span>@endif
    <span class="pgr-status">Page ${listing.page()} of ${listing.pages()}</span>
    @if(!listing.older().isEmpty())<a class="b pri pgr-older" href="${listing.older()}">${olderLabel} <span aria-hidden="true">→</span></a>@else<span class="pgr-gap"></span>@endif
  </nav>
@endif
