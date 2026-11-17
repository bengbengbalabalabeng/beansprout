@import dev.danvega.beansprout.view.Listing
@param Listing listing
@param String olderLabel = "Older posts"
@if(!listing.pages().equals("1"))
  <nav class="pager" aria-label="Pages">
    @if(!listing.newer().isEmpty())<a class="btn" href="${listing.newer()}"><span aria-hidden="true">←</span> Newer posts</a>@endif
    <span class="pager-at">Page ${listing.page()} of ${listing.pages()}</span>
    @if(!listing.older().isEmpty())<a class="btn" href="${listing.older()}">${olderLabel} <span aria-hidden="true">→</span></a>@endif
  </nav>
@endif
