# Spec: threads

Beansprout under many requests at once, and with virtual threads. Dan asked on 16 November 2026.

## Why

Every call for posts waits for a lock, even after the posts were read. Nothing tests Beansprout
with virtual threads (`spring.threads.virtual.enabled`).

## Requirements

| ID | Requirement |
| --- | --- |
| THREADS-1 | When posts are not reloaded, the system shall read the posts files once, however many requests ask for posts at the same time. |
| THREADS-2 | When posts are not reloaded and have been read, the system shall answer a call for posts without waiting for another thread. |
| THREADS-3 | The sample app shall serve the blog with virtual threads turned on. |
| THREADS-4 | When posts are reloaded, the system shall read the posts files for one call at a time. |

Reloading on every call (`beansprout.posts.reload`) stays as POSTS-14 says, and its test still holds it.

## Out of scope

- Turning virtual threads on for an app. That is the app's own setting, and it changes more than
  the blog.
- Reading posts in parallel. Reading is quick and happens once.
