# AGENTS.md

AGENTS.md is the ask. The gates are the check. Each rule below says which one it is.

## The project

- Beansprout is an open source Spring Boot starter. It adds a Markdown blog to an app that already exists.
- `PRODUCT.md` holds the scope and the decisions. Read it before you start a feature.
- Spring Boot 4.1, Java 25, Maven. JTE is the default template engine. Thymeleaf and FreeMarker are supported.
- Always `./mvnw`, never `mvn`. The build needs JDK 25 or newer. If `./mvnw -v` shows an older JDK, stop and say so.
- Boot 4 names differ from Boot 3 (`spring-boot-starter-webmvc`, modular auto-configuration).
  The pom is the source of truth, not your memory.

## Modules

| Module | What lives there |
| --- | --- |
| `beansprout-core` | Posts, tags and the view contract. Plain Java. No web, no Spring Boot, no template engine. |
| `beansprout-spring-boot-autoconfigure` | Properties, controllers, engine adapters, auto-configuration. |
| `beansprout-spring-boot-starter` | No code. Only the dependencies an app needs. |
| `beansprout-sample` | An ordinary Boot app that adds the starter. End-to-end tests go here. |
| `verification` | Repo-wide gates. Not yours to edit. |

## Packages

- `dev.danvega.beansprout.content`: reading Markdown files into posts.
- `dev.danvega.beansprout.view`: the view contract. Every value a theme receives, as records.
- `dev.danvega.beansprout.web`: controllers.
- `dev.danvega.beansprout.engine.jte`, `.thymeleaf`, `.freemarker`: the only places each engine's classes may appear.
- `dev.danvega.beansprout.autoconfigure`: `*AutoConfiguration` and `*Properties` classes.

## How we work

- Specs first. A feature starts as `specs/<feature>.md` with requirement IDs. `specs/README.md` shows the format.
  Dan approves the spec before any code.
- A spec and its implementation go in separate commits.
- Tests first. Write a failing test for each requirement, with the ID in its `@DisplayName`. Show the red, then implement.
- Existing test files are read-only. New behavior gets a new test file. If an old test fails, stop and say why.
- If a change touches more than one module, show a short plan and wait.
- One feature per change. No cleanup nobody asked for.
- Ask before adding a dependency. Template engines and MCP go in as `optional`, so an app only gets what it chose.
  No Lombok.
- If a gate blocks you and you think the gate is wrong, stop and say why. Do not work around it.
- Commits have one author. Never add a `Co-Authored-By` line or any other attribution.

## Enforced: the build or a hook fails

- **Nulls.** NullAway in JSpecify mode. Everything is non-null unless it is marked `@Nullable`.
- **Warnings.** Error Prone and `-Xlint:all -Werror`. A compiler warning fails the build.
- **Shape.** `ArchitectureTests.java`: core stays plain, each engine stays in its package, no package cycles,
  Boot naming for auto-configurations and properties, controllers in `web`, no `System.out`, no field injection,
  no generic exceptions. Read it before you add a class.
- **Registration.** `AutoConfigurationImportsTests`: every `@AutoConfiguration` is in the imports file, and nothing else is.
- **Dependencies.** Maven Enforcer: JDK 25+, Maven 3.9+, no Lombok, JUnit 4 or log4j 1.
  `beansprout-core` cannot depend on Spring Boot, Spring Web, servlets or any template engine.
- **Done.** `DefinitionOfDoneTests`: every spec requirement has a test that names its ID, every ID a test names
  exists in a spec, and no `TODO`, `FIXME` or `XXX` in any `src/main`.
- **Hooks.** You cannot stop while `./mvnw -q verify` is red (three tries, then report `Done: no`).
  You cannot stop or commit with a changed existing test. A commit runs `verify` first.
  A commit with both `specs/` and `src/main` changes is refused.
- **Protected files.** You cannot edit `AGENTS.md`, `.claude/`, `.mvn/`, `verification/` or `ArchitectureTests.java`.
  `pom.xml` edits ask first.

## Not enforced yet: follow these, a reviewer checks

Each of these should become a test when the feature it guards lands.

- An app without a given engine on the classpath still starts.
- Every engine renders the same pages from the same data.
- The starter never pulls in an engine or MCP on its own.
- Themes receive only `view` types, never `content` internals.
- The MCP server never publishes. It only creates files with `draft: true`, and only in a folder on disk.
- A post dated in the future stays hidden until that date, with no restart.

## Judgment: Dan reads these

- The view contract. Is it right for theme authors? Names follow Longhand's model where they fit.
- Public names: `beansprout.*` properties, routes, template names. Renaming one later breaks every user.
- The first-run experience: one dependency, a folder of Markdown, nothing else to configure.

## Done means

- `./mvnw -q verify` passes.
  One test class: `./mvnw -q -pl <module> -am test -Dtest=<Class> -Dsurefire.failIfNoSpecifiedTests=false`.
- `/attack` returns PASS, or each VALID finding is fixed or answered with evidence.
- For anything a reader sees: you ran the sample and hit the page with curl.
  `./mvnw -q install -DskipTests && ./mvnw -pl beansprout-sample spring-boot:run`

## Report

End every task with this block. A command without its output is not a result.

    Done: yes | no
    Ran:
      ./mvnw -q verify            -> exit 0 (or the failing lines)
      curl -s -i localhost:8080/blog  -> 200, ...
    Spec coverage: each requirement ID -> its test -> pass | fail
    Not checked: ...
    Open questions: ...
