@import dev.danvega.beansprout.view.Listing
@param Listing listing
@if(!listing.pages().equals("1"))
  <nav class="list-bottom-bar cli-box" aria-label="Pages">
    <span class="showing">Page ${listing.page()} of ${listing.pages()}</span>
    <p class="pager-btns">
      @if(!listing.newer().isEmpty())<a class="pager-btn" href="${listing.newer()}"><span aria-hidden="true">←</span> Prev</a>
      @else<span class="pager-btn disabled" aria-disabled="true"><span aria-hidden="true">←</span> Prev</span>@endif
      @if(!listing.older().isEmpty())<a class="pager-btn" href="${listing.older()}">Next <span aria-hidden="true">→</span></a>
      @else<span class="pager-btn disabled" aria-disabled="true">Next <span aria-hidden="true">→</span></span>@endif
    </p>
  </nav>
@endif
