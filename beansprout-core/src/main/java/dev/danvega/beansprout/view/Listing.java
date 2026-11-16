package dev.danvega.beansprout.view;

import java.util.List;

/**
 * A page of posts as cards, and the links to the pages either side of it.
 *
 * @param page this page's number, as text
 * @param pages how many pages there are, as text
 * @param newer the newer page's address, or empty on the first page
 * @param older the older page's address, or empty on the last
 */
public record Listing(List<Card> cards, String page, String pages, String newer, String older) {
}
