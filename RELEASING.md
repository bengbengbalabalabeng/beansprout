# Releasing Beansprout

A release goes to Maven Central when a version tag such as `v0.1.0` is pushed.
`.github/workflows/release.yml` builds, signs and uploads it. The upload then waits at
central.sonatype.com until you press **Publish**, so nothing goes live by accident.

These artifacts are published: `beansprout-parent`, `beansprout-bom`, `beansprout-core`,
`beansprout-spring-boot-autoconfigure`, `beansprout-spring-boot-starter`,
`beansprout-mcp-spring-boot-starter`, and the themes
`beansprout-theme-longhand`, `-dateline`, `-linocut`, `-marginalia` and `-small-machines`. The
sample and the verification module are not.

## One-time setup

### 1. A Maven Central account and the `dev.danvega` namespace

1. Sign in at [central.sonatype.com](https://central.sonatype.com) with GitHub.
2. Under **Namespaces**, add `dev.danvega`. Central shows a verification key.
3. Add a DNS TXT record on `danvega.dev` with that key as its value, then press **Verify** in
   Central. It can take a few minutes for DNS to update. You can delete the record afterwards.

### 2. A token for uploads

In Central, open **View Account** and choose **Generate User Token**. Save both parts as GitHub
secrets:

```bash
gh secret set CENTRAL_USERNAME --repo danvega/beansprout
```

```bash
gh secret set CENTRAL_PASSWORD --repo danvega/beansprout
```

### 3. A signing key

Maven Central needs every file signed. Create a key just for releases:

```bash
gpg --quick-generate-key "Beansprout releases" ed25519 sign 3y
```

Publish the public half, so Central can check the signatures:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

Save the private half and its passphrase as GitHub secrets:

```bash
gpg --armor --export-secret-keys <KEY_ID> | gh secret set MAVEN_GPG_KEY --repo danvega/beansprout
```

```bash
gh secret set MAVEN_GPG_PASSPHRASE --repo danvega/beansprout
```

### 4. Make the repository public

Maven Central lists the source repository on every artifact, so it should be public before the
first release.

```bash
gh repo edit danvega/beansprout --visibility public --accept-visibility-change-consequences
```

## Before the first release

Settled for 0.1.0 (see "Decisions" in PRODUCT.md). Before 1.0 they can still change:

- The group ID `dev.danvega` and the artifact names.
- The routes, the `beansprout.*` properties and the theme's template names.
- Whether an app needs one dependency or two (the starter and the theme).

## Each release

1. Set the version, commit and tag:

   ```bash
   ./mvnw versions:set -DnewVersion=0.1.0 -DgenerateBackupPoms=false
   ```

   ```bash
   git commit -am "Release 0.1.0" && git tag v0.1.0
   ```

2. Push the commit and the tag. The tag starts the release workflow:

   ```bash
   git push origin main v0.1.0
   ```

3. When the workflow is green, open **Deployments** at central.sonatype.com, check the files,
   and press **Publish**. They reach Maven Central within about half an hour.

4. Move `main` on to the next version:

   ```bash
   ./mvnw versions:set -DnewVersion=0.2.0-SNAPSHOT -DgenerateBackupPoms=false
   ```

   ```bash
   git commit -am "Start 0.2.0" && git push origin main
   ```

5. Update the README and `docs/` so they no longer say to install Beansprout locally.

## Try a release build without publishing

```bash
./mvnw -P release -Dgpg.skip=true -DskipPublishing=true deploy
```

This builds every jar, sources jar and Javadoc jar, and skips signing and the upload.
