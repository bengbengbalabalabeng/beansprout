# Deploy your blog

A Beansprout blog is an ordinary Spring Boot app. It needs no database, so any host that runs a
Java app or a Docker image will do. This guide uses [Railway](https://railway.com).

## 1. Add a Dockerfile

Put this `Dockerfile` next to your `pom.xml`. It builds the jar with a full JDK, then runs it on a
smaller image with only the Java runtime.

```dockerfile
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw --batch-mode --quiet dependency:go-offline
COPY src src
RUN ./mvnw --batch-mode --quiet -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Your posts and pictures are inside the jar, so the image needs nothing else.

## 2. Listen on Railway's port

Railway tells your app which port to use in the `PORT` variable. If you followed
[Start a new blog](new-blog.md), your `application.yaml` already has this:

```yaml
server:
  port: ${PORT:8080}
```

## 3. Deploy

1. Push your app to GitHub.
2. In Railway, choose **New Project**, then **Deploy from GitHub repo**, and pick your repo.
   Railway finds the Dockerfile and builds it.
3. Open the service's **Settings**, then **Networking**, and choose **Generate Domain**.
4. Under **Variables**, add `BEANSPROUT_SITE_URL` with that address, such as
   `https://my-blog-production.up.railway.app`. The canonical links, link previews, feed and
   sitemap all use it. Without it they use the address each request came in on, which behind
   a proxy can be the wrong one.

Your blog is at that address plus `/blog`.

Every push to your main branch deploys again. A new post is a commit.

## Scheduled posts

A post dated in the future appears at that time, with no redeploy. The date is read in the
server's time zone, which is UTC on Railway. To use yours, set `BEANSPROUT_TIME_ZONE`, such as
`Europe/Amsterdam`.

## Other hosts

Anything that runs the Dockerfile works the same way. Without Docker, build the jar and run it:

```bash
./mvnw package -DskipTests
java -jar target/*.jar
```
