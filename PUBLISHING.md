# Publishing to Maven Central

This project publishes to **Maven Central** through the
[Sonatype Central Portal](https://central.sonatype.com/). Continuous integration
(`.github/workflows/ci.yml`) builds and tests every push and PR; releasing
(`.github/workflows/release.yml`) is triggered by pushing a version tag.

The coordinates are:

```
groupId:    io.github.ankit--sethi
artifactId: PartiQLBuilder
```

## One-time setup

You only do steps 1–4 once.

### 1. Register the namespace

1. Sign in to <https://central.sonatype.com/> with your GitHub account.
2. The portal auto-creates the verified namespace **`io.github.ankit--sethi`**
   for your GitHub account. Confirm it shows as *verified* under
   **Namespaces**. (If it is not auto-verified, follow the portal's
   "Verify Namespace" flow, which asks you to create a temporary public repo
   whose name is the verification code it gives you.)

### 2. Generate a Central Portal user token

1. In the portal, go to your account → **Generate User Token**.
2. It gives you a token *username* and *password* (not your login password).
   These become the `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` secrets.

### 3. Create a GPG signing key

Central requires every artifact to be GPG-signed.

```bash
# Generate a key (choose RSA 4096, set a passphrase you will remember)
gpg --full-generate-key

# Find the key id
gpg --list-secret-keys --keyid-format=long

# Publish the PUBLIC key so Central can verify signatures
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>

# Export the PRIVATE key (armored) for the GitHub secret
gpg --armor --export-secret-keys <YOUR_KEY_ID>
```

The armored private key block (including the
`-----BEGIN/END PGP PRIVATE KEY BLOCK-----` lines) is the `GPG_PRIVATE_KEY`
secret; its passphrase is `MAVEN_GPG_PASSPHRASE`.

### 4. Add the GitHub repository secrets

In the repo: **Settings → Secrets and variables → Actions → New repository secret**.
Add all four:

| Secret                   | Value                                             |
|--------------------------|---------------------------------------------------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal token *username* (step 2)          |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal token *password* (step 2)          |
| `GPG_PRIVATE_KEY`        | Armored private key block (step 3)                |
| `MAVEN_GPG_PASSPHRASE`   | Passphrase for that key (step 3)                  |

## Cutting a release

Maven Central does **not** accept `-SNAPSHOT` versions. The release workflow
sets the project version for you from the tag, so you don't edit `pom.xml`:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The `Release` workflow then:

1. builds and runs the full test suite,
2. generates the `-sources.jar` and `-javadoc.jar`,
3. GPG-signs every artifact,
4. uploads the bundle to the Central Portal and (because `autoPublish=true`)
   publishes it once validation passes.

You can also run it manually from the **Actions** tab (**Release → Run
workflow**) and type the version.

After the pom in the working tree is still `1.0-SNAPSHOT` (the version bump
happens only inside the CI job), so there is nothing to commit back.

### Publishing manually from your machine

If you ever need to publish without CI, set the same four values in your
`~/.m2/settings.xml` (a `<server>` with id `central`, plus the GPG passphrase),
then:

```bash
mvn -Prelease -DnewVersion=1.0.0 versions:set -DgenerateBackupPoms=false
mvn -Prelease deploy
```

## Notes

- The `release` profile in `pom.xml` holds the signing and publishing plugins,
  so an ordinary `mvn verify` needs no key or credentials.
- `autoPublish` is `true` in `pom.xml`. Set it to `false` if you'd rather review
  each deployment in the portal and click **Publish** yourself.
- Builds target Java 21; the workflows use Temurin 21.
